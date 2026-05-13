package com.stockpro.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPerformanceDTO {
    private Long productId;
    private String sku;
    private Double turnoverRate;
    private String movementCategory; // TOP_MOVING, ACTIVE, SLOW_MOVING, DEAD
    private LocalDateTime lastCalculated;
}
