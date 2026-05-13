package com.apigateway.apiservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "mysecretkeymysecretkeymysecretkey12345";

    private final JwtUtil jwtUtil = new JwtUtil();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "SECRET", SECRET);
    }

    @Test
    void validateToken_ShouldAcceptSignedToken() {
        String token = Jwts.builder()
                .setSubject("admin@stockpro.com")
                .setIssuedAt(new Date())
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();

        assertThatCode(() -> jwtUtil.validateToken(token)).doesNotThrowAnyException();
    }

    @Test
    void validateToken_ShouldRejectInvalidToken() {
        assertThatThrownBy(() -> jwtUtil.validateToken("not.a.valid.token"))
                .isInstanceOf(RuntimeException.class);
    }
}
