package com.stockpro.authservice.dto;

import lombok.Data;

@Data
public class UserDto {

    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String department;
    private boolean active;
}