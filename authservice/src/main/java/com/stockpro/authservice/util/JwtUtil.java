package com.stockpro.authservice.util;

import com.stockpro.authservice.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
	
	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration}")
	private long expiration;


    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

 // We now pass the 'User' entity instead of just a String email
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();

        claims.put("role", user.getRole().name());
        //  Warehouse scoping — used by purchase-service, warehouse-service etc.
        if (user.getDepartment() != null && !user.getDepartment().isBlank()) {
            claims.put("department", user.getDepartment());
        }
        if (user.getUserId() != null) {
            claims.put("userId", user.getUserId());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }



    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Date extractExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    public long getRemainingValidityMillis(String token) {
        Date expirationDate = extractExpiration(token);
        return Math.max(0L, expirationDate.getTime() - System.currentTimeMillis());
    }

    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token — longer expiry (8x access token), marked with type=REFRESH
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("type", "REFRESH");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + (expiration * 8)))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
