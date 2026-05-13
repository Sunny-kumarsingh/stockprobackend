package com.stockpro.warehouse.publisher;

import com.stockpro.warehouse.config.RabbitMQConfig;
import com.stockpro.warehouse.dto.StockMovementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class StockEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a stock movement event to RabbitMQ.
     * Wrapped in try-catch so RabbitMQ failure NEVER breaks the main stock flow.
     */
    public void publishStockMovement(Long productId, Long warehouseId,
                                     Integer quantity, String movementType, String reason) {
        try {
            StockMovementEvent event = new StockMovementEvent(
                    productId,
                    warehouseId,
                    quantity,
                    movementType,
                    reason,
                    LocalDateTime.now()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.STOCK_MOVEMENT_ROUTING_KEY,
                    event
            );

            log.info("[RabbitMQ] Stock event published: Product={}, Type={}, Qty={}",
                    productId, movementType, quantity);

        } catch (Exception e) {
            log.warn("[RabbitMQ] Failed to publish stock event for Product {}: {}", productId, e.getMessage());
        }
    }

    /**
     * Publishes a dedicated STOCK_TRANSFER event so alert-service can notify
     * the destination warehouse that incoming stock is on its way.
     */
    public void publishTransferEvent(Long productId, Long fromWarehouseId, Long toWarehouseId,
                                     Integer qty, String reason, String performedBy) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("productId",       productId);
            event.put("fromWarehouseId", fromWarehouseId);
            event.put("toWarehouseId",   toWarehouseId);
            event.put("quantity",        qty);
            event.put("reason",          reason);
            event.put("performedBy",     performedBy);
            event.put("timestamp",       java.time.LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    RabbitMQConfig.STOCK_TRANSFER_ROUTING_KEY,
                    event
            );
            log.info("[RabbitMQ] Transfer event published: Product={}, From={}, To={}, Qty={}",
                    productId, fromWarehouseId, toWarehouseId, qty);
        } catch (Exception e) {
            log.warn("[RabbitMQ] Failed to publish transfer event: {}", e.getMessage());
        }
    }

    public void publishTransferRequestEvent(Long requestId, Long productId, Long requestingWarehouseId,
                                            Integer qty, String reason, String requestedBy,
                                            java.util.List<Long> candidateWarehouseIds) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("requestId", requestId);
            event.put("productId", productId);
            event.put("requestingWarehouseId", requestingWarehouseId);
            event.put("quantity", qty);
            event.put("reason", reason);
            event.put("requestedBy", requestedBy);
            event.put("candidateWarehouseIds", candidateWarehouseIds);
            event.put("timestamp", java.time.LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    "stock.transfer.request",
                    event
            );
            log.info("[RabbitMQ] Transfer request published: Request={}, Product={}, To={}, Qty={}, Candidates={}",
                    requestId, productId, requestingWarehouseId, qty, candidateWarehouseIds.size());
        } catch (Exception e) {
            log.warn("[RabbitMQ] Failed to publish transfer request event: {}", e.getMessage());
        }
    }

    public void publishTransferRequestCompleted(Long requestId) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("requestId", requestId);
            event.put("timestamp", java.time.LocalDateTime.now().toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    "stock.transfer.request.completed",
                    event
            );
            log.info("[RabbitMQ] Transfer request completion published: Request={}", requestId);
        } catch (Exception e) {
            log.warn("[RabbitMQ] Failed to publish transfer request completion: {}", e.getMessage());
        }
    }

    /**
     * Publishes a stock alert event (LOW_STOCK or OVERSTOCK) to the alert-service.
     * Called after every stock change when thresholds are crossed.
     */
    public void publishStockAlert(Long productId, Long warehouseId,
                                  Integer currentQty, String routingKey) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("productId", productId);
            event.put("warehouseId", warehouseId);
            event.put("currentQty", currentQty);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    routingKey,
                    event
            );

            log.info("[RabbitMQ] Stock alert published: Product={}, Qty={}, Key={}",
                    productId, currentQty, routingKey);

        } catch (Exception e) {
            log.warn("[RabbitMQ] Failed to publish stock alert for Product {}: {}", productId, e.getMessage());
        }
    }
}
