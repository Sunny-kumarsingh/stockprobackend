package com.stockpro.authservice.service;

import java.util.List;

import com.stockpro.authservice.dto.AdminCreateUserRequest;
import com.stockpro.authservice.dto.AuthResponse;
import com.stockpro.authservice.dto.LoginRequest;
import com.stockpro.authservice.dto.RegisterRequest;
import com.stockpro.authservice.entities.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
    
    boolean validateToken(String token);

    void logout(String token);

    AuthResponse refreshToken(String refreshToken);

    User getUserById(String userId);

    User getUserByEmail(String email);

    User updateProfileByEmail(String email, RegisterRequest request);

    void changePasswordByEmail(String email, String oldPassword, String newPassword);

    void deactivateUser(String userId);

    void deleteUser(String userId);
    
    List<User> getAllUsersScoped(String currentUserEmail);

    AuthResponse createUserByAdmin(AdminCreateUserRequest request);
    
    void updateUserRole(String userId, String role);

    // Get all users by warehouse department
    List<User> getUsersByDepartment(String department);

    // Update user's warehouse department assignment
    void updateUserDepartment(String userId, String department);

    //  Google OAuth Login
    AuthResponse loginWithGoogle(String googleIdToken);
}