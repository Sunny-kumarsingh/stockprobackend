package com.stockpro.payment;

import com.stockpro.payment.controller.PaymentController;
import com.stockpro.payment.dto.PaymentRequestDTO;
import com.stockpro.payment.dto.RazorpayOrderRequest;
import com.stockpro.payment.dto.RazorpayVerifyRequest;
import com.stockpro.payment.entity.Payment;
import com.stockpro.payment.entity.PaymentStatus;
import com.stockpro.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService service;

    private PaymentController controller;
    private Payment payment;
    private PaymentRequestDTO request;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(service);
        payment = Payment.builder()
                .id(1L)
                .purchaseOrderId(10L)
                .supplierId(20L)
                .amount(999.0)
                .paymentMethod("UPI")
                .status(PaymentStatus.PENDING)
                .build();

        request = new PaymentRequestDTO();
        request.setPurchaseOrderId(10L);
        request.setSupplierId(20L);
        request.setAmount(999.0);
        request.setPaymentMethod("UPI");
    }

    @Test
    void createAndRazorpayEndpointsDelegate() {
        RazorpayOrderRequest orderRequest = new RazorpayOrderRequest();
        RazorpayVerifyRequest verifyRequest = new RazorpayVerifyRequest();
        when(service.createPayment(request, "officer")).thenReturn(payment);
        when(service.createRazorpayOrder(orderRequest, "manager"))
                .thenReturn(Map.of("orderId", "order_123"));
        when(service.verifyAndCompletePayment(verifyRequest)).thenReturn(payment);

        assertThat(controller.createPayment(request, "officer").getBody()).isSameAs(payment);
        assertThat(controller.createRazorpayOrder(orderRequest, "manager").getBody())
                .containsEntry("orderId", "order_123");
        assertThat(controller.verifyPayment(verifyRequest).getBody()).isSameAs(payment);
    }

    @Test
    void stateTransitionEndpointsDelegate() {
        when(service.processPayment(1L, "manager")).thenReturn(payment);
        when(service.failPayment(1L, "bad signature")).thenReturn(payment);
        when(service.cancelPayment(1L, "duplicate")).thenReturn(payment);

        assertThat(controller.processPayment(1L, "manager").getBody()).isSameAs(payment);
        assertThat(controller.failPayment(1L, "bad signature").getBody()).isSameAs(payment);
        assertThat(controller.cancelPayment(1L, "duplicate").getBody()).isSameAs(payment);
    }

    @Test
    void readEndpointsDelegate() {
        when(service.getById(1L)).thenReturn(payment);
        when(service.getAllPayments()).thenReturn(List.of(payment));
        when(service.getByPurchaseOrder(10L)).thenReturn(List.of(payment));

        assertThat(controller.getById(1L).getBody()).isSameAs(payment);
        assertThat(controller.getAllPayments().getBody()).hasSize(1);
        assertThat(controller.getByPurchaseOrder(10L).getBody()).hasSize(1);
    }
}
