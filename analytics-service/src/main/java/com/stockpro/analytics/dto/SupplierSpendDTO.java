package com.stockpro.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierSpendDTO {
    private Long supplierId;
    private String supplierName;
    private Double totalSpend;
    private Integer totalOrdersReceived;
    private Double avgOrderValue;
    private String lastPurchaseDate;
}
