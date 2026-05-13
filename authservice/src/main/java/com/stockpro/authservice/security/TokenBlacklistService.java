package com.stockpro.authservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final RedisTemplate<String, String> redisTemplate;

    public void blacklist(String token, Duration ttl) {
        if (token == null || token.isBlank() || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(key(token), "revoked", ttl);
        } catch (RuntimeException ex) {
            log.warn("Unable to blacklist token because Redis is unavailable: {}", ex.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
        } catch (RuntimeException ex) {
            log.warn("Unable to check token blacklist because Redis is unavailable: {}", ex.getMessage());
            return false;
        }
    }

    private String key(String token) {
        return BLACKLIST_PREFIX + sha256(token);
    }

    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
