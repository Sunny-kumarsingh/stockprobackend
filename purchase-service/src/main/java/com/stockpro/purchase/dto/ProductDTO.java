package com.stockpro.purchase.dto;

import lombok.Data;

@Data
public class ProductDTO {
    private Long productId;
    private String sku;
    private String name;
}
