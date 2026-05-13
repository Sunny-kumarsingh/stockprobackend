package com.stockpro.purchase.exception;

public class InactiveSupplierException extends RuntimeException {
    public InactiveSupplierException(String message) {
        super(message);
    }
}