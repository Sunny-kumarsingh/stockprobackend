package com.stockpro.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseUtilizationDTO {
    private Long warehouseId;
    private String warehouseName;
    private Integer usedCapacity;
    private Integer totalCapacity;
    private Double utilizationPercent;  // 0.0 to 100.0
    private String status;              // "NORMAL" | "HIGH" | "CRITICAL"
}
