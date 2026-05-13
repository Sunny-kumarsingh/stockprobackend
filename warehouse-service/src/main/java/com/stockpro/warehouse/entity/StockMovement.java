package com.stockpro.warehouse.entity;

import jakarta.persistence.*;

import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long movementId;

    private Long warehouseId;
    private Long productId;

    private Integer quantity; //  Amount changed (+ or -)

    private Double unitCost;        // PDF §2.6: Cost per unit at time of movement
    private Integer balanceAfter;   // PDF §2.6: Stock balance after this movement
    private String referenceNumber; // PO number / Sales Order / Issue Order
    
    @Enumerated(EnumType.STRING)
    private MovementType type; // IN, OUT, TRANSFER, ADJUSTMENT

    private String reason;
    private String performedBy; // Username from JWT

    private LocalDateTime timestamp = LocalDateTime.now();
}
