package com.stockpro.alert.listerner;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.stockpro.alert.config.RabbitMQConfig;
import com.stockpro.alert.dto.PurchaseOrderEvent;
import com.stockpro.alert.dto.StockAlertEvent;
import com.stockpro.alert.entity.AlertSeverity;
import com.stockpro.alert.entity.AlertType;
import com.stockpro.alert.service.AlertService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertEventListener {

    private final AlertService alertService;

    @RabbitListener(queues = RabbitMQConfig.LOW_STOCK_QUEUE)
    public void lowStock(StockAlertEvent e) {
        log.info(" [RabbitMQ] Received LOW STOCK event: productId={}, qty={}",
                e.getProductId(), e.getCurrentQty());
        try {
            alertService.createAlert(
                    AlertType.LOW_STOCK,
                    AlertSeverity.CRITICAL,
                    "Low Stock Alert",
                    "Product #" + e.getProductId() + " is low: only " + e.getCurrentQty() + " units remaining",
                    e.getProductId(),
                    e.getWarehouseId(),
                    null
            );
            log.info(" LOW_STOCK alert saved for product {}", e.getProductId());
        } catch (Exception ex) {
            log.error(" Failed to create LOW_STOCK alert: {}", ex.getMessage(), ex);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.OVERSTOCK_QUEUE)
    public void overStock(StockAlertEvent e) {
        log.info(" [RabbitMQ] Received OVERSTOCK event: productId={}, qty={}",
                e.getProductId(), e.getCurrentQty());
        try {
            alertService.createAlert(
                    AlertType.OVERSTOCK,
                    AlertSeverity.WARNING,
                    "Overstock Alert",
                    "Product #" + e.getProductId() + " exceeds max level: " + e.getCurrentQty() + " units",
                    e.getProductId(),
                    e.getWarehouseId(),
                    null
            );
            log.info(" OVERSTOCK alert saved for product {}", e.getProductId());
        } catch (Exception ex) {
            log.error(" Failed to create OVERSTOCK alert: {}", ex.getMessage(), ex);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PO_PENDING_QUEUE)
    public void poPending(PurchaseOrderEvent e) {
        alertService.createAlert(
                AlertType.PO_PENDING,
                AlertSeverity.INFO,
                "PO Pending Approval",
                "PO " + e.getReferenceNumber() + " is awaiting your approval",
                null,
                e.getWarehouseId(),
                e.getPoId()
        );
    }

    //  Fires on EVERY goods receipt (partial OR full)
    @RabbitListener(queues = RabbitMQConfig.PO_RECEIVED_QUEUE)
    public void poReceived(PurchaseOrderEvent e) {
        log.info(" [RabbitMQ] po.received event: ref={}, status={}, amount={}",
                e.getReferenceNumber(), e.getStatus(), e.getTotalAmount());
        try {
            boolean isFullyReceived = "RECEIVED".equals(e.getStatus());
            String amountText = e.getTotalAmount() != null
                    ? String.format("₹%.2f", e.getTotalAmount())
                    : "N/A";

            // Always send GOODS_RECEIVED alert (partial or full)
            String goodsMsg = isFullyReceived
                    ? "All items for PO " + e.getReferenceNumber()
                        + " (Warehouse #" + e.getWarehouseId() + ") have been fully received into stock."
                    : "Goods partially received for PO " + e.getReferenceNumber()
                        + " (Warehouse #" + e.getWarehouseId() + "). Some items still pending delivery.";

            alertService.createAlert(
                    AlertType.GOODS_RECEIVED,
                    AlertSeverity.INFO,
                    isFullyReceived ? "Goods Fully Received" : "Goods Partially Received",
                    goodsMsg,
                    null,
                    e.getWarehouseId(),
                    e.getPoId()
            );
            log.info(" GOODS_RECEIVED alert created for PO {}", e.getReferenceNumber());

            // Only send PAYMENT_DUE when ALL goods are received
            if (isFullyReceived) {
                alertService.createAlert(
                        AlertType.PAYMENT_DUE,
                        AlertSeverity.CRITICAL,
                        "Payment Due — Action Required",
                        "All goods for PO " + e.getReferenceNumber()
                                + " have been received. Payment of " + amountText
                                + " is now due to Supplier #" + e.getSupplierId()
                                + ". Please process the payment immediately.",
                        null,
                        e.getWarehouseId(),
                        e.getPoId()
                );
                log.info(" PAYMENT_DUE alert created for PO {}", e.getReferenceNumber());
            }

        } catch (Exception ex) {
            log.error(" Failed to create alerts for PO {}: {}", e.getReferenceNumber(), ex.getMessage(), ex);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_TRANSFER_REQUEST_QUEUE)
    public void stockTransferRequested(java.util.Map<String, Object> event) {
        log.info(" [RabbitMQ] stock.transfer.request received: {}", event);
        try {
            Long requestId = event.get("requestId") != null ? ((Number) event.get("requestId")).longValue() : null;
            Long productId = event.get("productId") != null ? ((Number) event.get("productId")).longValue() : null;
            Long requestingWarehouseId = event.get("requestingWarehouseId") != null
                    ? ((Number) event.get("requestingWarehouseId")).longValue() : null;
            Integer quantity = event.get("quantity") != null ? ((Number) event.get("quantity")).intValue() : 0;
            String reason = event.get("reason") != null ? event.get("reason").toString() : "";
            String requestedBy = event.get("requestedBy") != null ? event.get("requestedBy").toString() : "Unknown";

            Object candidates = event.get("candidateWarehouseIds");
            if (!(candidates instanceof java.util.List<?> candidateWarehouseIds)) {
                return;
            }

            for (Object candidate : candidateWarehouseIds) {
                Long candidateWarehouseId = ((Number) candidate).longValue();
                String msg = "Warehouse #" + requestingWarehouseId
                        + " needs " + quantity + " unit(s) of Product #" + productId
                        + ". Requested by " + requestedBy
                        + (reason.isBlank() ? "" : ". Reason: " + reason)
                        + ". Accept request #" + requestId + " to start transfer.";

                alertService.createTransferRequestAlert(
                        requestId,
                        "Stock Transfer Request #" + requestId,
                        msg,
                        productId,
                        candidateWarehouseId
                );
            }
        } catch (Exception ex) {
            log.error(" Failed to create STOCK_TRANSFER_REQUEST alerts: {}", ex.getMessage(), ex);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.STOCK_TRANSFER_REQUEST_COMPLETED_QUEUE)
    public void stockTransferRequestCompleted(java.util.Map<String, Object> event) {
        log.info(" [RabbitMQ] stock.transfer.request.completed received: {}", event);
        try {
            Long requestId = event.get("requestId") != null ? ((Number) event.get("requestId")).longValue() : null;
            if (requestId != null) {
                alertService.acknowledgeTransferRequestAlerts(requestId);
            }
        } catch (Exception ex) {
            log.error(" Failed to close STOCK_TRANSFER_REQUEST alerts: {}", ex.getMessage(), ex);
        }
    }

    // ✅ Fires when staff transfers stock between warehouses → notifies destination warehouse
    @RabbitListener(queues = RabbitMQConfig.STOCK_TRANSFER_QUEUE)
    public void stockTransferred(java.util.Map<String, Object> event) {
        log.info(" [RabbitMQ] stock.transfer received: {}", event);
        try {
            Long productId       = event.get("productId")       != null ? ((Number) event.get("productId")).longValue()       : null;
            Long fromWarehouseId = event.get("fromWarehouseId") != null ? ((Number) event.get("fromWarehouseId")).longValue() : null;
            Long toWarehouseId   = event.get("toWarehouseId")   != null ? ((Number) event.get("toWarehouseId")).longValue()   : null;
            Integer quantity     = event.get("quantity")         != null ? ((Number) event.get("quantity")).intValue()         : 0;
            String reason        = event.get("reason")           != null ? event.get("reason").toString()                     : "";
            String performedBy   = event.get("performedBy")      != null ? event.get("performedBy").toString()                 : "Unknown";

            String msg = quantity + " unit(s) of Product #" + productId
                    + " transferred from Warehouse #" + fromWarehouseId
                    + " → Warehouse #" + toWarehouseId
                    + " by " + performedBy
                    + (reason.isBlank() ? "" : ". Reason: " + reason);

            alertService.createAlert(
                    AlertType.STOCK_TRANSFER,
                    AlertSeverity.INFO,
                    "Incoming Stock Transfer — Warehouse #" + toWarehouseId,
                    msg,
                    productId,
                    toWarehouseId,   // destination warehouse stores alert for "Receive" button
                    null
            );
            log.info(" STOCK_TRANSFER alert created for destination Warehouse #{}", toWarehouseId);

        } catch (Exception ex) {
            log.error(" Failed to create STOCK_TRANSFER alert: {}", ex.getMessage(), ex);
        }
    }
}
