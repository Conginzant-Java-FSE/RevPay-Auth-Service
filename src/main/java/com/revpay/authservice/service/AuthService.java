package com.revpay.authservice.service;

import com.revpay.authservice.client.NotificationFeignClient;
import com.revpay.authservice.dto.*;
import com.revpay.authservice.exception.SecurityAnswerMismatchException;
import com.revpay.authservice.exception.UserNotFoundException;
import com.revpay.authservice.model.BlacklistedToken;
import com.revpay.authservice.model.User;
import com.revpay.authservice.repository.BlacklistedTokenRepository;
import com.revpay.authservice.repository.UserRepository;
import com.revpay.authservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired private BlacklistedTokenRepository blacklistedTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    // OpenFeign client — calls notification-service via Eureka
    @Autowired private NotificationFeignClient notificationFeignClient;

    // ── Logout ────────────────────────────────────────────────────────────────
    @Transactional
    public void logout(String token) {
        if (blacklistedTokenRepository.existsByToken(token)) {
            throw new RuntimeException("Token already invalidated");
        }
        try {
            Long userId = jwtUtil.extractUserId(token);
            Date expirationDate = jwtUtil.extractExpiration(token);
            LocalDateTime expiresAt = expirationDate.toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            blacklistedTokenRepository.save(
                    new BlacklistedToken(token, LocalDateTime.now(), expiresAt, userId));
        } catch (Exception e) {
            throw new RuntimeException("Invalid token");
        }
    }

    // ── Step 1: verify user exists → return security question ────────────────
    public VerifyIdentityResponse verifyIdentity(VerifyIdentityRequest request) {
        User user = userRepository.findByEmail(request.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(request.getEmailOrPhone()))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (!user.isActive()) throw new RuntimeException("Account is inactive");
        return new VerifyIdentityResponse(true, "Identity verified successfully", user.getSecurityQuestion());
    }

    // ── Step 2: validate security Q&A → return short-lived reset token ───────
    public ValidateSecurityResponse validateSecurity(ValidateSecurityRequest request) {
        User user = userRepository.findByEmail(request.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(request.getEmailOrPhone()))
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (!user.getSecurityQuestion().equalsIgnoreCase(request.getSecurityQuestion()))
            throw new SecurityAnswerMismatchException("Security question mismatch");
        if (!passwordEncoder.matches(request.getSecurityAnswer(), user.getSecurityAnswer()))
            throw new SecurityAnswerMismatchException("Security answer incorrect");
        String resetToken = jwtUtil.generateResetToken(user.getId());
        return new ValidateSecurityResponse(true, "Security validated successfully", resetToken);
    }

    // ── Step 3: reset password using reset token ──────────────────────────────
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.extractAllClaims(request.getResetToken());
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired reset token");
        }
        if (!"RESET_PASSWORD".equals(claims.getSubject()))
            throw new RuntimeException("Invalid reset token");

        Long userId = claims.get("userId", Long.class);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Notify via OpenFeign → notification-service
        sendPasswordChangedNotification(user.getId());
        logger.info("Password reset via token for userId: {}", userId);
    }

    // ── Cleanup expired blacklisted tokens (daily at 2 AM) ───────────────────
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        blacklistedTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        logger.info("Cleaned up expired blacklisted tokens at: {}", LocalDateTime.now());
    }

    // ── Send notification via Feign ───────────────────────────────────────────
    private void sendPasswordChangedNotification(Long userId) {
        try {
            notificationFeignClient.createNotification(Map.of(
                    "userId",  userId,
                    "message", "Your password was changed successfully. If this wasn't you, contact support immediately.",
                    "type",    "SECURITY_ALERT"
            ));
        } catch (Exception e) {
            // Fallback handles this — auth-service never crashes due to notification failure
            logger.warn("Failed to send password-change notification for userId {}: {}", userId, e.getMessage());
        }
    }
}
