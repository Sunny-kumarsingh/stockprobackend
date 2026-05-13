package com.stockpro.purchase.scheduler;

import com.stockpro.purchase.entity.POStatus;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderScheduler {

    private static final String ALERT_EXCHANGE  = "stockpro.exchange";
    private static final String ALERT_ROUTING   = "po.submitted";

    private final PurchaseOrderRepository poRepo;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Runs every hour to check for overdue Purchase Orders.
     * Logic: If PO is APPROVED or PARTIALLY_RECEIVED but expected delivery date is past.
     */
    @Scheduled(cron = "0 0 * * * *") // Runs at the start of every hour
    @Transactional
    @CacheEvict(value = "purchaseOrders", allEntries = true)
    public void checkOverduePurchaseOrders() {
        log.info("Starting Overdue Purchase Order Check...");

        LocalDateTime now = LocalDateTime.now();
        List<POStatus> trackingStatuses = List.of(POStatus.APPROVED, POStatus.PARTIALLY_RECEIVED);

        List<PurchaseOrder> overdueOrders = poRepo.findByStatusInAndExpectedDeliveryDateBefore(trackingStatuses, now);

        if (overdueOrders.isEmpty()) {
            log.info("No overdue orders found.");
            return;
        }

        for (PurchaseOrder po : overdueOrders) {
            log.warn("Marking PO as OVERDUE: {} (Expected: {})", po.getReferenceNumber(), po.getExpectedDeliveryDate());
            po.setStatus(POStatus.OVERDUE);
            poRepo.save(po);

            // Notify alert-service via RabbitMQ
            String alertMessage = "PO " + po.getReferenceNumber() + " is OVERDUE. Expected by: " + po.getExpectedDeliveryDate();
            rabbitTemplate.convertAndSend(ALERT_EXCHANGE, ALERT_ROUTING, alertMessage);
            log.info("Overdue alert published for PO: {}", po.getReferenceNumber());
        }

        log.info("Updated {} orders to OVERDUE status.", overdueOrders.size());
    }
}
