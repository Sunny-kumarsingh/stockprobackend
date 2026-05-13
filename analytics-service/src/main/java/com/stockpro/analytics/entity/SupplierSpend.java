package com.stockpro.analytics.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_spend")
@Data
public class SupplierSpend {

    @Id
    private Long supplierId;

    private String supplierName;

    private Double totalSpend = 0.0;       // Cumulative from all received POs
    private Integer totalOrdersReceived = 0;
    private Double avgOrderValue = 0.0;    // totalSpend / totalOrdersReceived

    private LocalDateTime lastPurchaseDate;
    private LocalDateTime lastUpdated;
}
