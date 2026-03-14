package com.revpay.authservice.util;

import com.revpay.authservice.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
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
    private Long expiration;

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    // Generate a standard auth token
    public String generateToken(Long userId, String email, String accountType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("accountType", accountType);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Generate a short-lived password-reset token (5 minutes)
    public String generateResetToken(Long userId) {
        long resetExpiration = 5 * 60 * 1000;
        return Jwts.builder()
                .setSubject("RESET_PASSWORD")
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + resetExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        try {
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    public boolean validateToken(String token, String username) {
        try {
            String extracted = extractUsername(token);
            return extracted != null
                    && extracted.equals(username)
                    && !isTokenExpired(token)
                    && !isTokenBlacklisted(token);
        } catch (Exception e) {
            return false;
        }
    }
}
