package com.stockpro.purchase.exception;

public class OverReceiveException extends RuntimeException {
    public OverReceiveException(String message) {
        super(message);
    }
}