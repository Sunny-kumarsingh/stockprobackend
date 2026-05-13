package com.stockpro.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long purchaseOrderId;   // Links to purchase-service PO

    private Long warehouseId;       // Related warehouse for manager scoping

    private Long supplierId;        // Who is being paid

    private Double amount;          // Payment amount in INR

    private String paymentMethod;   // BANK_TRANSFER / CHEQUE / UPI

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;   // PENDING / COMPLETED / FAILED / CANCELLED

    @Column(unique = true)
    private String referenceNumber; // Auto-generated PAY-{timestamp}

    // ── Razorpay fields ──────────────────────────
    private String razorpayOrderId;     // Created by backend
    private String razorpayPaymentId;   // Returned by Razorpay after payment
    private String razorpaySignature;   // For HMAC verification
    // ─────────────────────────────────────────────

    private String remarks;         // Optional notes from Officer

    private String requestedBy;     // JWT username of Officer who created it

    private String approvedBy;      // JWT username of Manager who processed it

    private String rejectionReason; // Reason if CANCELLED or FAILED

    private LocalDateTime paidAt;   // Set when status = COMPLETED

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.referenceNumber == null) {
            this.referenceNumber = "PAY-" + System.currentTimeMillis();
        }
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }
}
