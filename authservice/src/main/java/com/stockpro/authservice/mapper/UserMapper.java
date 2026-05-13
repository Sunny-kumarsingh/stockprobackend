package com.stockpro.authservice.mapper;

import com.stockpro.authservice.dto.UserDto;
import com.stockpro.authservice.entities.User;

public class UserMapper {

    private UserMapper() {
        // Utility class — not instantiable
    }

    public static UserDto toDto(User user) {

        UserDto dto = new UserDto();

        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setDepartment(user.getDepartment());
        dto.setActive(user.isActive());

        return dto;
    }
}