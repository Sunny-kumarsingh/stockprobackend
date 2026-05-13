package com.stockpro.warehouse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockIssueRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Issue type is required")
    // Must be one of: SALES, PRODUCTION, INTERNAL_USE
    private String issueType;

    private String notes; // Optional: e.g. "Sales Order #1234" or "Production batch B-22"
}
