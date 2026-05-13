package com.stockpro.payment;

import com.stockpro.payment.dto.PaymentRequestDTO;
import com.stockpro.payment.entity.Invoice;
import com.stockpro.payment.entity.Payment;
import com.stockpro.payment.entity.PaymentStatus;
import com.stockpro.payment.exception.ResourceNotFoundException;
import com.stockpro.payment.client.WarehouseClient;
import com.stockpro.payment.repository.InvoiceRepository;
import com.stockpro.payment.repository.PaymentRepository;
import com.stockpro.payment.service.impl.PaymentServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl - Unit Tests")
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private WarehouseClient warehouseClient;

    @InjectMocks private PaymentServiceImpl paymentService;

    private Payment testPayment;
    private PaymentRequestDTO testRequest;

    @BeforeEach
    void setUp() {
        testPayment = Payment.builder()
                .id(1L)
                .purchaseOrderId(10L)
                .supplierId(5L)
                .amount(50000.0)
                .paymentMethod("BANK_TRANSFER")
                .status(PaymentStatus.PENDING)
                .referenceNumber("PAY-12345")
                .requestedBy("officer@stockpro.com")
                .build();

        testRequest = new PaymentRequestDTO();
        testRequest.setPurchaseOrderId(10L);
        testRequest.setSupplierId(5L);
        testRequest.setAmount(50000.0);
        testRequest.setPaymentMethod("BANK_TRANSFER");
    }

    @Test
    @DisplayName("createPayment() - should save with PENDING status and publish event")
    void createPayment_ShouldSaveWithPendingStatus() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        Payment result = paymentService.createPayment(testRequest, "officer@stockpro.com");

        assertThat(result).isNotNull();
        assertThat(result.getReferenceNumber()).isEqualTo("PAY-12345");
        verify(paymentRepository).save(any(Payment.class));
        verify(rabbitTemplate).convertAndSend(anyString(), eq("payment.requested"), anyString());
    }

    @Test
    @DisplayName("processPayment() - should set COMPLETED + paidAt + publish event")
    void processPayment_ShouldSetCompleted() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.findByPaymentId(1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.processPayment(1L, "manager@stockpro.com");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(result.getApprovedBy()).isEqualTo("manager@stockpro.com");
        verify(rabbitTemplate).convertAndSend(anyString(), eq("payment.completed"), anyString());
    }

    @Test
    @DisplayName("processPayment() - should throw when payment is not PENDING")
    void processPayment_ShouldThrow_WhenNotPending() {
        testPayment.setStatus(PaymentStatus.COMPLETED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        assertThatThrownBy(() -> paymentService.processPayment(1L, "manager@stockpro.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only PENDING payments can be processed");
    }

    @Test
    @DisplayName("failPayment() - should set status FAILED with reason")
    void failPayment_ShouldSetFailed() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.failPayment(1L, "Bank rejected transaction");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getRejectionReason()).isEqualTo("Bank rejected transaction");
    }

    @Test
    @DisplayName("cancelPayment() - should set status CANCELLED with reason")
    void cancelPayment_ShouldSetCancelled() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.cancelPayment(1L, "Duplicate payment");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(result.getRejectionReason()).isEqualTo("Duplicate payment");
    }

    @Test
    @DisplayName("getById() - should return payment when found")
    void getById_ShouldReturnPayment() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        Payment result = paymentService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getById() - should throw ResourceNotFoundException when not found")
    void getById_ShouldThrow_WhenNotFound() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    @DisplayName("getByPurchaseOrder() - should return payments for a PO")
    void getByPurchaseOrder_ShouldReturnList() {
        when(paymentRepository.findByPurchaseOrderId(10L)).thenReturn(List.of(testPayment));

        List<Payment> result = paymentService.getByPurchaseOrder(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPurchaseOrderId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getAllPayments() - should return all payments")
    void getAllPayments_ShouldReturnAll() {
        when(paymentRepository.findAll()).thenReturn(List.of(testPayment));

        List<Payment> result = paymentService.getAllPayments();

        assertThat(result).hasSize(1);
        verify(paymentRepository).findAll();
    }
}
