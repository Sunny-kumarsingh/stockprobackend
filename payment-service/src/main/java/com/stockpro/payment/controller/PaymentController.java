package com.stockpro.payment.controller;

import com.stockpro.payment.dto.PaymentRequestDTO;
import com.stockpro.payment.dto.RazorpayOrderRequest;
import com.stockpro.payment.dto.RazorpayVerifyRequest;
import com.stockpro.payment.entity.Invoice;
import com.stockpro.payment.entity.Payment;
import com.stockpro.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ─────────────────────────────────────────────
    // RAZORPAY: Create order — MANAGER/ADMIN only
    // ─────────────────────────────────────────────
    @PostMapping("/create-order")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> createRazorpayOrder(
            @RequestBody RazorpayOrderRequest request,
            @RequestHeader("X-User-Username") String requestedBy) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(request, requestedBy));
    }

    // ─────────────────────────────────────────────
    // RAZORPAY: Verify payment — MANAGER/ADMIN only
    // ─────────────────────────────────────────────
    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Payment> verifyPayment(@RequestBody RazorpayVerifyRequest verifyRequest) {
        return ResponseEntity.ok(paymentService.verifyAndCompletePayment(verifyRequest));
    }

    // ─────────────────────────────────────────────
    // OFFICER, MANAGER, ADMIN → Create payment
    // ─────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('OFFICER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Payment> createPayment(
            @RequestBody PaymentRequestDTO request,
            @RequestHeader("X-User-Username") String requestedBy) {
        return ResponseEntity.ok(paymentService.createPayment(request, requestedBy));
    }

    // ─────────────────────────────────────────────
    // MANAGER, ADMIN → Process (complete) payment
    // ─────────────────────────────────────────────
    @PutMapping("/{id}/process")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Payment> processPayment(
            @PathVariable Long id,
            @RequestHeader("X-User-Username") String processedBy) {
        return ResponseEntity.ok(paymentService.processPayment(id, processedBy));
    }

    // ─────────────────────────────────────────────
    // MANAGER, ADMIN → Mark as failed
    // ─────────────────────────────────────────────
    @PutMapping("/{id}/fail")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Payment> failPayment(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(paymentService.failPayment(id, reason));
    }

    // ─────────────────────────────────────────────
    // ADMIN only → Cancel payment
    // ─────────────────────────────────────────────
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Payment> cancelPayment(
            @PathVariable Long id,
            @RequestParam String reason) {
        return ResponseEntity.ok(paymentService.cancelPayment(id, reason));
    }

    // ─────────────────────────────────────────────
    // ALL authenticated → Get by ID
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Payment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    // ─────────────────────────────────────────────
    // OFFICER, STAFF, MANAGER, ADMIN → Get all
    // ─────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('OFFICER', 'STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    // ─────────────────────────────────────────────
    // ALL authenticated → Get payments for a PO
    // ─────────────────────────────────────────────
    @GetMapping("/po/{poId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Payment>> getByPurchaseOrder(@PathVariable Long poId) {
        return ResponseEntity.ok(paymentService.getByPurchaseOrder(poId));
    }

    @GetMapping("/{id}/invoice")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Invoice> getInvoiceByPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getInvoiceByPayment(id));
    }
}
