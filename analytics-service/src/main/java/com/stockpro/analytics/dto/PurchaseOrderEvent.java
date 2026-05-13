package com.stockpro.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderEvent {
    private Long poId;
    private String referenceNumber;
    private Long supplierId;
    private Long warehouseId;
    private Double totalAmount;
    private String status; // SUBMITTED, APPROVED, RECEIVED
    private LocalDateTime timestamp;
}
