package com.stockpro.productservice.mapper;

import com.stockpro.productservice.dto.*;
import com.stockpro.productservice.entity.Product;

public class ProductMapper {

    private ProductMapper() {
        
    }

    public static Product toEntity(ProductRequestDTO dto) {
        Product p = new Product();
        p.setSku(dto.getSku());
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setCategory(dto.getCategory());
        p.setBrand(dto.getBrand());
        p.setUnitOfMeasure(dto.getUnitOfMeasure());
        p.setCostPrice(dto.getCostPrice());
        p.setSellingPrice(dto.getSellingPrice());
        p.setReorderLevel(dto.getReorderLevel());
        p.setMaxStockLevel(dto.getMaxStockLevel());
        p.setLeadTimeDays(dto.getLeadTimeDays());
        p.setBarcode(dto.getBarcode());
        p.setImageUrl(dto.getImageUrl());
        return p;
    }

    public static ProductResponseDTO toDTO(Product p) {
        return ProductResponseDTO.builder()
                .productId(p.getProductId())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())  
                .category(p.getCategory())
                .brand(p.getBrand())
                .unitOfMeasure(p.getUnitOfMeasure())
                .sellingPrice(p.getSellingPrice())
                .costPrice(p.getCostPrice())
                .reorderLevel(p.getReorderLevel())
                .maxStockLevel(p.getMaxStockLevel())
                .leadTimeDays(p.getLeadTimeDays())
                .barcode(p.getBarcode())
                .imageUrl(p.getImageUrl())          //  Fixed
                .isActive(p.getIsActive())
                .totalStock(p.getTotalStock())
                .build();
    }
}