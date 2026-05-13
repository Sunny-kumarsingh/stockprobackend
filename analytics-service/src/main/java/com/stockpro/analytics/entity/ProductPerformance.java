package com.stockpro.analytics.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ProductPerformance {
    @Id
    private Long productId; // Same ID as Product Service

    private String sku;
    private Double turnoverRate;      // Total movements / days since first movement
    private Integer daysInStock;      // How long the product has been sitting
    private String movementCategory;  // "TOP_MOVING", "ACTIVE", "SLOW_MOVING", "VERY_SLOW", "DEAD"
    private LocalDateTime lastCalculated;
    private LocalDateTime lastMovementDate; // Last time a stock movement was recorded (for 90-day dead stock rule)
}
