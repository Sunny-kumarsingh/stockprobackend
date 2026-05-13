package com.stockpro.warehouse.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long warehouseId;

    @NotBlank(message = "Warehouse name is required")
    @Size(min = 2, max = 100, message = "Warehouse name must be between 2 and 100 characters")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Location is required")
    private String location;

    private String address;

    private Long managerId;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @Min(value = 0, message = "Used capacity cannot be negative")
    private Integer usedCapacity;

    private Boolean isActive = true;

    @Pattern(
        regexp = "^(\\+91)?[0-9]{10}$",
        message = "Phone number must be 10 digits, optionally prefixed with +91"
    )
    private String phone;

    private LocalDateTime createdAt = LocalDateTime.now();
}