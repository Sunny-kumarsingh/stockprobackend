package com.stockpro.authservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for custom exception classes and GlobalExceptionHandler
 * to ensure coverage on new code satisfies the SonarQube quality gate.
 */
class ExceptionClassesTest {

    // ─── GoogleAuthException ───────────────────────────────────────────

    @Test
    @DisplayName("GoogleAuthException - message constructor")
    void googleAuthException_message() {
        GoogleAuthException ex = new GoogleAuthException("Invalid token");
        assertThat(ex.getMessage()).isEqualTo("Invalid token");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("GoogleAuthException - message + cause constructor")
    void googleAuthException_messageAndCause() {
        Throwable cause = new IllegalStateException("root cause");
        GoogleAuthException ex = new GoogleAuthException("Failed to verify", cause);
        assertThat(ex.getMessage()).isEqualTo("Failed to verify");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // ─── ResourceNotFoundException ─────────────────────────────────────

    @Test
    @DisplayName("ResourceNotFoundException - message constructor")
    void resourceNotFoundException_message() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found: 42");
        assertThat(ex.getMessage()).isEqualTo("Not found: 42");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    // ─── UserAlreadyExistsException ────────────────────────────────────

    @Test
    @DisplayName("UserAlreadyExistsException - message constructor")
    void userAlreadyExistsException_message() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("Email already registered");
        assertThat(ex.getMessage()).isEqualTo("Email already registered");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    // ─── GlobalExceptionHandler ────────────────────────────────────────

    @Test
    @DisplayName("GlobalExceptionHandler - handles UserAlreadyExistsException")
    void handler_userAlreadyExists() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<String> resp = handler.handleUserExists(new UserAlreadyExistsException("dup"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isEqualTo("dup");
    }

    @Test
    @DisplayName("GlobalExceptionHandler - handles ResourceNotFoundException")
    void handler_resourceNotFound() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<String> resp = handler.handleResourceNotFound(new ResourceNotFoundException("missing"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isEqualTo("missing");
    }

    @Test
    @DisplayName("GlobalExceptionHandler - handles generic Exception")
    void handler_genericException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<String> resp = handler.handleGenericException(new RuntimeException("oops"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isEqualTo("oops");
    }
}
