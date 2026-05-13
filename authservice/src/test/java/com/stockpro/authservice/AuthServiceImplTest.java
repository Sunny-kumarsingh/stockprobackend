package com.stockpro.authservice;

import com.stockpro.authservice.dto.AdminCreateUserRequest;
import com.stockpro.authservice.dto.AuthResponse;
import com.stockpro.authservice.dto.LoginRequest;
import com.stockpro.authservice.dto.RegisterRequest;
import com.stockpro.authservice.entities.Role;
import com.stockpro.authservice.entities.User;
import com.stockpro.authservice.exception.ResourceNotFoundException;
import com.stockpro.authservice.exception.UserAlreadyExistsException;
import com.stockpro.authservice.repository.UserRepository;
import com.stockpro.authservice.security.TokenBlacklistService;
import com.stockpro.authservice.service.impl.AuthServiceImpl;
import com.stockpro.authservice.util.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl using JUnit 5 + Mockito.
 * Tests cover: register, login, logout, validateToken, refreshToken,
 *              getUserById, getUserByEmail, updateProfileByEmail,
 *              changePasswordByEmail, deactivateUser, deleteUser,
 *              updateUserRole, getAllUsersScoped, createUserByAdmin.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId("user-uuid-123");
        testUser.setEmail("test@stockpro.com");
        testUser.setFullName("Test User");
        testUser.setPasswordHash("$2a$10$hashedpassword");
        testUser.setRole(Role.VIEWER);
        testUser.setActive(true);
        testUser.setDepartment("IT");
        testUser.setPhone("9876543210");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("new@stockpro.com");
        registerRequest.setFullName("New User");
        registerRequest.setPassword("password123");
        registerRequest.setPhone("9999999999");
        registerRequest.setDepartment("HR");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@stockpro.com");
        loginRequest.setPassword("password123");
    }

    // ─────────────────────────────────────────────
    // REGISTER TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("register() - should register new user and return token")
    void register_ShouldReturnAuthResponse_WhenEmailNotExists() {
        when(userRepository.existsByEmail("new@stockpro.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register() - should throw UserAlreadyExistsException if email exists")
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("new@stockpro.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register() - new user should always get VIEWER role")
    void register_ShouldAlwaysAssignViewerRole() {
        when(userRepository.existsByEmail("new@stockpro.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(jwtUtil.generateToken(any())).thenReturn("token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh");

        // Capture what user was saved
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            assertThat(saved.getRole()).isEqualTo(Role.VIEWER);
            return testUser;
        });

        authService.register(registerRequest);

        verify(userRepository).save(any(User.class));
    }

    // ─────────────────────────────────────────────
    // LOGIN TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("login() - should return token when credentials are valid")
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token");
        verify(userRepository).save(any(User.class)); // lastLoginAt updated
    }

    @Test
    @DisplayName("login() - should throw ResourceNotFoundException if email not found")
    void login_ShouldThrowException_WhenEmailNotFound() {
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No account found");
    }

    @Test
    @DisplayName("login() - should throw RuntimeException if password is wrong")
    void login_ShouldThrowException_WhenPasswordIsIncorrect() {
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid password");
    }

    @Test
    @DisplayName("login() - should throw RuntimeException if account is deactivated")
    void login_ShouldThrowException_WhenUserIsInactive() {
        testUser.setActive(false);
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("deactivated");
    }

    // ─────────────────────────────────────────────
    // LOGOUT & VALIDATE TOKEN
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("logout() - should complete without error (stateless JWT)")
    void logout_ShouldCompleteWithoutError() {
        assertThatCode(() -> authService.logout("some-token"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateToken() - should return true for valid token")
    void validateToken_ShouldReturnTrue_WhenTokenIsValid() {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);

        boolean result = authService.validateToken("valid-token");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("validateToken() - should return false for invalid token")
    void validateToken_ShouldReturnFalse_WhenTokenIsInvalid() {
        when(jwtUtil.validateToken("bad-token")).thenReturn(false);

        boolean result = authService.validateToken("bad-token");

        assertThat(result).isFalse();
    }

    // ─────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("refreshToken() - should return new token pair")
    void refreshToken_ShouldReturnNewTokens_WhenTokenIsValid() {
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("refresh-token")).thenReturn(false);
        when(jwtUtil.extractUsername("refresh-token")).thenReturn("test@stockpro.com");
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUser)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refreshToken("refresh-token");

        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    // ─────────────────────────────────────────────
    // GET USER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getUserById() - should return user when found")
    void getUserById_ShouldReturnUser_WhenExists() {
        when(userRepository.findById("user-uuid-123")).thenReturn(Optional.of(testUser));

        User result = authService.getUserById("user-uuid-123");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@stockpro.com");
    }

    @Test
    @DisplayName("getUserById() - should throw ResourceNotFoundException when not found")
    void getUserById_ShouldThrow_WhenNotFound() {
        when(userRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById("invalid"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("getUserByEmail() - should return user when found")
    void getUserByEmail_ShouldReturnUser_WhenExists() {
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));

        User result = authService.getUserByEmail("test@stockpro.com");

        assertThat(result.getFullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("getUserByEmail() - should throw ResourceNotFoundException when not found")
    void getUserByEmail_ShouldThrow_WhenNotFound() {
        when(userRepository.findByEmail("ghost@stockpro.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserByEmail("ghost@stockpro.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────
    // UPDATE PROFILE
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("updateProfileByEmail() - should update name, phone, department")
    void updateProfileByEmail_ShouldUpdateFields_WhenUserExists() {
        registerRequest.setFullName("Updated Name");
        registerRequest.setPhone("1111111111");
        registerRequest.setDepartment("Finance");

        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.updateProfileByEmail("test@stockpro.com", registerRequest);

        assertThat(testUser.getFullName()).isEqualTo("Updated Name");
        assertThat(testUser.getPhone()).isEqualTo("1111111111");
        assertThat(testUser.getDepartment()).isEqualTo("IT");
        verify(userRepository).save(testUser);
    }

    // ─────────────────────────────────────────────
    // CHANGE PASSWORD
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("changePasswordByEmail() - should update password when old password matches")
    void changePasswordByEmail_ShouldUpdatePassword_WhenOldPasswordCorrect() {
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "$2a$10$hashedpassword")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHashedPassword");

        authService.changePasswordByEmail("test@stockpro.com", "oldPass", "newPass");

        assertThat(testUser.getPasswordHash()).isEqualTo("newHashedPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("changePasswordByEmail() - should throw when old password is wrong")
    void changePasswordByEmail_ShouldThrow_WhenOldPasswordWrong() {
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPass", "$2a$10$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() ->
                authService.changePasswordByEmail("test@stockpro.com", "wrongPass", "newPass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Old password does not match");
    }

    // ─────────────────────────────────────────────
    // DEACTIVATE USER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("deactivateUser() - should toggle active status from true to false")
    void deactivateUser_ShouldToggleActiveStatus_TrueToFalse() {
        testUser.setActive(true);
        when(userRepository.findById("user-uuid-123")).thenReturn(Optional.of(testUser));

        authService.deactivateUser("user-uuid-123");

        assertThat(testUser.isActive()).isFalse();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("deactivateUser() - should toggle active status from false to true")
    void deactivateUser_ShouldToggleActiveStatus_FalseToTrue() {
        testUser.setActive(false);
        when(userRepository.findById("user-uuid-123")).thenReturn(Optional.of(testUser));

        authService.deactivateUser("user-uuid-123");

        assertThat(testUser.isActive()).isTrue();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("deactivateUser() - should throw ResourceNotFoundException if user not found")
    void deactivateUser_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.deactivateUser("invalid-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────
    // DELETE USER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser() - should delete user when found")
    void deleteUser_ShouldDeleteSuccessfully_WhenUserExists() {
        when(userRepository.existsById("user-uuid-123")).thenReturn(true);

        authService.deleteUser("user-uuid-123");

        verify(userRepository).deleteById("user-uuid-123");
    }

    @Test
    @DisplayName("deleteUser() - should throw ResourceNotFoundException if user not found")
    void deleteUser_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.existsById("invalid-id")).thenReturn(false);

        assertThatThrownBy(() -> authService.deleteUser("invalid-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ─────────────────────────────────────────────
    // UPDATE ROLE
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("updateUserRole() - should update role successfully")
    void updateUserRole_ShouldUpdateRole_WhenValidRoleProvided() {
        when(userRepository.findById("user-uuid-123")).thenReturn(Optional.of(testUser));

        authService.updateUserRole("user-uuid-123", "MANAGER");

        assertThat(testUser.getRole()).isEqualTo(Role.MANAGER);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateUserRole() - should throw RuntimeException for invalid role")
    void updateUserRole_ShouldThrow_WhenInvalidRoleProvided() {
        when(userRepository.findById("user-uuid-123")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.updateUserRole("user-uuid-123", "SUPERADMIN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid role");
    }

    // ─────────────────────────────────────────────
    // SCOPED USER LISTING
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getAllUsersScoped() - ADMIN should see all users")
    void getAllUsersScoped_ShouldReturnAll_WhenCallerIsAdmin() {
        testUser.setRole(Role.ADMIN);
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<User> result = authService.getAllUsersScoped("test@stockpro.com");

        assertThat(result).hasSize(1);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("getAllUsersScoped() - MANAGER should only see their department")
    void getAllUsersScoped_ShouldReturnDepartmentOnly_WhenCallerIsManager() {
        testUser.setRole(Role.MANAGER);
        testUser.setDepartment("IT");
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByDepartment("IT")).thenReturn(List.of(testUser));

        List<User> result = authService.getAllUsersScoped("test@stockpro.com");

        assertThat(result).hasSize(1);
        verify(userRepository).findByDepartment("IT");
    }

    @Test
    @DisplayName("getAllUsersScoped() - VIEWER should see empty list")
    void getAllUsersScoped_ShouldReturnEmpty_WhenCallerIsViewer() {
        testUser.setRole(Role.VIEWER);
        when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));

        List<User> result = authService.getAllUsersScoped("test@stockpro.com");

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // ADMIN CREATE USER
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("createUserByAdmin() - should create user with specified role")
    void createUserByAdmin_ShouldCreateUser_WithSpecifiedRole() {
        AdminCreateUserRequest request = new AdminCreateUserRequest();
        request.setFullName("Staff Member");
        request.setEmail("staff@stockpro.com");
        request.setPassword("pass123");
        request.setRole("STAFF");
        request.setPhone("8888888888");
        request.setDepartment("Warehouse");

        when(userRepository.existsByEmail("staff@stockpro.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");
        when(jwtUtil.generateToken(any())).thenReturn("admin-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("admin-refresh");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            assertThat(u.getRole()).isEqualTo(Role.STAFF);
            assertThat(u.isActive()).isTrue();
            return u;
        });

        AuthResponse response = authService.createUserByAdmin(request);

        assertThat(response.getToken()).isEqualTo("admin-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUserByAdmin() - should throw if email already exists")
    void createUserByAdmin_ShouldThrow_WhenEmailAlreadyExists() {
        AdminCreateUserRequest request = new AdminCreateUserRequest();
        request.setEmail("test@stockpro.com"); // already exists
        request.setPassword("pass");
        request.setRole("STAFF");

        when(userRepository.existsByEmail("test@stockpro.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.createUserByAdmin(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }
}
