package com.stockpro.payment.dto;

import lombok.Data;

@Data
public class RazorpayVerifyRequest {
    private Long paymentRecordId;       // Our DB payment ID
    private String razorpayOrderId;     // From Razorpay
    private String razorpayPaymentId;   // From Razorpay after success
    private String razorpaySignature;   // HMAC signature to verify
}
