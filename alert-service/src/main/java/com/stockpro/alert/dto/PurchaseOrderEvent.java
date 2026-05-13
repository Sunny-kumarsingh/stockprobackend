package com.stockpro.alert.dto;

import lombok.Data;

@Data
public class PurchaseOrderEvent {
    private Long poId;
    private Long warehouseId;
    private Long supplierId;
    private String referenceNumber;
    private Double totalAmount;
    private String status;
}