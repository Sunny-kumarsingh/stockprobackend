package com.stockpro.productservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @NotBlank(message = "SKU is required")
    @Size(min = 2, max = 50, message = "SKU must be between 2 and 50 characters")
    @Column(unique = true, nullable = false)
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private String category;
    private String brand;
    private String unitOfMeasure; // kg, piece, liter

    @DecimalMin(value = "0.0", inclusive = false, message = "Cost price must be greater than 0")
    private Double costPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Selling price must be greater than 0")
    private Double sellingPrice;

    @Min(value = 0, message = "Reorder level cannot be negative")
    private Integer reorderLevel;

    @Min(value = 0, message = "Max stock level cannot be negative")
    private Integer maxStockLevel;

    @Min(value = 0, message = "Lead time days cannot be negative")
    private Integer leadTimeDays;

    @Column(length = 1000)
    private String imageUrl;

    private String barcode;

    private Integer totalStock = 0; // Added for RabbitMQ syncing

    private Boolean isActive = true;
}