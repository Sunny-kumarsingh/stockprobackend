package com.stockpro.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"warehouseId", "productId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockId;

    private Long warehouseId;
    private Long productId;

    private Integer quantity = 0;
    private Integer reservedQuantity = 0;
    
    private Integer minThreshold = 25;
    private Integer maxStockLevel = 1000; // PDF §2.7: Overstock alert threshold

    private String location; // bin / aisle

    private LocalDateTime lastUpdated = LocalDateTime.now();

    //  Derived Field (DO NOT STORE IN DB)
    @Transient
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
    
    public boolean isLowStock() {
        return this.quantity <= this.minThreshold;
    }

}