package com.stockpro.alert.exception;

public class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException(Long id) {
        super("Alert not found with ID: " + id);
    }

    public AlertNotFoundException(String message) {
        super(message);
    }
}
