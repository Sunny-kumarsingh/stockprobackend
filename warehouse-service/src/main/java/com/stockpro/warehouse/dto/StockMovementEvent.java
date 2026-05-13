package com.stockpro.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementEvent {

    private Long productId;
    private Long warehouseId;
    private Integer quantity;
    private String movementType; // IN, OUT, TRANSFER
    private String reason;
    private LocalDateTime timestamp;
}
