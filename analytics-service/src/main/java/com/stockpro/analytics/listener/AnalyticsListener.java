package com.stockpro.analytics.listener;

import com.stockpro.analytics.config.RabbitMQConfig;
import com.stockpro.analytics.dto.PurchaseOrderEvent;
import com.stockpro.analytics.dto.StockMovementEvent;
import com.stockpro.analytics.entity.InventorySnapshot;
import com.stockpro.analytics.entity.ProductPerformance;
import com.stockpro.analytics.entity.SupplierSpend;
import com.stockpro.analytics.repository.ProductPerformanceRepository;
import com.stockpro.analytics.repository.SnapshotRepository;
import com.stockpro.analytics.repository.SupplierSpendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyticsListener {

    private final SnapshotRepository snapshotRepository;
    private final ProductPerformanceRepository performanceRepository;
    private final SupplierSpendRepository supplierSpendRepository;

    // ─────────────────────────────────────────────
    // 1. Stock Movement Event (from warehouse-service)
    // ─────────────────────────────────────────────
    @RabbitListener(queues = RabbitMQConfig.STOCK_MOVEMENT_QUEUE)
    @CacheEvict(value = {
            "analyticsDashboard", "analyticsValuation", "analyticsTopMoving",
            "analyticsSlowMoving", "analyticsDeadStock", "analyticsWarehouseUtilization"
    }, allEntries = true)
    public void handleStockMovement(StockMovementEvent event) {
        log.info("[Analytics] Stock movement: Product={}, Warehouse={}, Qty={}, Type={}",
                event.getProductId(), event.getWarehouseId(), event.getQuantity(), event.getMovementType());
        try {
            // 1. Save snapshot for historical trend tracking
            InventorySnapshot snapshot = new InventorySnapshot();
            snapshot.setProductId(event.getProductId());
            snapshot.setWarehouseId(event.getWarehouseId());
            snapshot.setQuantity(event.getQuantity());
            snapshot.setUnitCost(0.0);
            snapshot.setTotalValuation(0.0);
            snapshot.setSnapshotDate(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now());
            snapshotRepository.save(snapshot);

            // 2. Update ProductPerformance — recalculate turnover category
            ProductPerformance perf = performanceRepository.findById(event.getProductId())
                    .orElseGet(() -> {
                        ProductPerformance p = new ProductPerformance();
                        p.setProductId(event.getProductId());
                        p.setTurnoverRate(0.0);
                        p.setMovementCategory("NEW");
                        return p;
                    });

            // Count total movements for this product (ISSUE + TRANSFER types = demand events)
            List<InventorySnapshot> allMovements = snapshotRepository.findByProductId(event.getProductId());
            int totalMoved = allMovements.size();

            // Turnover Rate = total movements / days since first movement
            // Higher rate = faster moving product
            if (totalMoved > 1) {
                InventorySnapshot first = allMovements.get(0);
                long daysSinceFirst = java.time.Duration.between(
                        first.getSnapshotDate(), LocalDateTime.now()).toDays();
                double rate = daysSinceFirst > 0
                        ? (double) totalMoved / daysSinceFirst
                        : totalMoved;
                perf.setTurnoverRate(Math.round(rate * 100.0) / 100.0);
            }

            // Classify by total movement count
            if (totalMoved >= 50) {
                perf.setMovementCategory("TOP_MOVING");
            } else if (totalMoved >= 20) {
                perf.setMovementCategory("ACTIVE");
            } else if (totalMoved >= 5) {
                perf.setMovementCategory("SLOW_MOVING");
            } else if (totalMoved >= 1) {
                perf.setMovementCategory("VERY_SLOW");
            } else {
                perf.setMovementCategory("DEAD");
            }

            perf.setLastMovementDate(LocalDateTime.now());
            perf.setLastCalculated(LocalDateTime.now());
            performanceRepository.save(perf);

        } catch (Exception e) {
            log.error("[Analytics] Failed to process StockMovementEvent for Product {}: {}",
                    event.getProductId(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 2. PO Submitted Event
    // ─────────────────────────────────────────────
    @RabbitListener(queues = RabbitMQConfig.PO_SUBMITTED_QUEUE)
    public void handlePoSubmitted(PurchaseOrderEvent event) {
        log.info("[Analytics] PO Submitted: ref={}, Supplier={}, Amount={}",
                event.getReferenceNumber(), event.getSupplierId(), event.getTotalAmount());
    }

    // ─────────────────────────────────────────────
    // 3. PO Approved Event
    // ─────────────────────────────────────────────
    @RabbitListener(queues = RabbitMQConfig.PO_APPROVED_QUEUE)
    public void handlePoApproved(PurchaseOrderEvent event) {
        log.info("[Analytics] PO Approved: ref={}, Amount={}",
                event.getReferenceNumber(), event.getTotalAmount());
    }

    // ─────────────────────────────────────────────
    // 4. PO Received Event → PERSIST supplier spend
    // ─────────────────────────────────────────────
    @RabbitListener(queues = RabbitMQConfig.PO_RECEIVED_QUEUE)
    @CacheEvict(value = {"analyticsDashboard", "analyticsSupplierSpend"}, allEntries = true)
    public void handlePoReceived(PurchaseOrderEvent event) {
        log.info("[Analytics] PO Received: ref={}, Supplier={}, TotalSpend={}",
                event.getReferenceNumber(), event.getSupplierId(), event.getTotalAmount());
        try {
            if (event.getSupplierId() == null || event.getTotalAmount() == null) return;

            SupplierSpend spend = supplierSpendRepository.findById(event.getSupplierId())
                    .orElseGet(() -> {
                        SupplierSpend s = new SupplierSpend();
                        s.setSupplierId(event.getSupplierId());
                        s.setTotalSpend(0.0);
                        s.setTotalOrdersReceived(0);
                        return s;
                    });

            // Accumulate
            spend.setTotalSpend(spend.getTotalSpend() + event.getTotalAmount());
            spend.setTotalOrdersReceived(spend.getTotalOrdersReceived() + 1);
            spend.setAvgOrderValue(spend.getTotalSpend() / spend.getTotalOrdersReceived());
            spend.setLastPurchaseDate(LocalDateTime.now());
            spend.setLastUpdated(LocalDateTime.now());

            supplierSpendRepository.save(spend);
            log.info("[Analytics] Supplier {} spend updated: total={}", event.getSupplierId(), spend.getTotalSpend());

        } catch (Exception e) {
            log.error("[Analytics] Failed to process PO received for supplier {}: {}",
                    event.getSupplierId(), e.getMessage());
        }
    }
}
