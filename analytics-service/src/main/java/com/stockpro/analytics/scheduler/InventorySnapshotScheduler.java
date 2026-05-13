package com.stockpro.analytics.scheduler;

import com.stockpro.analytics.client.ProductClient;
import com.stockpro.analytics.client.WarehouseClient;
import com.stockpro.analytics.dto.ProductDTO;
import com.stockpro.analytics.dto.StockLevelDTO;
import com.stockpro.analytics.dto.WarehouseDTO;
import com.stockpro.analytics.entity.InventorySnapshot;
import com.stockpro.analytics.repository.SnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Daily inventory snapshot scheduler — PDF §2.8.
 * Runs every night at midnight (00:00:00).
 * Captures a point-in-time snapshot of every product's stock level
 * across all warehouses, stored in inventory_snapshot for trend reporting.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InventorySnapshotScheduler {

    private final SnapshotRepository snapshotRepository;
    private final WarehouseClient warehouseClient;
    private final ProductClient productClient;

    /**
     * Runs every day at midnight: cron = "0 0 0 * * *"
     * For testing: use @Scheduled(fixedRate = 60_000) to run every minute.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @CacheEvict(value = {
            "analyticsDashboard", "analyticsValuation",
            "analyticsTopMoving", "analyticsDeadStock",
            "analyticsWarehouseUtilization"
    }, allEntries = true)
    public void takeNightlySnapshot() {
        log.info("[SnapshotScheduler] Starting nightly inventory snapshot at {}", LocalDateTime.now());

        try {
            // 1. Get product costs for valuation
            List<ProductDTO> products = productClient.getAllProducts();
            Map<Long, Double> costMap = products == null ? Map.of()
                    : products.stream()
                        .filter(p -> p.getId() != null)
                        .collect(Collectors.toMap(
                                ProductDTO::getId,
                                p -> p.getCostPrice() != null ? p.getCostPrice() : 0.0,
                                (a, b) -> a
                        ));

            // 2. Get all warehouses
            List<WarehouseDTO> warehouses = warehouseClient.getAllWarehouses();
            if (warehouses == null || warehouses.isEmpty()) {
                log.warn("[SnapshotScheduler] No warehouses found — snapshot skipped.");
                return;
            }

            int totalSaved = 0;
            LocalDateTime snapshotTime = LocalDateTime.now();

            // 3. For each warehouse, fetch inventory and save a snapshot per product
            for (WarehouseDTO warehouse : warehouses) {
                try {
                    List<StockLevelDTO> inventory = warehouseClient.getWarehouseInventory(warehouse.getId());
                    if (inventory == null || inventory.isEmpty()) continue;

                    for (StockLevelDTO stock : inventory) {
                        if (stock.getProductId() == null) continue;

                        double unitCost = costMap.getOrDefault(stock.getProductId(), 0.0);
                        int qty = stock.getQuantity() != null ? stock.getQuantity() : 0;
                        double totalValuation = qty * unitCost;

                        InventorySnapshot snapshot = new InventorySnapshot();
                        snapshot.setWarehouseId(warehouse.getId());
                        snapshot.setProductId(stock.getProductId());
                        snapshot.setQuantity(qty);
                        snapshot.setUnitCost(unitCost);
                        snapshot.setTotalValuation(Math.round(totalValuation * 100.0) / 100.0);
                        snapshot.setSnapshotDate(snapshotTime);

                        snapshotRepository.save(snapshot);
                        totalSaved++;
                    }
                } catch (Exception warehouseEx) {
                    log.warn("[SnapshotScheduler] Failed to snapshot Warehouse {}: {}",
                            warehouse.getId(), warehouseEx.getMessage());
                }
            }

            log.info("[SnapshotScheduler] Nightly snapshot complete — {} records saved.", totalSaved);

        } catch (Exception e) {
            log.error("[SnapshotScheduler] Nightly snapshot FAILED: {}", e.getMessage(), e);
        }
    }
}
