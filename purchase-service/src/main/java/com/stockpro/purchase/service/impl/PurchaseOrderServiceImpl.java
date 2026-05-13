package com.stockpro.purchase.service.impl;

import com.stockpro.purchase.client.ProductClient;
import com.stockpro.purchase.client.SupplierClient;
import com.stockpro.purchase.client.WarehouseClient;
import com.stockpro.purchase.config.RabbitMQConfig;
import com.stockpro.purchase.entity.*;
import com.stockpro.purchase.publisher.PurchaseOrderEventPublisher;
import com.stockpro.purchase.repository.PurchaseOrderRepository;
import com.stockpro.purchase.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final SupplierClient supplierClient;
    private final ProductClient productClient;
    private final WarehouseClient warehouseClient;
    private final PurchaseOrderEventPublisher eventPublisher; // RabbitMQ Publisher (non-critical)

    //  CREATE PO
    @Override
    @CacheEvict(value = "purchaseOrders", allEntries = true)
    public PurchaseOrder createPO(PurchaseOrder po) {

        //  Supplier validation
        if (!supplierClient.isSupplierActive(po.getSupplierId())) {
            throw new RuntimeException("Supplier is inactive");
        }

        //  Set defaults
        po.setStatus(POStatus.DRAFT);
        po.setOrderDate(LocalDateTime.now());
        po.setReferenceNumber("PO-" + System.currentTimeMillis());

        //  Product validation and Cost Calculation
        if (po.getItems() != null && !po.getItems().isEmpty()) {
            po.getItems().forEach(item -> {
                if (item.getSku() != null && !item.getSku().isEmpty()) {
                    // Fetch product by SKU
                    com.stockpro.purchase.dto.ProductDTO productDTO = productClient.getProductBySku(item.getSku());
                    item.setProductId(productDTO.getProductId());
                } else if (item.getProductId() != null) {
                    // Validate product by ID
                    productClient.getProductById(item.getProductId());
                } else {
                    throw new RuntimeException("Line item must have either productId or sku");
                }
            });

            //  Calculate total cost
            po.getItems().forEach(item -> {
                item.setTotalCost(item.getOrderedQuantity() * item.getUnitCost());
                item.setPurchaseOrder(po);
            });

            po.calculateTotalAmount();
        } else {
            po.setTotalAmount(0.0);
        }

        return poRepository.save(po);
    }

    //  SUBMIT PO → Trigger alert later
    @Override
    @CacheEvict(value = "purchaseOrders", allEntries = true)
    public PurchaseOrder submitPO(Long id) {
        PurchaseOrder po = getPO(id);

        po.setStatus(POStatus.PENDING);

        PurchaseOrder saved = poRepository.save(po);

        // [RabbitMQ] Notify alert-service and analytics-service that a PO needs approval
        eventPublisher.publish(saved, RabbitMQConfig.PO_SUBMITTED_ROUTING_KEY);

        return saved;
    }

    //  APPROVE PO
    @Override
    @CacheEvict(value = "purchaseOrders", allEntries = true)
    public PurchaseOrder approvePO(Long id) {
        PurchaseOrder po = getPO(id);

        if (po.getStatus() != POStatus.PENDING) {
            throw new RuntimeException("Only pending PO can be approved");
        }

        po.setStatus(POStatus.APPROVED);
        PurchaseOrder saved = poRepository.save(po);

        // [RabbitMQ] Notify analytics-service that a PO was approved
        eventPublisher.publish(saved, RabbitMQConfig.PO_APPROVED_ROUTING_KEY);

        return saved;
    }

    //  REJECT PO
    @Override
    @CacheEvict(value = "purchaseOrders", allEntries = true)
    public PurchaseOrder rejectPO(Long id, String reason) {
        PurchaseOrder po = getPO(id);

        po.setStatus(POStatus.REJECTED);
        po.setCancelReason(reason);

        return poRepository.save(po);
    }

    //  RECEIVE GOODS (MOST IMPORTANT)
    @Override
    @CacheEvict(value = "purchaseOrders", allEntries = true)
    public PurchaseOrder receiveGoods(Long id, Long productId, Integer receivedQty) {

        PurchaseOrder po = getPO(id);
        assertCurrentUserCanAccessWarehouse(po.getWarehouseId());

        if (po.getStatus() != POStatus.APPROVED &&
            po.getStatus() != POStatus.PARTIALLY_RECEIVED) {
            throw new RuntimeException("PO not eligible for receiving");
        }

        //  Find item
        POLineItem item = po.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found"));

        //  Prevent over-receive
        int newQty = item.getReceivedQuantity() + receivedQty;
        if (newQty > item.getOrderedQuantity()) {
            throw new RuntimeException("Received quantity exceeds ordered");
        }

        item.setReceivedQuantity(newQty);

        //  Update warehouse stock
        warehouseClient.addStock(
                po.getWarehouseId(),
                productId,
                receivedQty,
                "PO Receipt: " + po.getReferenceNumber() // 🔥 Linked Reason
        );

        //  Check overall PO status
        boolean allReceived = po.getItems().stream()
                .allMatch(i -> i.getReceivedQuantity()
                        .equals(i.getOrderedQuantity()));

        if (allReceived) {
            po.setStatus(POStatus.RECEIVED);
            po.setReceivedDate(LocalDateTime.now());
        } else {
            po.setStatus(POStatus.PARTIALLY_RECEIVED);
        }

        PurchaseOrder saved = poRepository.save(po);

        // [RabbitMQ] Always publish on ANY goods receipt (partial or full)
        // → alert-service checks status: PARTIALLY_RECEIVED = GOODS_RECEIVED alert only
        //                                  RECEIVED = GOODS_RECEIVED + PAYMENT_DUE alerts
        eventPublisher.publish(saved, RabbitMQConfig.PO_RECEIVED_ROUTING_KEY);
        log.info("[RabbitMQ] po.received event published: ref={}, status={}, amount={}",
                saved.getReferenceNumber(), saved.getStatus(), saved.getTotalAmount());

        return saved;
    }

    //  CANCEL PO
    @Override
    @CacheEvict(value = "purchaseOrders", allEntries = true)
    public PurchaseOrder cancelPO(Long id, String reason) {
        PurchaseOrder po = getPO(id);

        po.setStatus(POStatus.CANCELLED);
        po.setCancelReason(reason);

        return poRepository.save(po);
    }

    //  FILTER API
    @Override
    @Cacheable(
            value = "purchaseOrders",
            key = "@purchaseCacheKey.scope() + ':' + #supplierId + ':' + #warehouseId + ':' + #status + ':' + #startDate + ':' + #endDate"
    )
    public List<PurchaseOrder> getPOs(Long supplierId,
                                     Long warehouseId,
                                     String status,
                                     LocalDateTime startDate,
                                     LocalDateTime endDate) {

        if (supplierId != null) {
            return detachPurchaseOrders(poRepository.findBySupplierId(supplierId));
        }

        if (warehouseId != null) {
            return detachPurchaseOrders(poRepository.findByWarehouseId(warehouseId));
        }

        if (status != null) {
            return detachPurchaseOrders(poRepository.findByStatus(POStatus.valueOf(status)));
        }

        if (startDate != null && endDate != null) {
            return detachPurchaseOrders(poRepository.findByOrderDateBetween(startDate, endDate));
        }

        return detachPurchaseOrders(poRepository.findAll());
    }

    //  Helper
    private PurchaseOrder getPO(Long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PO not found"));
    }

    // ✅ Resolves a warehouse department name → warehouseId for MANAGER/OFFICER scoping
    private void assertCurrentUserCanAccessWarehouse(Long warehouseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return;
        }

        boolean isAdminOrOfficer = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_OFFICER".equals(role));
        if (isAdminOrOfficer) {
            return;
        }

        boolean isWarehouseScoped = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(role -> "ROLE_MANAGER".equals(role) || "ROLE_STAFF".equals(role));
        if (!isWarehouseScoped) {
            throw new AccessDeniedException("You are not allowed to access this purchase order");
        }

        String department = auth.getDetails() instanceof String ? (String) auth.getDetails() : null;
        Long userWarehouseId = department == null || department.isBlank() ? null : resolveWarehouseId(department);
        if (warehouseId == null || userWarehouseId == null || !warehouseId.equals(userWarehouseId)) {
            throw new AccessDeniedException("You can receive goods only for your assigned warehouse");
        }
    }

    @Override
    @Cacheable(value = "purchaseWarehouseLookup", key = "#warehouseName")
    public Long resolveWarehouseId(String warehouseName) {
        try {
            return warehouseClient.getAllWarehouses()
                    .stream()
                    .filter(w -> warehouseName.equals(w.get("name")))
                    .map(w -> {
                        Object id = w.get("warehouseId");
                        if (id instanceof Number) return ((Number) id).longValue();
                        return null;
                    })
                    .filter(id -> id != null)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            // If warehouse-service is down, return null (no scoping — safe fallback)
            return null;
        }
    }

    private List<PurchaseOrder> detachPurchaseOrders(List<PurchaseOrder> orders) {
        return orders.stream().map(this::detachPurchaseOrder).toList();
    }

    private PurchaseOrder detachPurchaseOrder(PurchaseOrder po) {
        List<POLineItem> items = po.getItems() == null
                ? List.of()
                : po.getItems().stream()
                .map(this::detachLineItem)
                .toList();

        return PurchaseOrder.builder()
                .id(po.getId())
                .referenceNumber(po.getReferenceNumber())
                .supplierId(po.getSupplierId())
                .warehouseId(po.getWarehouseId())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .orderDate(po.getOrderDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .receivedDate(po.getReceivedDate())
                .notes(po.getNotes())
                .createdById(po.getCreatedById())
                .createdBy(po.getCreatedBy())
                .cancelReason(po.getCancelReason())
                .items(new ArrayList<>(items))
                .build();
    }

    private POLineItem detachLineItem(POLineItem item) {
        return POLineItem.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .sku(item.getSku())
                .orderedQuantity(item.getOrderedQuantity())
                .receivedQuantity(item.getReceivedQuantity())
                .unitCost(item.getUnitCost())
                .totalCost(item.getTotalCost())
                .build();
    }
}
