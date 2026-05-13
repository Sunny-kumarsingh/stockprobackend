package com.stockpro.analytics.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class GlobalMetricsDTO {

    // Valuation
    private Double totalInventoryValue;

    // Warehouse Overview
    private Integer totalWarehouses;
    private Map<String, Double> warehouseUtilization; // "Warehouse A" -> 85.5%
    private Integer lowStockAlerts;                   // warehouses above 90% capacity

    // Product Performance
    private Integer totalProducts;
    private Long topMovingCount;
    private Long slowMovingCount;
    private Long deadStockCount;

    // Top Performers (for dashboard table)
    private List<ProductPerformanceDTO> topMovingProducts;
}
