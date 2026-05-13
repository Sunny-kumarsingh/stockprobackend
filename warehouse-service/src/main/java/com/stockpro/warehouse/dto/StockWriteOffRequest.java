package com.stockpro.warehouse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockWriteOffRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Write-off reason is required")
    // e.g. "DAMAGED", "EXPIRED", "LOST"
    private String writeOffReason;

    private String notes; // Optional: e.g. "Batch B-22 expired on 2026-01-01"
}
