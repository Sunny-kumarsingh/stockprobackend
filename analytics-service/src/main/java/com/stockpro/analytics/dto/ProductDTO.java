package com.stockpro.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProductDTO {
    @JsonProperty("productId")
    private Long id;
    private String sku;
    private String name;
    private String category;
    private String brand;
    private Double costPrice;    // Required for Valuation
    private Double sellingPrice;
    private Integer reorderLevel;
    private String barcode;
}
