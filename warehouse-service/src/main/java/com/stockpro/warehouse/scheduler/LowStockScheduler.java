package com.stockpro.warehouse.scheduler;

import com.stockpro.warehouse.config.RabbitMQConfig;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.publisher.StockEventPublisher;
import com.stockpro.warehouse.repository.StockLevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodic low-stock & overstock scanner.
 *
 * Runs every 10 minutes and scans ALL stock levels across ALL warehouses.
 * Publishes STOCK_LOW or STOCK_HIGH events to RabbitMQ so alert-service
 * can create/update alerts — even for products that were already low
 * before the service started (fixes the "no alert on startup" problem).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockScheduler {

    private final StockLevelRepository stockRepo;
    private final StockEventPublisher   publisher;

    /** Runs at startup (delay=10 s) then every 10 minutes */
    @Scheduled(initialDelay = 10_000, fixedRate = 600_000)
    public void scanAllStockLevels() {
        log.info("[Scheduler] Running periodic low-stock / overstock scan...");

        List<StockLevel> all = stockRepo.findAll();
        int lowCount  = 0;
        int highCount = 0;

        for (StockLevel stock : all) {
            if (stock.getQuantity() == null) continue;

            int qty          = stock.getQuantity();
            int minThreshold = (stock.getMinThreshold()   != null) ? stock.getMinThreshold()   : 25;
            int maxLevel     = (stock.getMaxStockLevel()  != null) ? stock.getMaxStockLevel()  : 1000;

            if (qty <= minThreshold && minThreshold > 0) {
                // LOW STOCK — publish event → alert-service creates CRITICAL alert
                publisher.publishStockAlert(
                        stock.getProductId(),
                        stock.getWarehouseId(),
                        qty,
                        RabbitMQConfig.STOCK_LOW_ROUTING_KEY
                );
                log.warn("[Scheduler] LOW STOCK detected: Product={}, Warehouse={}, Qty={}, Min={}",
                        stock.getProductId(), stock.getWarehouseId(), qty, minThreshold);
                lowCount++;

            } else if (qty > maxLevel) {
                // OVERSTOCK — publish event → alert-service creates WARNING alert
                publisher.publishStockAlert(
                        stock.getProductId(),
                        stock.getWarehouseId(),
                        qty,
                        RabbitMQConfig.STOCK_HIGH_ROUTING_KEY
                );
                log.warn("[Scheduler] OVERSTOCK detected: Product={}, Warehouse={}, Qty={}, Max={}",
                        stock.getProductId(), stock.getWarehouseId(), qty, maxLevel);
                highCount++;
            }
        }

        log.info("[Scheduler] Scan complete — {} low-stock, {} overstock items found.", lowCount, highCount);
    }
}
