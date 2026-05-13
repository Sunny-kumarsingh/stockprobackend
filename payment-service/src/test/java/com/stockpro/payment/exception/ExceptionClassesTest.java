package com.stockpro.payment.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for payment custom exception classes and GlobalExceptionHandler.
 */
class ExceptionClassesTest {

    // ─── PaymentException ──────────────────────────────────────────────

    @Test
    @DisplayName("PaymentException - message constructor")
    void paymentException_message() {
        PaymentException ex = new PaymentException("Payment failed");
        assertThat(ex.getMessage()).isEqualTo("Payment failed");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("PaymentException - message + cause constructor")
    void paymentException_messageAndCause() {
        Throwable cause = new IllegalStateException("razorpay error");
        PaymentException ex = new PaymentException("Order creation failed", cause);
        assertThat(ex.getMessage()).isEqualTo("Order creation failed");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // ─── ResourceNotFoundException ─────────────────────────────────────

    @Test
    @DisplayName("ResourceNotFoundException - message constructor")
    void resourceNotFoundException_message() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Payment 99 not found");
        assertThat(ex.getMessage()).isEqualTo("Payment 99 not found");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    // ─── GlobalExceptionHandler ────────────────────────────────────────

    @Test
    @DisplayName("GlobalExceptionHandler - handles ResourceNotFoundException")
    void handler_resourceNotFound() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> resp = handler.handleNotFound(new ResourceNotFoundException("not found"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsKey("error");
    }

    @Test
    @DisplayName("GlobalExceptionHandler - handles RuntimeException")
    void handler_runtimeException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> resp = handler.handleRuntime(new RuntimeException("bad request"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).containsKey("message");
    }

    @Test
    @DisplayName("GlobalExceptionHandler - handles general Exception")
    void handler_generalException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> resp = handler.handleGeneral(new Exception("server error"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).containsKey("timestamp");
    }
}
