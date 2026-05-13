package com.stockpro.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false, unique = true)
    private Long paymentId;

    private Long purchaseOrderId;

    private Long warehouseId;

    private Long supplierId;

    private Double amount;

    private String paymentReferenceNumber;

    private String paymentMethod;

    private String razorpayPaymentId;

    private String status;

    private LocalDateTime paidAt;

    private LocalDateTime generatedAt;

    @PrePersist
    public void prePersist() {
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
        if (this.invoiceNumber == null) {
            this.invoiceNumber = "INV-" + System.currentTimeMillis();
        }
        if (this.status == null) {
            this.status = "PAID";
        }
    }
}
