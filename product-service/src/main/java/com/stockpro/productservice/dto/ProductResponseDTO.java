package com.stockpro.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {

    private Long productId;
    private String sku;
    private String name;
    private String description;   //  Added
    private String category;
    private String brand;
    private String unitOfMeasure;
    private Double sellingPrice;
    private Double costPrice;
    private Integer reorderLevel;
    private Integer maxStockLevel;
    private Integer leadTimeDays;
    private String barcode;
    private String imageUrl;      //  Added — required for product images in the UI
    private Boolean isActive;
    private Integer totalStock;
}
