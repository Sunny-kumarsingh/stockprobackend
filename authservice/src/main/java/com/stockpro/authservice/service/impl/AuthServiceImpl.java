package com.stockpro.authservice.service.impl;

import com.stockpro.authservice.dto.AdminCreateUserRequest;
import com.stockpro.authservice.dto.AuthResponse;
import com.stockpro.authservice.dto.LoginRequest;
import com.stockpro.authservice.dto.RegisterRequest;
import com.stockpro.authservice.entities.User;
import com.stockpro.authservice.entities.Role;
import com.stockpro.authservice.exception.GoogleAuthException;
import com.stockpro.authservice.exception.ResourceNotFoundException;
import com.stockpro.authservice.exception.UserAlreadyExistsException;

import com.stockpro.authservice.repository.UserRepository;
import com.stockpro.authservice.security.TokenBlacklistService;
import com.stockpro.authservice.service.AuthService;
import com.stockpro.authservice.util.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${google.client.id}")
    private String googleClientId;

    //  REGISTER
    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());        //  Added
        user.setDepartment(request.getDepartment()); //  Added
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);
        user.setRole(Role.VIEWER);  // By default, public self-registrations are VIEWER only

        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken);
    }

    //  LOGIN
    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with this email."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password. Please try again.");
        }

        //  BUG FIX: Prevent inactive users from logging in
        if (!user.isActive()) {
            throw new RuntimeException("This account has been deactivated. Please contact support.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken);
    }
    //  LOGOUT (Simple version)
    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        if (!jwtUtil.validateToken(token)) {
            return;
        }

        long remainingMillis = jwtUtil.getRemainingValidityMillis(token);
        tokenBlacklistService.blacklist(token, Duration.ofMillis(remainingMillis));
    }

    // ✅ VALIDATE TOKEN
    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token) && !tokenBlacklistService.isBlacklisted(token);
    }

    // ✅ REFRESH TOKEN
    @Override
    public AuthResponse refreshToken(String token) {
        if (!validateToken(token)) {
            throw new RuntimeException("Invalid or expired refresh token.");
        }

        String email = jwtUtil.extractUsername(token);
        User user = userRepository.findByEmail(email).orElseThrow();
        
        // Generate new access token and a new refresh token (standard practice)
        String newToken = jwtUtil.generateToken(user); 
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        
        return new AuthResponse(newToken, newRefreshToken);
    }



    @Override
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    // GET USER BY EMAIL
    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    //  UPDATE PROFILE BY EMAIL
    @Override
    public User updateProfileByEmail(String email, RegisterRequest request) {
        User user = getUserByEmail(email);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        return userRepository.save(user);
    }

    //  CHANGE PASSWORD BY EMAIL
    @Override
    public void changePasswordByEmail(String email, String oldPassword, String newPassword) {
        User user = getUserByEmail(email);
        
        //  Verify the old password matches before allowing a change
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Old password does not match!");
        }
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }


    //  DEACTIVATE USER
    @Override
    public void deactivateUser(String userId) {

        User user = getUserById(userId);
        user.setActive(!user.isActive());  // ✅ FIXED

        userRepository.save(user);
    }

    //  DELETE USER (Hard Delete as per PDF)
    @Override
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
    }

    //  UPDATE USER ROLE
    @Override
    public void updateUserRole(String userId, String role) {
        User user = getUserById(userId);
        try {
            user.setRole(Role.valueOf(role.toUpperCase()));
            userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role provided: " + role);
        }
    }

    //  GET ALL USERS (SCOPED)
    @Override
    public List<User> getAllUsersScoped(String currentUserEmail) {
        // 1. Get the details of the person making the request
        User currentUser = getUserByEmail(currentUserEmail);

        // 2. If they are an ADMIN, they see everyone
        if (currentUser.getRole() == Role.ADMIN) {
            return userRepository.findAll();
        }

        // 3. If they are a MANAGER, they only see their own department
        if (currentUser.getRole() == Role.MANAGER) {
            return userRepository.findByDepartment(currentUser.getDepartment());
        }

        // 4. Otherwise, return empty
        return List.of();
    }

    // GET USERS BY WAREHOUSE/DEPARTMENT
    @Override
    public List<User> getUsersByDepartment(String department) {
        return userRepository.findByDepartment(department);
    }

    // UPDATE USER DEPARTMENT (warehouse assignment)
    @Override
    public void updateUserDepartment(String userId, String department) {
        User user = getUserById(userId);
        user.setDepartment(department);
        userRepository.save(user);
    }


    @Override
    public AuthResponse createUserByAdmin(AdminCreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(true);

        //  Set custom role from Admin
        user.setRole(Role.valueOf(request.getRole().toUpperCase()));

        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken);
    }

    //  Google OAuth Login
    @Override
    public AuthResponse loginWithGoogle(String googleIdTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleIdTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                String email = payload.getEmail();
                String name = (String) payload.get("name");

                // Find or create user
                User user = userRepository.findByEmail(email).orElse(null);
                if (user == null) {
                    user = new User();
                    user.setUserId(UUID.randomUUID().toString());
                    user.setEmail(email);
                    user.setFullName(name);
                    user.setPasswordHash(""); // No password for OAuth users
                    user.setActive(true);
                    user.setRole(Role.VIEWER); // Default role
                    user = userRepository.save(user);
                }

                String token = jwtUtil.generateToken(user);
                String refreshToken = jwtUtil.generateRefreshToken(user);

                return new AuthResponse(token, refreshToken);
            } else {
                throw new GoogleAuthException("Invalid Google ID token.");
            }
        } catch (GoogleAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new GoogleAuthException("Failed to verify Google token", e);
        }
    }
}
