package com.stockpro.payment.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.stockpro.payment.dto.PaymentRequestDTO;
import com.stockpro.payment.dto.RazorpayOrderRequest;
import com.stockpro.payment.dto.RazorpayVerifyRequest;
import com.stockpro.payment.entity.Invoice;
import com.stockpro.payment.entity.Payment;
import com.stockpro.payment.entity.PaymentStatus;
import com.stockpro.payment.exception.PaymentException;
import com.stockpro.payment.exception.ResourceNotFoundException;
import com.stockpro.payment.client.WarehouseClient;
import com.stockpro.payment.repository.InvoiceRepository;
import com.stockpro.payment.repository.PaymentRepository;
import com.stockpro.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String EXCHANGE = "stockpro.payments.exchange";
    private static final String PAYMENT_REF_PREFIX = "Payment ";

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RazorpayClient razorpayClient;
    private final WarehouseClient warehouseClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret; //  Required for HMAC-SHA256 signature verification

    // ─────────────────────────────────────────────
    // CREATE RAZORPAY ORDER (Step 1 of checkout)
    // ─────────────────────────────────────────────
    @Override
    public Map<String, Object> createRazorpayOrder(RazorpayOrderRequest request, String requestedBy) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(request.getAmount() * 100)); // INR → paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "PAY-" + System.currentTimeMillis());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            //  If existingPaymentId is provided, update that record — don't create a new one
            Payment payment;
            if (request.getExistingPaymentId() != null) {
                payment = paymentRepository.findById(request.getExistingPaymentId())
                        .orElseThrow(() -> new PaymentException("Payment record not found: " + request.getExistingPaymentId()));
                assertCurrentUserCanAccessPayment(payment);
                payment.setRazorpayOrderId(razorpayOrderId);
                payment.setRequestedBy(requestedBy);
                // Keep the rest of the fields as-is (amount, supplierId, etc.)
            } else {
                // Fallback: create new payment record (e.g., standalone Razorpay initiation)
                payment = Payment.builder()
                        .purchaseOrderId(request.getPurchaseOrderId())
                        .warehouseId(request.getWarehouseId())
                        .supplierId(request.getSupplierId())
                        .amount(request.getAmount())
                        .paymentMethod(request.getPaymentMethod())
                        .remarks(request.getRemarks())
                        .requestedBy(requestedBy)
                        .razorpayOrderId(razorpayOrderId)
                        .status(PaymentStatus.PENDING)
                        .build();
            }

            Payment saved = paymentRepository.save(payment);
            log.info("[Razorpay] Order created: {} for payment record {}", razorpayOrderId, saved.getId());

            // Return what frontend needs to open checkout
            Map<String, Object> response = new HashMap<>();
            response.put("razorpayOrderId", razorpayOrderId);
            response.put("amount", (int)(saved.getAmount() * 100));
            response.put("currency", "INR");
            response.put("key", razorpayKeyId);
            response.put("paymentRecordId", saved.getId());
            response.put("referenceNumber", saved.getReferenceNumber());
            return response;

        } catch (RazorpayException e) {
            log.error("[Razorpay] Failed to create order: {}", e.getMessage());
            throw new PaymentException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // VERIFY PAYMENT SIGNATURE + COMPLETE (Step 2)
    // ─────────────────────────────────────────────
    @Override
    public Payment verifyAndCompletePayment(RazorpayVerifyRequest verifyRequest) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id",   verifyRequest.getRazorpayOrderId());
            attributes.put("razorpay_payment_id",  verifyRequest.getRazorpayPaymentId());
            attributes.put("razorpay_signature",   verifyRequest.getRazorpaySignature());

            //  Use SECRET key for HMAC-SHA256 signature verification (NOT the public key ID)
            Utils.verifyPaymentSignature(attributes, razorpayKeySecret);

            // Signature valid — mark COMPLETED
            Payment payment = getById(verifyRequest.getPaymentRecordId());
            assertCurrentUserCanAccessPayment(payment);
            payment.setRazorpayPaymentId(verifyRequest.getRazorpayPaymentId());
            payment.setRazorpaySignature(verifyRequest.getRazorpaySignature());
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());

            Payment saved = paymentRepository.save(payment);
            Invoice invoice = generateInvoice(saved);
            log.info("[Razorpay] Payment verified & completed: {}", verifyRequest.getRazorpayPaymentId());

            // Notify analytics-service
            rabbitTemplate.convertAndSend(EXCHANGE, "payment.completed",
                    PAYMENT_REF_PREFIX + saved.getReferenceNumber() + " completed via Razorpay. Invoice: "
                            + invoice.getInvoiceNumber() + ". Amount: INR " + saved.getAmount());

            return saved;

        } catch (RazorpayException e) {
            log.error("[Razorpay] Signature FAILED: {}", e.getMessage());
            Payment payment = getById(verifyRequest.getPaymentRecordId());
            assertCurrentUserCanAccessPayment(payment);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRejectionReason("Signature verification failed");
            paymentRepository.save(payment);
            throw new PaymentException("Payment verification failed");
        }
    }

    // ─────────────────────────────────────────────
    // CREATE PAYMENT (manual, non-Razorpay)
    // ─────────────────────────────────────────────
    @Override
    public Payment createPayment(PaymentRequestDTO request, String requestedBy) {
        Payment payment = Payment.builder()
                .purchaseOrderId(request.getPurchaseOrderId())
                .warehouseId(request.getWarehouseId())
                .supplierId(request.getSupplierId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .remarks(request.getRemarks())
                .requestedBy(requestedBy)
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("[Payment] Created {} for PO {} by {}", saved.getReferenceNumber(), request.getPurchaseOrderId(), requestedBy);

        rabbitTemplate.convertAndSend(EXCHANGE, "payment.requested",
                PAYMENT_REF_PREFIX + saved.getReferenceNumber() + " needs approval for PO " + request.getPurchaseOrderId());

        return saved;
    }

    // ─────────────────────────────────────────────
    // PROCESS (manual complete — MANAGER)
    // ─────────────────────────────────────────────
    @Override
    public Payment processPayment(Long id, String processedBy) {
        Payment payment = getById(id);
        assertCurrentUserCanAccessPayment(payment);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("Only PENDING payments can be processed. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setApprovedBy(processedBy);

        Payment saved = paymentRepository.save(payment);
        Invoice invoice = generateInvoice(saved);
        log.info("[Payment] Processed {} by {}", saved.getReferenceNumber(), processedBy);

        rabbitTemplate.convertAndSend(EXCHANGE, "payment.completed",
                PAYMENT_REF_PREFIX + saved.getReferenceNumber() + " completed. Invoice: "
                        + invoice.getInvoiceNumber() + ". Amount: INR " + saved.getAmount());

        return saved;
    }

    // ─────────────────────────────────────────────
    // FAIL PAYMENT
    // ─────────────────────────────────────────────
    @Override
    public Payment failPayment(Long id, String reason) {
        Payment payment = getById(id);
        assertCurrentUserCanAccessPayment(payment);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setRejectionReason(reason);
        Payment saved = paymentRepository.save(payment);
        log.warn("[Payment] {} FAILED. Reason: {}", saved.getReferenceNumber(), reason);
        return saved;
    }

    // ─────────────────────────────────────────────
    // CANCEL PAYMENT
    // ─────────────────────────────────────────────
    @Override
    public Payment cancelPayment(Long id, String reason) {
        Payment payment = getById(id);
        assertCurrentUserCanAccessPayment(payment);
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setRejectionReason(reason);
        Payment saved = paymentRepository.save(payment);
        log.info("[Payment] {} cancelled. Reason: {}", saved.getReferenceNumber(), reason);
        return saved;
    }

    // ─────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────
    @Override
    public Payment getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        assertCurrentUserCanAccessPayment(payment);
        return payment;
    }

    // ─────────────────────────────────────────────
    // GET BY PO
    // ─────────────────────────────────────────────
    @Override
    public List<Payment> getByPurchaseOrder(Long purchaseOrderId) {
        return visibleToCurrentUser(paymentRepository.findByPurchaseOrderId(purchaseOrderId));
    }

    // ─────────────────────────────────────────────
    // GET ALL
    // ─────────────────────────────────────────────
    @Override
    public List<Payment> getAllPayments() {
        return visibleToCurrentUser(paymentRepository.findAll());
    }

    @Override
    public Invoice getInvoiceByPayment(Long paymentId) {
        Payment payment = getById(paymentId);
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentException("Invoice is generated only after payment is completed");
        }

        return invoiceRepository.findByPaymentId(paymentId)
                .orElseGet(() -> generateInvoice(payment));
    }

    public Long resolveWarehouseId(String warehouseName) {
        try {
            return warehouseClient.getAllWarehouses()
                    .stream()
                    .filter(w -> warehouseName.equals(w.get("name")))
                    .map(w -> {
                        Object id = w.get("warehouseId");
                        if (id instanceof Number) return ((Number) id).longValue();
                        return null;
                    })
                    .filter(id -> id != null)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[Payment] Could not resolve warehouse '{}' for current user visibility: {}", warehouseName, e.getMessage());
            return null;
        }
    }

    private List<Payment> visibleToCurrentUser(List<Payment> payments) {
        return payments.stream()
                .filter(this::isVisibleToCurrentUser)
                .toList();
    }

    private boolean isVisibleToCurrentUser(Payment payment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return true;
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return true;
        }

        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()));
        if (!isManager) {
            return false;
        }

        String department = auth.getDetails() instanceof String ? (String) auth.getDetails() : null;
        Long managerWarehouseId = department == null || department.isBlank() ? null : resolveWarehouseId(department);
        return payment.getWarehouseId() != null && payment.getWarehouseId().equals(managerWarehouseId);
    }

    private void assertCurrentUserCanAccessPayment(Payment payment) {
        if (!isVisibleToCurrentUser(payment)) {
            throw new AccessDeniedException("You are not allowed to access this payment request");
        }
    }

    private Invoice generateInvoice(Payment payment) {
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentException("Cannot generate invoice for non-completed payment");
        }

        return invoiceRepository.findByPaymentId(payment.getId())
                .orElseGet(() -> {
                    Invoice invoice = Invoice.builder()
                            .paymentId(payment.getId())
                            .purchaseOrderId(payment.getPurchaseOrderId())
                            .warehouseId(payment.getWarehouseId())
                            .supplierId(payment.getSupplierId())
                            .amount(payment.getAmount())
                            .paymentReferenceNumber(payment.getReferenceNumber())
                            .paymentMethod(payment.getPaymentMethod())
                            .razorpayPaymentId(payment.getRazorpayPaymentId())
                            .paidAt(payment.getPaidAt())
                            .status("PAID")
                            .build();

                    Invoice saved = invoiceRepository.save(invoice);
                    log.info("[Invoice] Generated {} for payment {}", saved.getInvoiceNumber(), payment.getReferenceNumber());
                    return saved;
                });
    }
}
