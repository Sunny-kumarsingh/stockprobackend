package com.stockpro.authservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Pattern(
        regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "Please provide a valid email address (e.g. user@example.com)"
    )
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[@$!%*?&]).{6,64}$",
        message = "Password must be 6-64 characters and contain at least 1 uppercase letter and 1 special character (@$!%*?&)"
    )
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "ADMIN|MANAGER|OFFICER|STAFF|VIEWER",
        message = "Role must be one of: ADMIN, MANAGER, OFFICER, STAFF, VIEWER"
    )
    private String role;

    @Pattern(
        regexp = "^(\\+91)?[0-9]{10}$",
        message = "Phone number must be 10 digits, optionally prefixed with +91 (e.g. 9876543210 or +919876543210)"
    )
    private String phone;

    private String department;
}