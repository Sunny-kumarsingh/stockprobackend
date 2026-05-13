package com.stockpro.authservice.exception;

/**
 * Thrown when Google OAuth token verification fails.
 * Replaces generic RuntimeException for Sonar compliance.
 */
public class GoogleAuthException extends RuntimeException {

    public GoogleAuthException(String message) {
        super(message);
    }

    public GoogleAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
