package com.stockpro.alert.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PurchaseOrderDTO {
    private Long id;
    private String referenceNumber;
    private Long supplierId;
    private Long warehouseId;
    private String status;
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime orderDate;
}
