package com.stockpro.payment.entity;

public enum PaymentStatus {
    PENDING,        // Created, awaiting processing
    COMPLETED,      // Payment successfully processed
    FAILED,         // Payment attempt failed
    CANCELLED       // Manually cancelled
}
