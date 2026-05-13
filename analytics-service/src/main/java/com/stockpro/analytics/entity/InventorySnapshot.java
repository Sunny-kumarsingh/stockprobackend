package com.stockpro.analytics.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class InventorySnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long warehouseId;
    private Long productId;
    private Integer quantity;
    private Double unitCost;
    private Double totalValuation; // quantity * unitCost
    private LocalDateTime snapshotDate;
}
