package com.stockpro.purchase.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "po_line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class POLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    @jakarta.persistence.Transient
    private String sku;

    @com.fasterxml.jackson.annotation.JsonAlias("quantity")
    private Integer orderedQuantity;

    private Integer receivedQuantity = 0;

    private Double unitCost;

    private Double totalCost;

    // 🔥 ManyToOne mapping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private PurchaseOrder purchaseOrder;
}