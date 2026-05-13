package com.stockpro.analytics.dto;

import lombok.Data;

@Data
public class StockLevelDTO {
    private Long id;
    private Long warehouseId;
    private Long productId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer minThreshold;
}
