package com.stockpro.alert.scheduler;

import com.stockpro.alert.client.PurchaseClient;
import com.stockpro.alert.dto.PurchaseOrderDTO;
import com.stockpro.alert.entity.AlertSeverity;
import com.stockpro.alert.entity.AlertType;
import com.stockpro.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverduePOScheduler {

    private final AlertService alertService;
    private final PurchaseClient purchaseClient;

    // PDF §2.7 — Runs every 1 hour, checks for APPROVED POs past their expected delivery date
    @Scheduled(fixedRate = 3_600_000)
    public void checkOverduePOs() {
        log.info("[OverduePOScheduler] Running overdue PO check at {}", LocalDateTime.now());

        try {
            List<PurchaseOrderDTO> overduePOs = purchaseClient.getOverduePOs();

            if (overduePOs == null || overduePOs.isEmpty()) {
                log.info("[OverduePOScheduler] No overdue POs found.");
                return;
            }

            log.info("[OverduePOScheduler] Found {} overdue PO(s)", overduePOs.size());

            for (PurchaseOrderDTO po : overduePOs) {
                alertService.createAlert(
                        AlertType.OVERDUE_PO,
                        AlertSeverity.CRITICAL,
                        "Overdue Purchase Order",
                        "PO #" + po.getReferenceNumber() + " was due on "
                                + po.getExpectedDeliveryDate() + " but has not been received.",
                        null,
                        po.getWarehouseId(),
                        po.getId()
                );
            }

            log.info("[OverduePOScheduler] Check complete — {} alerts created.", overduePOs.size());

        } catch (Exception e) {
            // Non-critical: log and continue — never crash the service
            log.warn("[OverduePOScheduler] Could not fetch overdue POs: {}", e.getMessage());
        }
    }
}
