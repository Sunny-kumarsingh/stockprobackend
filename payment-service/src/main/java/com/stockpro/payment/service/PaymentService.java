package com.stockpro.payment.service;

import com.stockpro.payment.dto.PaymentRequestDTO;
import com.stockpro.payment.dto.RazorpayOrderRequest;
import com.stockpro.payment.dto.RazorpayVerifyRequest;
import com.stockpro.payment.entity.Invoice;
import com.stockpro.payment.entity.Payment;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    Payment createPayment(PaymentRequestDTO request, String requestedBy);

    // ── Razorpay ──────────────────────────────────
    Map<String, Object> createRazorpayOrder(RazorpayOrderRequest request, String requestedBy);

    Payment verifyAndCompletePayment(RazorpayVerifyRequest verifyRequest);
    // ─────────────────────────────────────────────

    Payment processPayment(Long id, String processedBy);

    Payment failPayment(Long id, String reason);

    Payment cancelPayment(Long id, String reason);

    Payment getById(Long id);

    List<Payment> getByPurchaseOrder(Long purchaseOrderId);

    List<Payment> getAllPayments();

    Invoice getInvoiceByPayment(Long paymentId);
}
