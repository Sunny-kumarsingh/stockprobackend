package com.stockpro.payment.dto;

import lombok.Data;

@Data
public class RazorpayOrderRequest {
    private Long existingPaymentId; // If set, update this record instead of creating a new one
    private Long purchaseOrderId;
    private Long warehouseId;
    private Long supplierId;
    private Double amount;          // In INR (will be converted to paise)
    private String paymentMethod;   // For record only — BANK_TRANSFER/CHEQUE/UPI
    private String remarks;
}
