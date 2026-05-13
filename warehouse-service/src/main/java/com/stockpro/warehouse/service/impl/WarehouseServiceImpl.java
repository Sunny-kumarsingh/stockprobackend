package com.stockpro.warehouse.service.impl;

import com.stockpro.warehouse.client.MovementClient;
import com.stockpro.warehouse.client.ProductClient;
import com.stockpro.warehouse.dto.StockIssueRequest;
import com.stockpro.warehouse.dto.StockWriteOffRequest;
import com.stockpro.warehouse.dto.StockReturnRequest;
import com.stockpro.warehouse.entity.MovementType;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.StockMovement;
import com.stockpro.warehouse.entity.StockTransferRequest;
import com.stockpro.warehouse.entity.TransferRequestStatus;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.publisher.StockEventPublisher;
import com.stockpro.warehouse.repository.StockLevelRepository;
import com.stockpro.warehouse.repository.StockTransferRequestRepository;
import com.stockpro.warehouse.repository.WarehouseRepository;
import com.stockpro.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseServiceImpl.class);

    private final WarehouseRepository warehouseRepo;
    private final StockLevelRepository stockRepo;
    private final StockTransferRequestRepository transferRequestRepo;
    private final MovementClient movementClient;   // 🔥 Feign → stockmovement-service
    private final ProductClient productClient;
    private final StockEventPublisher stockEventPublisher; // 📡 RabbitMQ Publisher (non-critical)

    
    private String getCurrentUser() {
        try {
            return SecurityContextHolder.getContext()
                   .getAuthentication().getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }



    //  Create warehouse
    @Override
    @CacheEvict(value = {"warehouses", "warehouse"}, allEntries = true)
    public Warehouse createWarehouse(Warehouse warehouse) {
        warehouse.setIsActive(true);
        warehouse.setUsedCapacity(0); // 🔥 Initialize at 0
        return warehouseRepo.save(warehouse);
    }

    //  Get all warehouses
    @Override
    @Cacheable(value = "warehouses")
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepo.findAll();
    }
    
 //  Get single warehouse by ID
    @Override
    @Cacheable(value = "warehouse", key = "#p0")
    public Warehouse getWarehouseById(Long id) {
        return warehouseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found: " + id));
    }

    //  Update warehouse fields (name, location, address, phone, capacity)
    @Override
    @CacheEvict(value = {"warehouses", "warehouse"}, allEntries = true)
    public Warehouse updateWarehouse(Long id, Warehouse updated) {
        Warehouse existing = getWarehouseById(id);
        existing.setName(updated.getName());
        existing.setLocation(updated.getLocation());
        existing.setAddress(updated.getAddress());
        existing.setPhone(updated.getPhone());
        if (updated.getCapacity() != null) {
            existing.setCapacity(updated.getCapacity());
        }
        return warehouseRepo.save(existing);
    }

    //  Activate or deactivate a warehouse
    @Override
    @CacheEvict(value = {"warehouses", "warehouse"}, allEntries = true)
    public void setWarehouseActive(Long id, boolean active) {
        Warehouse warehouse = getWarehouseById(id);
        warehouse.setIsActive(active);
        warehouseRepo.save(warehouse);
    }

    //  Assign a manager to a warehouse
    @Override
    @CacheEvict(value = {"warehouses", "warehouse"}, allEntries = true)
    public void assignManager(Long warehouseId, Long managerId) {
        Warehouse warehouse = getWarehouseById(warehouseId);
        warehouse.setManagerId(managerId);
        warehouseRepo.save(warehouse);
    }


    //  Get stock
    @Override
    @Cacheable(value = "warehouseStock", key = "#p0 + ':' + #p1")
    public StockLevel getStock(Long warehouseId, Long productId) {
        return stockRepo.findByWarehouseId(warehouseId).stream()
                .filter(s -> s.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Stock not found"));
    }

    //  Update stock
    @Override
    @CacheEvict(value = {"warehouses", "warehouse", "warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public StockLevel updateStock(Long warehouseId, Long productId, int quantity, String reason) {

        //  0. Validate Product via Product Service (Feign)
         try {
             productClient.getProductById(productId);
         } catch (Exception e) {
             throw new RuntimeException("Invalid Product ID: " + productId + " (Product not found) - Details: " + e.getMessage());
         } 

        // 1. Fetch Warehouse
        Warehouse warehouse = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        // 2. Fetch existing stock (Handle duplicates gracefully if any exist)
        StockLevel stock = stockRepo.findByWarehouseId(warehouseId).stream()
                .filter(s -> s.getProductId().equals(productId))
                .findFirst()
                .orElse(new StockLevel());

        int oldQuantity = (stock.getQuantity() != null) ? stock.getQuantity() : 0;
        int delta = quantity - oldQuantity;

        // 3. Capacity Validation
        int currentUsedCapacity = (warehouse.getUsedCapacity() != null) ? warehouse.getUsedCapacity() : 0;
        int maxCapacity = (warehouse.getCapacity() != null) ? warehouse.getCapacity() : Integer.MAX_VALUE;
        if (currentUsedCapacity + delta > maxCapacity) {
            throw new RuntimeException("Warehouse capacity exceeded! 🛑 Max: " + maxCapacity);
        }

        // 4. Update Stock
        stock.setWarehouseId(warehouseId);
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        stock.setReservedQuantity(0);
        stock.setLastUpdated(LocalDateTime.now());

        // 5. Update Warehouse Capacity
        warehouse.setUsedCapacity(currentUsedCapacity + delta);
        warehouseRepo.save(warehouse);

        // 6. Record Movement
        MovementType movementType = (delta >= 0) ? MovementType.IN : MovementType.OUT;
        recordMovement(warehouseId, productId, Math.abs(delta), movementType, reason); // 🔥 Using dynamic reason

        StockLevel savedStock = stockRepo.save(stock);

        //  Step 1: Existing Feign sync to product-service (unchanged)
        publishStockUpdateEvent(productId);

        //  Step 2: RabbitMQ event for analytics-service (non-blocking)
        stockEventPublisher.publishStockMovement(
                productId, warehouseId, Math.abs(delta),
                movementType.name(), reason
        );

        //  Step 3: PDF §2.7 — Check low-stock AND overstock thresholds
        try {
            int minThreshold = (savedStock.getMinThreshold() != null) ? savedStock.getMinThreshold() : 25;
            int maxLevel = (savedStock.getMaxStockLevel() != null) ? savedStock.getMaxStockLevel() : 1000;

            if (savedStock.getQuantity() <= minThreshold && minThreshold > 0) {
                // LOW STOCK — CRITICAL
                stockEventPublisher.publishStockAlert(
                        productId, warehouseId, savedStock.getQuantity(),
                        com.stockpro.warehouse.config.RabbitMQConfig.STOCK_LOW_ROUTING_KEY
                );
            } else if (savedStock.getQuantity() > maxLevel) {
                // OVERSTOCK — WARNING
                stockEventPublisher.publishStockAlert(
                        productId, warehouseId, savedStock.getQuantity(),
                        com.stockpro.warehouse.config.RabbitMQConfig.STOCK_HIGH_ROUTING_KEY
                );
            }
        } catch (Exception e) {
            // Never break the stock flow
        }

        return savedStock;
    }

    //  NEW: Incremental Stock Update for Purchase Receipts
    @Override
    @CacheEvict(value = {"warehouses", "warehouse", "warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public StockLevel addStock(Long warehouseId, Long productId, int delta, String reason) {
        // 1. Fetch current stock (or create new)
        StockLevel stock = stockRepo.findByWarehouseId(warehouseId).stream()
                .filter(s -> s.getProductId().equals(productId))
                .findFirst()
                .orElseGet(() -> {
                    StockLevel s = new StockLevel();
                    s.setWarehouseId(warehouseId);
                    s.setProductId(productId);
                    s.setQuantity(0);
                    s.setReservedQuantity(0);
                    return s;
                });

        int newTotal = stock.getQuantity() + delta;
        
        // 2. Reuse updateStock logic by calling it with the NEW total
        // This ensures Capacity validation and Movement recording are handled correctly!
        return updateStock(warehouseId, productId, newTotal, reason);
    }

    //  Feign: Sync total global stock to Product Service
    private void publishStockUpdateEvent(Long productId) {
        try {
            int totalStock = stockRepo.findByProductId(productId)
                    .stream().mapToInt(StockLevel::getQuantity).sum();
            productClient.updateTotalStock(productId, totalStock);
            log.info("[Feign] Stock synced for Product {} -> {} units", productId, totalStock);
        } catch (Exception e) {
            log.warn("Could not sync stock to Product Service: {}", e.getMessage());
        }
    }

    //  Delete orphan stock entry (admin cleanup)
    @Override
    @CacheEvict(value = {"warehouses", "warehouse", "warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public void deleteStockEntry(Long warehouseId, Long productId) {
        StockLevel stock = stockRepo.findByWarehouseId(warehouseId).stream()
                .filter(s -> s.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Stock entry not found for warehouse " + warehouseId + " / product " + productId));

        // Correct warehouse usedCapacity
        Warehouse warehouse = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        int currentUsed = (warehouse.getUsedCapacity() != null) ? warehouse.getUsedCapacity() : 0;
        int stockQty   = (stock.getQuantity() != null) ? stock.getQuantity() : 0;
        warehouse.setUsedCapacity(Math.max(0, currentUsed - stockQty));
        warehouseRepo.save(warehouse);

        stockRepo.delete(stock);
        log.info("Stock entry deleted: warehouse={}, product={}", warehouseId, productId);
    }
    
    //updatethreshold
    @Override
    @CacheEvict(value = {"warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public void updateStockThreshold(Long warehouseId, Long productId, int threshold) {
        StockLevel stock = stockRepo.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new RuntimeException("Stock entry not found"));
        stock.setMinThreshold(threshold);
        StockLevel saved = stockRepo.save(stock);
        publishThresholdAlertIfNeeded(saved);
    }


    //  Reserve stock
    @Override
    @CacheEvict(value = {"warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public void reserveStock(Long warehouseId, Long productId, int qty) {

        StockLevel stock = getStock(warehouseId, productId);

        if (stock.getAvailableQuantity() < qty) {
            throw new RuntimeException("Not enough available stock");
        }

        stock.setReservedQuantity(stock.getReservedQuantity() + qty);
        stockRepo.save(stock);
    }

    //  Release stock
    @Override
    @CacheEvict(value = {"warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public void releaseReservation(Long warehouseId, Long productId, int qty) {

        StockLevel stock = getStock(warehouseId, productId);

        stock.setReservedQuantity(stock.getReservedQuantity() - qty);
        stockRepo.save(stock);
    }
    
    
    @Override
    @Cacheable(value = "lowStockReport", key = "#p0")
    public List<StockLevel> getLowStockReport(Long warehouseId) {

        List<StockLevel> allStock = stockRepo.findByWarehouseId(warehouseId);

        return allStock.stream()
                .filter(StockLevel::isLowStock)
                .toList();
    }

    @Override
    @Cacheable(value = "warehouseInventory", key = "#p0")
    public List<StockLevel> getWarehouseInventory(Long warehouseId) {
        return stockRepo.findByWarehouseId(warehouseId);
    }
    // Transfer stock
    @Override
    @CacheEvict(value = {"warehouses", "warehouse", "warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public void transferStock(Long productId, Long fromWarehouse, Long toWarehouse, int qty, String reason) {
        executeTransfer(productId, fromWarehouse, toWarehouse, qty, reason);
    }

    @Override
    public StockTransferRequest requestStockTransfer(Long productId, Long requestingWarehouseId, int qty, String reason) {
        if (qty <= 0) {
            throw new RuntimeException("Transfer quantity must be greater than zero");
        }

        try {
            productClient.getProductById(productId);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Product ID: " + productId + " (Product not found) - Details: " + e.getMessage());
        }

        warehouseRepo.findById(requestingWarehouseId)
                .orElseThrow(() -> new RuntimeException("Requesting warehouse not found: " + requestingWarehouseId));

        List<Long> candidateWarehouseIds = stockRepo.findByProductId(productId).stream()
                .filter(stock -> !requestingWarehouseId.equals(stock.getWarehouseId()))
                .filter(stock -> stock.getQuantity() != null && stock.getQuantity() >= qty)
                .map(StockLevel::getWarehouseId)
                .distinct()
                .toList();

        if (candidateWarehouseIds.isEmpty()) {
            throw new RuntimeException("No other warehouse has enough available stock for this product");
        }

        StockTransferRequest request = StockTransferRequest.builder()
                .productId(productId)
                .requestingWarehouseId(requestingWarehouseId)
                .quantity(qty)
                .reason(reason)
                .requestedBy(getCurrentUser())
                .status(TransferRequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        StockTransferRequest saved = transferRequestRepo.save(request);
        stockEventPublisher.publishTransferRequestEvent(
                saved.getRequestId(),
                productId,
                requestingWarehouseId,
                qty,
                reason,
                saved.getRequestedBy(),
                candidateWarehouseIds
        );
        return saved;
    }

    @Override
    @CacheEvict(value = {"warehouses", "warehouse", "warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public StockTransferRequest acceptTransferRequest(Long requestId, Long fromWarehouseId) {
        StockTransferRequest request = transferRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Transfer request not found: " + requestId));

        if (request.getStatus() != TransferRequestStatus.PENDING) {
            throw new RuntimeException("Transfer request is already " + request.getStatus());
        }

        executeTransfer(
                request.getProductId(),
                fromWarehouseId,
                request.getRequestingWarehouseId(),
                request.getQuantity(),
                "REQUEST #" + request.getRequestId() + " | " + request.getReason()
        );

        request.setAcceptedFromWarehouseId(fromWarehouseId);
        request.setAcceptedBy(getCurrentUser());
        request.setAcceptedAt(LocalDateTime.now());
        request.setCompletedAt(LocalDateTime.now());
        request.setStatus(TransferRequestStatus.COMPLETED);
        StockTransferRequest saved = transferRequestRepo.save(request);
        stockEventPublisher.publishTransferRequestCompleted(saved.getRequestId());
        return saved;
    }

    @Override
    public List<StockTransferRequest> getTransferRequestsForWarehouse(Long warehouseId) {
        return transferRequestRepo.findByRequestingWarehouseIdOrderByRequestedAtDesc(warehouseId);
    }

    @Override
    public List<StockTransferRequest> getPendingTransferRequests() {
        return transferRequestRepo.findByStatusOrderByRequestedAtDesc(TransferRequestStatus.PENDING);
    }

    private void executeTransfer(Long productId, Long fromWarehouse, Long toWarehouse, int qty, String reason) {

        //  1. Source MUST exist and have enough stock
        StockLevel source = getStock(fromWarehouse, productId);
        if (source.getQuantity() < qty) {
            throw new RuntimeException("Insufficient stock in source warehouse!");
        }

        //                      2. Target - If not exists, create it!
        StockLevel target = stockRepo.findByWarehouseId(toWarehouse).stream()
                .filter(s -> s.getProductId().equals(productId))
                .findFirst()
                .orElseGet(() -> {
                    StockLevel newStock = new StockLevel();
                    newStock.setWarehouseId(toWarehouse);
                    newStock.setProductId(productId);
                    newStock.setQuantity(0);
                    newStock.setReservedQuantity(0);
                    newStock.setMinThreshold(5); // Default
                    return newStock;
                });
        
        // 1. Fetch Warehouse Objects
        Warehouse sourceW = warehouseRepo.findById(fromWarehouse).orElseThrow();
        Warehouse targetW = warehouseRepo.findById(toWarehouse).orElseThrow();

        // 3. Validate Target Capacity
        int targetUsedCapacity = (targetW.getUsedCapacity() != null) ? targetW.getUsedCapacity() : 0;
        int targetMaxCapacity = (targetW.getCapacity() != null) ? targetW.getCapacity() : Integer.MAX_VALUE;
        if (targetUsedCapacity + qty > targetMaxCapacity) {
            throw new RuntimeException("Target Warehouse capacity exceeded!  Max: " + targetMaxCapacity);
        }

        // 4. Update Stock Levels
        source.setQuantity(source.getQuantity() - qty);
        target.setQuantity(target.getQuantity() + qty);

        // 5. Update Warehouse usedCapacity
        int sourceUsedCapacity = (sourceW.getUsedCapacity() != null) ? sourceW.getUsedCapacity() : 0;
        sourceW.setUsedCapacity(sourceUsedCapacity - qty);
        targetW.setUsedCapacity(targetUsedCapacity + qty);

        // 6. Record Movements
        recordMovement(fromWarehouse, productId, -qty, MovementType.TRANSFER, reason);
        recordMovement(toWarehouse, productId, qty, MovementType.TRANSFER, reason);

        warehouseRepo.save(sourceW);
        warehouseRepo.save(targetW);
        stockRepo.save(source);
        stockRepo.save(target);

        //  Step 1: Existing Feign sync (unchanged)
        publishStockUpdateEvent(productId);

        //  Step 2: NEW RabbitMQ event for analytics-service (non-blocking)
        stockEventPublisher.publishStockMovement(
                productId, fromWarehouse, qty, "TRANSFER", reason
        );

        //  Step 3: STOCK_TRANSFER alert → alert-service notifies destination warehouse
        stockEventPublisher.publishTransferEvent(
                productId, fromWarehouse, toWarehouse, qty, reason, getCurrentUser()
        );

        publishThresholdAlertIfNeeded(source);
        publishThresholdAlertIfNeeded(target);
    }
    
    //  Records movement via Feign → stockmovement-service
    // Warehouse service no longer stores movements itself
    private void recordMovement(Long warehouseId, Long productId, int quantity, MovementType type, String reason) {
        StockMovement movement = new StockMovement();
        movement.setWarehouseId(warehouseId);
        movement.setProductId(productId);
        movement.setQuantity(quantity);
        movement.setType(type);
        movement.setReason(reason);
        movement.setPerformedBy(getCurrentUser());
        movement.setTimestamp(LocalDateTime.now());

        try {
            movementClient.saveMovement(movement);
        } catch (Exception e) {
            // Log but don't fail — movement recording is non-critical
            log.warn("Could not record movement to stock-movement-service: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // STOCK ISSUE — Consumption (Sales / Production / Internal Use)
    // PDF §2.2: "Record stock issues/consumption for production, sales, or internal use"
    // ─────────────────────────────────────────────────────────────
    @Override
    @CacheEvict(value = {"warehouses", "warehouse", "warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public StockLevel issueStock(StockIssueRequest request) {

        // 1. Validate issue type
        String issueType = request.getIssueType().toUpperCase();
        if (!issueType.equals("SALES") && !issueType.equals("PRODUCTION") && !issueType.equals("INTERNAL_USE")) {
            throw new RuntimeException("Invalid issue type. Must be: SALES, PRODUCTION, or INTERNAL_USE");
        }

        // 2. Fetch stock record
        StockLevel stock = getStock(request.getWarehouseId(), request.getProductId());

        // 3. Check available quantity (cannot issue reserved stock)
        int available = stock.getAvailableQuantity();
        if (request.getQuantity() > available) {
            throw new RuntimeException(
                "Insufficient available stock. Requested: " + request.getQuantity() +
                ", Available: " + available + " (Total: " + stock.getQuantity() +
                ", Reserved: " + stock.getReservedQuantity() + ")"
            );
        }

        // 4. Deduct quantity
        int newQty = stock.getQuantity() - request.getQuantity();
        stock.setQuantity(newQty);
        stock.setLastUpdated(LocalDateTime.now());

        // 5. Update warehouse used capacity
        Warehouse warehouse = warehouseRepo.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        int currentUsed = warehouse.getUsedCapacity() != null ? warehouse.getUsedCapacity() : 0;
        warehouse.setUsedCapacity(Math.max(0, currentUsed - request.getQuantity()));
        warehouseRepo.save(warehouse);

        // 6. Save updated stock
        StockLevel saved = stockRepo.save(stock);

        // 7. Build reason string: e.g. "ISSUE:SALES | Sales Order #1234"
        String reason = "ISSUE:" + issueType +
                (request.getNotes() != null ? " | " + request.getNotes() : "");

        // 8. Record movement as ISSUE type
        recordMovement(request.getWarehouseId(), request.getProductId(),
                request.getQuantity(), MovementType.ISSUE, reason);

        // 9. Feign sync to product-service
        publishStockUpdateEvent(request.getProductId());

        // 10. RabbitMQ event to analytics-service
        stockEventPublisher.publishStockMovement(
                request.getProductId(), request.getWarehouseId(),
                request.getQuantity(), "ISSUE", reason
        );

        // 11. Check low-stock threshold after issue
        try {
            int minThreshold = (saved.getMinThreshold() != null) ? saved.getMinThreshold() : 25;
            if (saved.getQuantity() <= minThreshold && minThreshold > 0) {
                stockEventPublisher.publishStockAlert(
                        request.getProductId(),
                        request.getWarehouseId(),
                        saved.getQuantity(),
                        com.stockpro.warehouse.config.RabbitMQConfig.STOCK_LOW_ROUTING_KEY
                );
            }
        } catch (Exception e) {
            // Never break the stock flow for alert failures
        }

        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    // STOCK WRITE-OFF — Damaged / Expired goods (PDF §2.6)
    // ─────────────────────────────────────────────────────────────
    @Override
    @CacheEvict(value = {"warehouses", "warehouse", "warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public StockLevel writeOffStock(StockWriteOffRequest request) {

        StockLevel stock = getStock(request.getWarehouseId(), request.getProductId());

        if (request.getQuantity() > stock.getQuantity()) {
            throw new RuntimeException(
                "Cannot write off more than available stock. Available: " + stock.getQuantity());
        }

        // Deduct stock
        stock.setQuantity(stock.getQuantity() - request.getQuantity());
        stock.setLastUpdated(LocalDateTime.now());
        StockLevel saved = stockRepo.save(stock);

        // Update warehouse capacity
        Warehouse warehouse = warehouseRepo.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        int currentUsed = warehouse.getUsedCapacity() != null ? warehouse.getUsedCapacity() : 0;
        warehouse.setUsedCapacity(Math.max(0, currentUsed - request.getQuantity()));
        warehouseRepo.save(warehouse);

        // Build reason string
        String reason = "WRITE_OFF:" + request.getWriteOffReason() +
                (request.getNotes() != null ? " | " + request.getNotes() : "");

        // Record movement as WRITE_OFF
        recordMovement(request.getWarehouseId(), request.getProductId(),
                request.getQuantity(), MovementType.WRITE_OFF, reason);

        // Feign sync
        publishStockUpdateEvent(request.getProductId());

        // RabbitMQ event
        stockEventPublisher.publishStockMovement(
                request.getProductId(), request.getWarehouseId(),
                request.getQuantity(), "WRITE_OFF", reason);

        publishThresholdAlertIfNeeded(saved);

        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    // STOCK RETURN — Supplier or Customer Return (PDF §2.6)
    // ─────────────────────────────────────────────────────────────
    @Override
    @CacheEvict(value = {"warehouses", "warehouse", "warehouseInventory", "warehouseStock", "lowStockReport"}, allEntries = true)
    public StockLevel returnStock(StockReturnRequest request) {

        String returnType = request.getReturnType().toUpperCase();
        if (!returnType.equals("SUPPLIER_RETURN") && !returnType.equals("CUSTOMER_RETURN")) {
            throw new RuntimeException("Invalid return type. Must be: SUPPLIER_RETURN or CUSTOMER_RETURN");
        }

        StockLevel stock = getStock(request.getWarehouseId(), request.getProductId());

        // Returns ADD back to stock
        stock.setQuantity(stock.getQuantity() + request.getQuantity());
        stock.setLastUpdated(LocalDateTime.now());
        StockLevel saved = stockRepo.save(stock);

        // Update warehouse capacity
        Warehouse warehouse = warehouseRepo.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        int currentUsed = warehouse.getUsedCapacity() != null ? warehouse.getUsedCapacity() : 0;
        warehouse.setUsedCapacity(currentUsed + request.getQuantity());
        warehouseRepo.save(warehouse);

        // Build reason string
        String reason = "RETURN:" + returnType +
                (request.getReferenceNumber() != null ? " | Ref: " + request.getReferenceNumber() : "") +
                (request.getNotes() != null ? " | " + request.getNotes() : "");

        // Record movement as RETURN
        recordMovement(request.getWarehouseId(), request.getProductId(),
                request.getQuantity(), MovementType.RETURN, reason);

        // Feign sync
        publishStockUpdateEvent(request.getProductId());

        // RabbitMQ event
        stockEventPublisher.publishStockMovement(
                request.getProductId(), request.getWarehouseId(),
                request.getQuantity(), "RETURN", reason);

        publishThresholdAlertIfNeeded(saved);

        return saved;
    }

    private void publishThresholdAlertIfNeeded(StockLevel stock) {
        if (stock == null) {
            return;
        }
        try {
            int quantity = (stock.getQuantity() != null) ? stock.getQuantity() : 0;
            int minThreshold = (stock.getMinThreshold() != null) ? stock.getMinThreshold() : 25;
            int maxLevel = (stock.getMaxStockLevel() != null) ? stock.getMaxStockLevel() : 1000;

            if (minThreshold > 0 && quantity <= minThreshold) {
                stockEventPublisher.publishStockAlert(
                        stock.getProductId(),
                        stock.getWarehouseId(),
                        quantity,
                        com.stockpro.warehouse.config.RabbitMQConfig.STOCK_LOW_ROUTING_KEY
                );
            } else if (quantity > maxLevel) {
                stockEventPublisher.publishStockAlert(
                        stock.getProductId(),
                        stock.getWarehouseId(),
                        quantity,
                        com.stockpro.warehouse.config.RabbitMQConfig.STOCK_HIGH_ROUTING_KEY
                );
            }
        } catch (Exception e) {
            log.warn("Could not publish stock threshold alert: {}", e.getMessage());
        }
    }

}
