package com.stockpro.warehouse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockReturnRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Return type is required")
    // Must be one of: SUPPLIER_RETURN, CUSTOMER_RETURN
    private String returnType;

    private String referenceNumber; // e.g. PO number or sales order number

    private String notes; // Optional notes
}
