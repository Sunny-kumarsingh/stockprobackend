package com.stockpro.analytics.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PurchaseOrderDTO {
    private Long id;
    private String referenceNumber;
    private Long supplierId;
    private Long warehouseId;
    private String status;
    private Double totalAmount;
    private LocalDateTime orderDate;
    private LocalDateTime receivedDate;
}
