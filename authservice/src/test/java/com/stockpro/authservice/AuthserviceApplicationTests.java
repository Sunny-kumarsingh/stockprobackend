package com.stockpro.authservice;

import com.stockpro.authservice.dto.AuthResponse;
import com.stockpro.authservice.dto.LoginRequest;
import com.stockpro.authservice.dto.RegisterRequest;
import com.stockpro.authservice.entities.Role;
import com.stockpro.authservice.entities.User;
import com.stockpro.authservice.exception.ResourceNotFoundException;
import com.stockpro.authservice.exception.UserAlreadyExistsException;
import com.stockpro.authservice.repository.UserRepository;
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
 * JUnit 5 + Mockito unit tests for AuthServiceImpl.
 * Tests: register, login, deactivate, delete, updateRole, getAllUsersScoped.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl - Unit Tests")
class AuthserviceApplicationTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtUtil;

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

	@Test
	@DisplayName("register() - should return tokens when email is new")
	void register_ShouldReturnAuthResponse_WhenEmailNotExists() {
		when(userRepository.existsByEmail("new@stockpro.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
		when(userRepository.save(any(User.class))).thenReturn(testUser);
		when(jwtUtil.generateToken(any(User.class))).thenReturn("access-token");
		when(jwtUtil.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

		AuthResponse response = authService.register(registerRequest);

		assertThat(response).isNotNull();
		assertThat(response.getToken()).isEqualTo("access-token");
		verify(userRepository).save(any(User.class));
	}

	@Test
	@DisplayName("register() - should throw UserAlreadyExistsException if email is taken")
	void register_ShouldThrow_WhenEmailAlreadyExists() {
		when(userRepository.existsByEmail("new@stockpro.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(registerRequest))
				.isInstanceOf(UserAlreadyExistsException.class)
				.hasMessageContaining("Email already exists");

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("login() - should return token when credentials are valid")
	void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
		when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
		when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);
		when(userRepository.save(any(User.class))).thenReturn(testUser);
		when(jwtUtil.generateToken(any(User.class))).thenReturn("access-token");
		when(jwtUtil.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

		AuthResponse response = authService.login(loginRequest);

		assertThat(response.getToken()).isEqualTo("access-token");
		verify(userRepository).save(any(User.class));
	}

	@Test
	@DisplayName("login() - should throw when email not found")
	void login_ShouldThrow_WhenEmailNotFound() {
		when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(loginRequest))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("login() - should throw when password is incorrect")
	void login_ShouldThrow_WhenPasswordIsWrong() {
		when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
		when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(loginRequest))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Invalid password");
	}

	@Test
	@DisplayName("login() - should throw when user account is deactivated")
	void login_ShouldThrow_WhenUserIsInactive() {
		testUser.setActive(false);
		when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
		when(passwordEncoder.matches("password123", "$2a$10$hashedpassword")).thenReturn(true);

		assertThatThrownBy(() -> authService.login(loginRequest))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("deactivated");
	}

	@Test
	@DisplayName("deactivateUser() - should toggle active status")
	void deactivateUser_ShouldToggleStatus() {
		testUser.setActive(true);
		when(userRepository.findById("user-uuid-123")).thenReturn(Optional.of(testUser));
		when(userRepository.save(any(User.class))).thenReturn(testUser);

		authService.deactivateUser("user-uuid-123");

		assertThat(testUser.isActive()).isFalse();
		verify(userRepository).save(testUser);
	}

	@Test
	@DisplayName("deleteUser() - should delete when user exists")
	void deleteUser_ShouldDelete_WhenUserExists() {
		when(userRepository.existsById("user-uuid-123")).thenReturn(true);

		authService.deleteUser("user-uuid-123");

		verify(userRepository).deleteById("user-uuid-123");
	}

	@Test
	@DisplayName("deleteUser() - should throw when user not found")
	void deleteUser_ShouldThrow_WhenUserNotFound() {
		when(userRepository.existsById("invalid-id")).thenReturn(false);

		assertThatThrownBy(() -> authService.deleteUser("invalid-id"))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("updateUserRole() - should update to valid role")
	void updateUserRole_ShouldUpdateRole_WhenValidRole() {
		when(userRepository.findById("user-uuid-123")).thenReturn(Optional.of(testUser));

		authService.updateUserRole("user-uuid-123", "MANAGER");

		assertThat(testUser.getRole()).isEqualTo(Role.MANAGER);
		verify(userRepository).save(testUser);
	}

	@Test
	@DisplayName("updateUserRole() - should throw for invalid role")
	void updateUserRole_ShouldThrow_WhenInvalidRole() {
		when(userRepository.findById("user-uuid-123")).thenReturn(Optional.of(testUser));

		assertThatThrownBy(() -> authService.updateUserRole("user-uuid-123", "SUPERADMIN"))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Invalid role");
	}

	@Test
	@DisplayName("getAllUsersScoped() - ADMIN sees all users")
	void getAllUsersScoped_AdminSeesAll() {
		testUser.setRole(Role.ADMIN);
		when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
		when(userRepository.findAll()).thenReturn(List.of(testUser));

		List<User> result = authService.getAllUsersScoped("test@stockpro.com");

		assertThat(result).hasSize(1);
		verify(userRepository).findAll();
	}

	@Test
	@DisplayName("getAllUsersScoped() - MANAGER sees only their department")
	void getAllUsersScoped_ManagerSeesDepartmentOnly() {
		testUser.setRole(Role.MANAGER);
		testUser.setDepartment("IT");
		when(userRepository.findByEmail("test@stockpro.com")).thenReturn(Optional.of(testUser));
		when(userRepository.findByDepartment("IT")).thenReturn(List.of(testUser));

		List<User> result = authService.getAllUsersScoped("test@stockpro.com");

		assertThat(result).hasSize(1);
		verify(userRepository).findByDepartment("IT");
	}
}
