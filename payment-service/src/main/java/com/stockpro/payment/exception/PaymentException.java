package com.stockpro.payment.exception;

/**
 * Thrown when a payment operation is invalid or fails.
 * Replaces generic RuntimeException for Sonar compliance.
 */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
