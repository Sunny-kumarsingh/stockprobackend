package com.stockpro.alert.dto;

import lombok.Data;

@Data
public class StockAlertEvent {
    private Long productId;
    private Long warehouseId;
    private Integer currentQty;
}