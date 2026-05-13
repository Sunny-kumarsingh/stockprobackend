package com.stockpro.authservice.controller;

import com.stockpro.authservice.dto.*;
import com.stockpro.authservice.entities.User;
import com.stockpro.authservice.mapper.UserMapper;
import com.stockpro.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;

    
    // 1. Register — Public self-registration (defaults to VIEWER)
    @Operation(summary = "Register a new user", security = {})
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // 2. Login
    @Operation(summary = "Login with email and password", security = {})
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // 2.5 Google OAuth Login
    @Operation(summary = "Login with Google ID token", security = {})
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@RequestBody java.util.Map<String, String> request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request.get("token")));
    }

    // 3. Logout
    @Operation(summary = "Logout and invalidate token", security = {})
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam String token) {
        authService.logout(token);
        return ResponseEntity.ok("Logged out successfully");
    }

    // 4. Refresh
    @Operation(summary = "Refresh access token", security = {})
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    // Update line 48 in AuthResource.java
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(Authentication authentication) {

        String email = authentication.getName();

        return ResponseEntity.ok(
                UserMapper.toDto(authService.getUserByEmail(email))
        );
    }

    // 6. Update Profile
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(Authentication authentication, @Valid @RequestBody RegisterRequest request) {
        
        String email = authentication.getName();
    
        // We fetch the user by email first, then update
        User updatedUser = authService.updateProfileByEmail(email, request);
        
        return ResponseEntity.ok(UserMapper.toDto(updatedUser));
    }


    // 7. Change Password
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(Authentication authentication,
                                                 @RequestParam String oldPassword,
                                                 @RequestParam String newPassword) {
        String email = authentication.getName();
        authService.changePasswordByEmail(email, oldPassword, newPassword);
        return ResponseEntity.ok("Password updated");
    }

    // 8. Deactivate
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/deactivate")
    public ResponseEntity<String> deactivateUser(@RequestParam("userId") String userId) {
        authService.deactivateUser(userId);
        return ResponseEntity.ok("User deactivated");
    }

    // 9. Delete User (Hard Delete)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable("userId") String userId) {
        authService.deleteUser(userId);
        return ResponseEntity.ok("User permanently deleted");
    }

    // 9.5 Update User Role (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<String> updateUserRole(@PathVariable("userId") String userId, @RequestParam("role") String role) {
        authService.updateUserRole(userId, role);
        return ResponseEntity.ok("User role updated successfully");
    }

    // 10. Get All Users
    //  ADMIN or MANAGER can access
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers(Authentication authentication) {
        
        String currentUserEmail = authentication.getName();

        List<UserDto> users = authService.getAllUsersScoped(currentUserEmail)
                .stream()
                .map(UserMapper::toDto)
                .toList();

        return ResponseEntity.ok(users);
    }

    // 10.5 Get Users by Warehouse (department)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/users/warehouse")
    public ResponseEntity<List<UserDto>> getUsersByWarehouse(@RequestParam("department") String department) {
        List<UserDto> users = authService.getUsersByDepartment(department)
                .stream()
                .map(UserMapper::toDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    // 10.6 Update user's warehouse/department assignment (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/department")
    public ResponseEntity<String> updateUserDepartment(
            @PathVariable("userId") String userId,
            @RequestParam("department") String department) {
        authService.updateUserDepartment(userId, department);
        return ResponseEntity.ok("Department updated successfully");
    }

    // 11. Admin Create User
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/users")
    public ResponseEntity<AuthResponse> adminCreateUser(@Valid @RequestBody com.stockpro.authservice.dto.AdminCreateUserRequest request) {
        return ResponseEntity.ok(authService.createUserByAdmin(request));
    }

}