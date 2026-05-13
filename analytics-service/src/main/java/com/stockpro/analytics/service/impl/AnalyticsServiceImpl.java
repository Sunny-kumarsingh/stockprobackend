package com.stockpro.analytics.service.impl;

import com.stockpro.analytics.client.ProductClient;
import com.stockpro.analytics.client.PurchaseClient;
import com.stockpro.analytics.client.WarehouseClient;
import com.stockpro.analytics.dto.*;
import com.stockpro.analytics.entity.InventorySnapshot;
import com.stockpro.analytics.entity.ProductPerformance;
import com.stockpro.analytics.repository.ProductPerformanceRepository;
import com.stockpro.analytics.repository.SnapshotRepository;
import com.stockpro.analytics.repository.SupplierSpendRepository;
import com.stockpro.analytics.service.AnalyticsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final String CONTENT_TYPE_CSV = "text/csv";
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String COL_PRODUCT_ID = "Product ID";
    private static final String COL_SKU = "SKU";
    private static final String COL_QUANTITY = "Quantity";
    private static final String COL_COST_PRICE = "Cost Price";
    private static final String COL_TOTAL_VALUE = "Total Value";
    private static final String COL_TURNOVER = "Turnover Rate";
    private static final String COL_CATEGORY = "Category";
    private static final String COL_LAST_UPDATED = "Last Updated";

    private final ProductClient productClient;
    private final PurchaseClient purchaseClient;
    private final WarehouseClient warehouseClient;
    private final ProductPerformanceRepository performanceRepository;
    private final SnapshotRepository snapshotRepository;
    private final SupplierSpendRepository supplierSpendRepository;

    // ─────────────────────────────────────────────────────────────
    // 1. GLOBAL INVENTORY VALUATION
    // ─────────────────────────────────────────────────────────────
    @Override
    @Cacheable(value = "analyticsValuation", key = "@analyticsCacheKey.scope()")
    public Double calculateGlobalValuation() {
        try {
            List<ProductDTO> products = productClient.getAllProducts();
            if (products == null) return 0.0;

            Map<Long, Double> productCosts = products.stream()
                    .filter(p -> p.getId() != null)
                    .collect(Collectors.toMap(
                            ProductDTO::getId,
                            p -> p.getCostPrice() != null ? p.getCostPrice() : 0.0,
                            (v1, v2) -> v1 // Handle duplicate IDs if any
                    ));

            List<WarehouseDTO> warehouses = getVisibleWarehouses();
            if (warehouses == null) return 0.0;

            return warehouses.stream()
                    .flatMap(w -> {
                        try {
                            List<StockLevelDTO> inv = warehouseClient.getWarehouseInventory(w.getId());
                            return inv != null ? inv.stream() : Stream.<StockLevelDTO>empty();
                        } catch (Exception e) {
                            return Stream.empty();
                        }
                    })
                    .mapToDouble(stock -> {
                        int qty = stock.getQuantity() != null ? stock.getQuantity() : 0;
                        double cost = productCosts.getOrDefault(stock.getProductId(), 0.0);
                        return qty * cost;
                    })
                    .sum();

        } catch (Exception e) {
            log.error("[Analytics] Valuation failed: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. TOP MOVING PRODUCTS
    // ─────────────────────────────────────────────────────────────
    @Override
    @Cacheable(value = "analyticsTopMoving", key = "@analyticsCacheKey.scope() + ':' + #p0")
    public List<ProductPerformanceDTO> getTopMovingProducts(int limit) {
        try {
            if (isWarehouseScopedUser()) {
                return buildScopedProductPerformance().stream()
                        .sorted(Comparator.comparing(ProductPerformanceDTO::getTurnoverRate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(limit)
                        .toList();
            }

            return performanceRepository.findTop10ByOrderByTurnoverRateDesc()
                    .stream().limit(limit).map(this::toDTO).toList();
        } catch (Exception e) {
            log.error("[Analytics] Failed to fetch top moving products: {}", e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. SLOW-MOVING PRODUCTS
    // ─────────────────────────────────────────────────────────────
    @Override
    @Cacheable(value = "analyticsSlowMoving", key = "@analyticsCacheKey.scope()")
    public List<ProductPerformanceDTO> getSlowMovingProducts() {
        try {
            List<String> slowCategories = List.of("SLOW_MOVING", "VERY_SLOW");

            if (isWarehouseScopedUser()) {
                // For warehouse-scoped users, filter scoped performance by slow categories
                return buildScopedProductPerformance().stream()
                        .filter(p -> slowCategories.contains(p.getMovementCategory()))
                        .sorted(Comparator.comparing(ProductPerformanceDTO::getTurnoverRate,
                                Comparator.nullsLast(Comparator.naturalOrder())))  // lowest first
                        .toList();
            }

            return performanceRepository
                    .findByMovementCategoryInOrderByTurnoverRateAsc(slowCategories)
                    .stream().map(this::toDTO).toList();

        } catch (Exception e) {
            log.error("[Analytics] Failed to fetch slow-moving products: {}", e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────
    @Override
    @Cacheable(value = "analyticsDeadStock", key = "@analyticsCacheKey.scope()")
    public List<ProductPerformanceDTO> getDeadStock() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(90);

            if (isWarehouseScopedUser()) {
                return buildScopedDeadStock(cutoff);
            }

            return performanceRepository.findDeadStockBefore(cutoff)
                    .stream().map(this::toDTO).toList();
        } catch (Exception e) {
            log.error("[Analytics] Failed to fetch dead stock: {}", e.getMessage());
            return List.of();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. DASHBOARD METRICS (single call)
    // ─────────────────────────────────────────────────────────────
    @Override
    @Cacheable(value = "analyticsDashboard", key = "@analyticsCacheKey.scope()")
    public GlobalMetricsDTO getGlobalDashboardMetrics() {
        GlobalMetricsDTO metrics = new GlobalMetricsDTO();

        // ── 1. Total inventory valuation ──────────────────────────
        try {
            metrics.setTotalInventoryValue(calculateGlobalValuation());
        } catch (Exception e) {
            log.warn("[Analytics] Valuation skipped: {}", e.getMessage());
            metrics.setTotalInventoryValue(0.0);
        }

        // ── 2. Product count (from product-service) ───────────────
        try {
            metrics.setTotalProducts(calculateVisibleProductCount());
        } catch (Exception e) {
            log.warn("[Analytics] Product count skipped: {}", e.getMessage());
            metrics.setTotalProducts(0);
        }

        // ── 3. Warehouse count + utilization ──────────────────────
        try {
            List<WarehouseDTO> warehouses = getVisibleWarehouses();
            metrics.setTotalWarehouses(warehouses != null ? warehouses.size() : 0);

            Map<String, Double> utilizationMap = new HashMap<>();
            int highCapacityCount = 0;

            if (warehouses != null) {
                for (WarehouseDTO w : warehouses) {
                    if (w.getName() == null) continue;
                    int capacity = w.getCapacity() != null ? w.getCapacity() : 0;
                    int used = w.getUsedCapacity() != null ? w.getUsedCapacity() : 0;
                    double usage = (capacity > 0) ? ((double) used / capacity) * 100 : 0.0;
                    double rounded = Math.round(usage * 100.0) / 100.0;
                    utilizationMap.put(w.getName(), rounded);
                    if (rounded > 90.0) highCapacityCount++;
                }
            }
            metrics.setWarehouseUtilization(utilizationMap);
            metrics.setLowStockAlerts(highCapacityCount);
        } catch (Exception e) {
            log.warn("[Analytics] Warehouse data skipped: {}", e.getMessage());
            metrics.setTotalWarehouses(0);
            metrics.setWarehouseUtilization(new HashMap<>());
        }

        // ── 4. Product performance (from local analytics DB) ──────
        try {
            if (isWarehouseScopedUser()) {
                List<ProductPerformanceDTO> scopedPerf = buildScopedProductPerformance();
                metrics.setTopMovingCount(scopedPerf.stream().filter(p -> "TOP_MOVING".equals(p.getMovementCategory())).count());
                metrics.setSlowMovingCount(scopedPerf.stream().filter(p -> "SLOW_MOVING".equals(p.getMovementCategory())
                        || "VERY_SLOW".equals(p.getMovementCategory())).count());
                metrics.setDeadStockCount((long) getDeadStock().size());
            } else {
                List<ProductPerformance> allPerf = performanceRepository.findAll();
                metrics.setTopMovingCount(allPerf.stream().filter(p -> "TOP_MOVING".equals(p.getMovementCategory())).count());
                metrics.setSlowMovingCount(allPerf.stream().filter(p -> "SLOW_MOVING".equals(p.getMovementCategory())).count());
                metrics.setDeadStockCount(allPerf.stream().filter(p -> "DEAD".equals(p.getMovementCategory())).count());
            }
            metrics.setTopMovingProducts(getTopMovingProducts(5));
        } catch (Exception e) {
            log.warn("[Analytics] Performance data skipped: {}", e.getMessage());
        }

        return metrics;
    }

    // ─────────────────────────────────────────────────────────────
    // 5. WAREHOUSE UTILIZATION — Dedicated Endpoint 
    // ─────────────────────────────────────────────────────────────
    @Override
    @Cacheable(value = "analyticsWarehouseUtilization", key = "@analyticsCacheKey.scope()")
    public List<WarehouseUtilizationDTO> getWarehouseUtilization() {
        return getVisibleWarehouses().stream().map(w -> {
            double pct = (w.getCapacity() != null && w.getCapacity() > 0)
                    ? ((double) w.getUsedCapacity() / w.getCapacity()) * 100 : 0.0;
            double rounded = Math.round(pct * 100.0) / 100.0;
            String status = rounded >= 90 ? "CRITICAL" : rounded >= 70 ? "HIGH" : "NORMAL";
            return new WarehouseUtilizationDTO(
                    w.getId(), w.getName(),
                    w.getUsedCapacity() != null ? w.getUsedCapacity() : 0,
                    w.getCapacity() != null ? w.getCapacity() : 0,
                    rounded, status
            );
        }).toList();
    }

    // ─────────────────────────────────────────────────────────────
    // 6. SUPPLIER SPEND ANALYTICS
    // ─────────────────────────────────────────────────────────────
    @Override
    @Cacheable(value = "analyticsSupplierSpend", key = "@analyticsCacheKey.scope()")
    public List<SupplierSpendDTO> getSupplierSpend() {
        if (isWarehouseScopedUser()) {
            return getScopedSupplierSpend();
        }

        return supplierSpendRepository.findTopSuppliersBySpend()
                .stream()
                .map(s -> new SupplierSpendDTO(
                        s.getSupplierId(),
                        s.getSupplierName(),
                        s.getTotalSpend(),
                        s.getTotalOrdersReceived(),
                        s.getAvgOrderValue(),
                        s.getLastPurchaseDate() != null ? s.getLastPurchaseDate().toString() : "N/A"
                ))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    // 7. CSV EXPORTS
    // ─────────────────────────────────────────────────────────────
    @Override
    public void exportValuationCsv(HttpServletResponse response) throws IOException {
        response.setContentType(CONTENT_TYPE_CSV);
        response.setHeader(HEADER_CONTENT_DISPOSITION, "attachment; filename=\"inventory_valuation.csv\"");

        try (PrintWriter writer = response.getWriter();
             CSVPrinter csv = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(COL_PRODUCT_ID, COL_SKU, COL_QUANTITY, COL_COST_PRICE, COL_TOTAL_VALUE)
                             .build())) {

            Map<Long, Double> costs = productClient.getAllProducts()
                    .stream().collect(Collectors.toMap(ProductDTO::getId, ProductDTO::getCostPrice));
            Map<Long, String> skus = productClient.getAllProducts()
                    .stream().collect(Collectors.toMap(ProductDTO::getId, p -> p.getSku() != null ? p.getSku() : ""));

            getVisibleWarehouses().stream()
                    .flatMap(w -> warehouseClient.getWarehouseInventory(w.getId()).stream())
                    .forEach(stock -> {
                        try {
                            double cost = costs.getOrDefault(stock.getProductId(), 0.0);
                            double value = stock.getQuantity() * cost;
                            csv.printRecord(
                                    stock.getProductId(),
                                    skus.getOrDefault(stock.getProductId(), ""),
                                    stock.getQuantity(),
                                    cost,
                                    Math.round(value * 100.0) / 100.0
                            );
                        } catch (IOException e) {
                            log.error("CSV write error: {}", e.getMessage());
                        }
                    });
        }
    }

    @Override
    public void exportDeadStockCsv(HttpServletResponse response) throws IOException {
        response.setContentType(CONTENT_TYPE_CSV);
        response.setHeader(HEADER_CONTENT_DISPOSITION, "attachment; filename=\"dead_stock_report.csv\"");

        try (PrintWriter writer = response.getWriter();
             CSVPrinter csv = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(COL_PRODUCT_ID, COL_SKU, COL_TURNOVER, COL_CATEGORY, COL_LAST_UPDATED)
                             .build())) {

            for (ProductPerformanceDTO p : getDeadStock()) {
                csv.printRecord(
                        p.getProductId(),
                        p.getSku() != null ? p.getSku() : "",
                        p.getTurnoverRate(),
                        p.getMovementCategory(),
                        p.getLastCalculated()
                );
            }
        }
    }

    @Override
    public void exportTopMovingCsv(HttpServletResponse response) throws IOException {
        response.setContentType(CONTENT_TYPE_CSV);
        response.setHeader(HEADER_CONTENT_DISPOSITION, "attachment; filename=\"top_moving_products.csv\"");

        try (PrintWriter writer = response.getWriter();
             CSVPrinter csv = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(COL_PRODUCT_ID, COL_SKU, COL_TURNOVER, COL_CATEGORY, COL_LAST_UPDATED)
                             .build())) {

            for (ProductPerformanceDTO p : getTopMovingProducts(10)) {
                csv.printRecord(
                        p.getProductId(),
                        p.getSku() != null ? p.getSku() : "",
                        p.getTurnoverRate(),
                        p.getMovementCategory(),
                        p.getLastCalculated()
                );
            }
        }
    }

    @Override
    public void exportSlowMovingCsv(HttpServletResponse response) throws IOException {
        response.setContentType(CONTENT_TYPE_CSV);
        response.setHeader(HEADER_CONTENT_DISPOSITION, "attachment; filename=\"slow_moving_products.csv\"");

        try (PrintWriter writer = response.getWriter();
             CSVPrinter csv = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(COL_PRODUCT_ID, COL_SKU, COL_TURNOVER, COL_CATEGORY, COL_LAST_UPDATED)
                             .build())) {

            for (ProductPerformanceDTO p : getSlowMovingProducts()) {
                csv.printRecord(
                        p.getProductId(),
                        p.getSku() != null ? p.getSku() : "",
                        p.getTurnoverRate(),
                        p.getMovementCategory(),
                        p.getLastCalculated()
                );
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────
    private List<WarehouseDTO> getVisibleWarehouses() {
        List<WarehouseDTO> warehouses = warehouseClient.getAllWarehouses();
        if (warehouses == null || !isWarehouseScopedUser()) {
            return warehouses != null ? warehouses : List.of();
        }

        String department = getCurrentDepartment();
        if (department == null || department.isBlank()) {
            return List.of();
        }

        return warehouses.stream()
                .filter(w -> department.equals(w.getName()))
                .toList();
    }

    private boolean isWarehouseScopedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }

        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(role -> "ROLE_MANAGER".equals(role) || "ROLE_STAFF".equals(role));
    }

    private String getCurrentDepartment() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getDetails() instanceof String ? (String) auth.getDetails() : null;
    }

    private List<ProductPerformanceDTO> buildScopedProductPerformance() {
        List<WarehouseDTO> warehouses = getVisibleWarehouses();
        if (warehouses.isEmpty()) {
            return List.of();
        }

        Map<Long, String> productSkus = getProductSkus();
        Map<Long, Long> movementCounts = warehouses.stream()
                .flatMap(w -> snapshotRepository.findByWarehouseId(w.getId()).stream())
                .filter(snapshot -> snapshot.getProductId() != null)
                .collect(Collectors.groupingBy(InventorySnapshot::getProductId, Collectors.counting()));

        return movementCounts.entrySet().stream()
                .map(entry -> new ProductPerformanceDTO(
                        entry.getKey(),
                        productSkus.getOrDefault(entry.getKey(), ""),
                        entry.getValue().doubleValue(),
                        classifyMovement(entry.getValue()),
                        LocalDateTime.now()
                ))
                .toList();
    }

    private List<ProductPerformanceDTO> buildScopedDeadStock(LocalDateTime cutoff) {
        List<WarehouseDTO> warehouses = getVisibleWarehouses();
        if (warehouses.isEmpty()) {
            return List.of();
        }

        Map<Long, String> productSkus = getProductSkus();

        // Get all productIds visible in this warehouse's stock
        // Then cross-check against performance: no movement since cutoff
        return warehouses.stream()
                .flatMap(w -> warehouseClient.getWarehouseInventory(w.getId()).stream())
                .map(StockLevelDTO::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .filter(productId -> performanceRepository.findById(productId)
                        .map(p -> p.getLastMovementDate() == null || p.getLastMovementDate().isBefore(cutoff))
                        .orElse(true))  // never tracked = dead
                .map(productId -> new ProductPerformanceDTO(
                        productId,
                        productSkus.getOrDefault(productId, ""),
                        0.0,
                        "DEAD",
                        LocalDateTime.now()
                ))
                .toList();
    }

    private List<SupplierSpendDTO> getScopedSupplierSpend() {
        try {
            return purchaseClient.getPurchaseOrders().stream()
                    .filter(po -> "RECEIVED".equals(po.getStatus()))
                    .filter(po -> po.getSupplierId() != null)
                    .collect(Collectors.groupingBy(PurchaseOrderDTO::getSupplierId))
                    .entrySet().stream()
                    .map(entry -> {
                        double total = entry.getValue().stream()
                                .map(PurchaseOrderDTO::getTotalAmount)
                                .filter(Objects::nonNull)
                                .mapToDouble(Double::doubleValue)
                                .sum();
                        int orders = entry.getValue().size();
                        String lastPurchaseDate = entry.getValue().stream()
                                .map(po -> po.getReceivedDate() != null ? po.getReceivedDate() : po.getOrderDate())
                                .filter(Objects::nonNull)
                                .max(LocalDateTime::compareTo)
                                .map(LocalDateTime::toString)
                                .orElse("N/A");

                        return new SupplierSpendDTO(
                                entry.getKey(),
                                "Supplier #" + entry.getKey(),
                                total,
                                orders,
                                orders > 0 ? total / orders : 0.0,
                                lastPurchaseDate
                        );
                    })
                    .sorted(Comparator.comparing(SupplierSpendDTO::getTotalSpend,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } catch (Exception e) {
            log.warn("[Analytics] Scoped supplier spend skipped: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<Long, String> getProductSkus() {
        try {
            return productClient.getAllProducts().stream()
                    .filter(product -> product.getId() != null)
                    .collect(Collectors.toMap(
                            ProductDTO::getId,
                            product -> product.getSku() != null ? product.getSku() : "",
                            (first, second) -> first
                    ));
        } catch (Exception e) {
            log.warn("[Analytics] Product SKU lookup skipped: {}", e.getMessage());
            return Map.of();
        }
    }

    private String classifyMovement(long movementCount) {
        if (movementCount >= 50) return "TOP_MOVING";
        if (movementCount >= 20) return "ACTIVE";
        if (movementCount >= 5) return "SLOW_MOVING";
        if (movementCount >= 1) return "VERY_SLOW";
        return "DEAD";
    }

    private int calculateVisibleProductCount() {
        if (isWarehouseScopedUser()) {
            return (int) getVisibleWarehouses().stream()
                    .flatMap(w -> warehouseClient.getWarehouseInventory(w.getId()).stream())
                    .map(StockLevelDTO::getProductId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
        }

        List<ProductDTO> products = productClient.getAllProducts();
        if (products == null) {
            return 0;
        }

        return (int) products.stream()
                .map(ProductDTO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private ProductPerformanceDTO toDTO(ProductPerformance p) {
        return new ProductPerformanceDTO(
                p.getProductId(), p.getSku(),
                p.getTurnoverRate(), p.getMovementCategory(), p.getLastCalculated()
        );
    }
}
