package com.revpay.authservice.service;

import com.revpay.authservice.client.NotificationFeignClient;
import com.revpay.authservice.dto.*;
import com.revpay.authservice.enums.AccountType;
import com.revpay.authservice.exception.SecurityAnswerMismatchException;
import com.revpay.authservice.exception.UserNotFoundException;
import com.revpay.authservice.model.User;
import com.revpay.authservice.repository.UserRepository;
import com.revpay.authservice.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    // OpenFeign client — calls notification-service via Eureka
    @Autowired private NotificationFeignClient notificationFeignClient;

    // ── Register new user ─────────────────────────────────────────────────────
    @Transactional
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent())
            return new UserRegistrationResponse("Email already registered");
        if (userRepository.findByPhone(request.getPhone()).isPresent())
            return new UserRegistrationResponse("Phone number already registered");

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setSecurityQuestion(request.getSecurityQuestion());
        user.setSecurityAnswer(passwordEncoder.encode(request.getSecurityAnswer().toLowerCase().trim()));
        user.setAccountType(request.getAccountType() != null ? request.getAccountType() : AccountType.PERSONAL);
        user.setActive(true);
        User saved = userRepository.save(user);

        // Send welcome notification via Feign to notification-service
        try {
            notificationFeignClient.createNotification(Map.of(
                    "userId",  saved.getId(),
                    "message", "Welcome to RevPay! Your account has been created successfully.",
                    "type",    "GENERAL"
            ));
        } catch (Exception e) {
            logger.warn("Could not send welcome notification for userId {}: {}", saved.getId(), e.getMessage());
        }

        logger.info("User registered: {}", request.getEmail());
        return new UserRegistrationResponse("User registered successfully");
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(request.getEmailOrPhone()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!user.isActive()) throw new RuntimeException("Account is inactive");
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid credentials");

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getAccountType().toString());
        logger.info("User logged in: {}", user.getEmail());
        return new LoginResponse(token, user.getId(), user.getFullName(), user.getEmail(), user.getAccountType());
    }

    // ── Forgot password (combined one-step flow) ──────────────────────────────
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(request.getEmailOrPhone()))
                .orElseThrow(() -> new UserNotFoundException("No user found with provided email or phone"));

        if (!user.isActive()) throw new RuntimeException("Account is inactive. Cannot reset password.");
        if (!user.getSecurityQuestion().equalsIgnoreCase(request.getSecurityQuestion()))
            throw new SecurityAnswerMismatchException("Security question does not match");
        if (!passwordEncoder.matches(request.getSecurityAnswer(), user.getSecurityAnswer()))
            throw new SecurityAnswerMismatchException("Security answer is incorrect");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        try {
            notificationFeignClient.createNotification(Map.of(
                    "userId",  user.getId(),
                    "message", "Your password was changed successfully. If this wasn't you, contact support immediately.",
                    "type",    "SECURITY_ALERT"
            ));
        } catch (Exception e) {
            logger.warn("Could not send password-change notification: {}", e.getMessage());
        }

        logger.info("Password reset via forgot-password for: {}", user.getEmail());
        return new ForgotPasswordResponse("Password reset successfully", true);
    }

    // ── Get all users (admin) ─────────────────────────────────────────────────
    public List<UserListResponse> getAllUsers() {
        return userRepository.findAll().stream().map(user -> {
            UserListResponse resp = new UserListResponse();
            resp.setUserId(user.getId());
            resp.setFullName(user.getFullName());
            resp.setEmail(user.getEmail());
            resp.setPhone(user.getPhone());
            resp.setAccountType(user.getAccountType());
            return resp;
        }).collect(Collectors.toList());
    }
}
