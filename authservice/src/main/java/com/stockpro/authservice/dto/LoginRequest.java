package com.stockpro.authservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Pattern(
        regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "Please provide a valid email address (e.g. user@example.com)"
    )
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}