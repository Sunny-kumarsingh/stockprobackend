package com.stockpro.purchase.publisher;

import com.stockpro.purchase.config.RabbitMQConfig;
import com.stockpro.purchase.dto.PurchaseOrderEvent;
import com.stockpro.purchase.entity.PurchaseOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class PurchaseOrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish a PO status change event.
     * Fully wrapped in try-catch — NEVER breaks the main PO flow.
     */
    public void publish(PurchaseOrder po, String routingKey) {
        try {
            PurchaseOrderEvent event = new PurchaseOrderEvent(
                    po.getId(),
                    po.getReferenceNumber(),
                    po.getSupplierId(),
                    po.getWarehouseId(),
                    po.getTotalAmount(),
                    po.getStatus().name(),
                    LocalDateTime.now()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE,
                    routingKey,
                    event
            );

            log.info("[RabbitMQ] PO event published: ref={}, status={}, routing={}",
                    po.getReferenceNumber(), po.getStatus(), routingKey);

        } catch (Exception e) {
            // NON-CRITICAL: Log but never propagate — main PO flow must continue
            log.warn("[RabbitMQ] Failed to publish PO event for ref={}: {}",
                    po.getReferenceNumber(), e.getMessage());
        }
    }
}
