package com.stockpro.authservice.util;

import com.stockpro.authservice.entities.Role;
import com.stockpro.authservice.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtUtil — tests token generation, validation, and extraction.
 * Uses ReflectionTestUtils to inject @Value fields without Spring context.
 */
@DisplayName("JwtUtil - Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    // 256-bit minimum secret for HMAC-SHA256
    private static final String TEST_SECRET = "stockpro-super-secret-key-for-testing-jwt-256bit";
    private static final long TEST_EXPIRATION = 3600000L; // 1 hour in ms

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);

        testUser = new User();
        testUser.setEmail("test@stockpro.com");
        testUser.setRole(Role.MANAGER);
        testUser.setActive(true);
    }

    // ─── generateToken ────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken() - should return non-null JWT string")
    void generateToken_ShouldReturnToken() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generateToken() - token should contain user email as subject")
    void generateToken_ShouldContainEmailAsSubject() {
        String token = jwtUtil.generateToken(testUser);
        String extracted = jwtUtil.extractUsername(token);
        assertThat(extracted).isEqualTo("test@stockpro.com");
    }

    // ─── generateRefreshToken ─────────────────────────────────────────────────

    @Test
    @DisplayName("generateRefreshToken() - should return non-null JWT string")
    void generateRefreshToken_ShouldReturnToken() {
        String token = jwtUtil.generateRefreshToken(testUser);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generateRefreshToken() - token subject should be user email")
    void generateRefreshToken_ShouldContainEmailAsSubject() {
        String token = jwtUtil.generateRefreshToken(testUser);
        String extracted = jwtUtil.extractUsername(token);
        assertThat(extracted).isEqualTo("test@stockpro.com");
    }

    // ─── generateTokenFromUsername ────────────────────────────────────────────

    @Test
    @DisplayName("generateTokenFromUsername() - should return valid token")
    void generateTokenFromUsername_ShouldReturnToken() {
        String token = jwtUtil.generateTokenFromUsername("admin@stockpro.com");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generateTokenFromUsername() - token subject should match username")
    void generateTokenFromUsername_SubjectShouldMatch() {
        String token = jwtUtil.generateTokenFromUsername("admin@stockpro.com");
        String extracted = jwtUtil.extractUsername(token);
        assertThat(extracted).isEqualTo("admin@stockpro.com");
    }

    // ─── validateToken ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken() - should return true for a valid token")
    void validateToken_ShouldReturnTrue_ForValidToken() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken() - should return false for a tampered token")
    void validateToken_ShouldReturnFalse_ForTamperedToken() {
        assertThat(jwtUtil.validateToken("not.a.valid.jwt.token")).isFalse();
    }

    @Test
    @DisplayName("validateToken() - should return false for empty string")
    void validateToken_ShouldReturnFalse_ForEmptyToken() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    // ─── extractUsername ──────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUsername() - should extract correct email from token")
    void extractUsername_ShouldReturnCorrectEmail() {
        String token = jwtUtil.generateToken(testUser);
        String email = jwtUtil.extractUsername(token);
        assertThat(email).isEqualTo("test@stockpro.com");
    }

    @Test
    @DisplayName("extractUsername() - should throw for invalid token")
    void extractUsername_ShouldThrow_ForInvalidToken() {
        assertThatThrownBy(() -> jwtUtil.extractUsername("invalid-token"))
                .isInstanceOf(Exception.class);
    }

    // ─── different roles ──────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken() - should work for ADMIN role")
    void generateToken_ShouldWork_ForAdminRole() {
        testUser.setRole(Role.ADMIN);
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("generateToken() - should work for VIEWER role")
    void generateToken_ShouldWork_ForViewerRole() {
        testUser.setRole(Role.VIEWER);
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }
}
