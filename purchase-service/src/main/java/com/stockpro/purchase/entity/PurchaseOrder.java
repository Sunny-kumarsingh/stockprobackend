package com.stockpro.purchase.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceNumber;

    private Long supplierId;

    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    private POStatus status;

    private Double totalAmount;

    private LocalDateTime orderDate;

    private LocalDateTime expectedDeliveryDate;

    private LocalDateTime receivedDate;

    private String notes;

    private Long createdById;

    private String createdBy;

    private String cancelReason;

    // 🔥 Relationship with Line Items
    @OneToMany(mappedBy = "purchaseOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonAlias("lineItems")
    private List<POLineItem> items;
    
    
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .mapToDouble(POLineItem::getTotalCost)
                .sum();
    }
}
