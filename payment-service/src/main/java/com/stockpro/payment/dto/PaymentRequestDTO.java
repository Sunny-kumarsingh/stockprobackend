package com.stockpro.payment.dto;

import lombok.Data;

@Data
public class PaymentRequestDTO {
    private Long purchaseOrderId;
    private Long warehouseId;
    private Long supplierId;
    private Double amount;
    private String paymentMethod;   // BANK_TRANSFER / CHEQUE / UPI
    private String remarks;
}
