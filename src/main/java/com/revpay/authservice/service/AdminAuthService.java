package com.revpay.authservice.service;

import com.revpay.authservice.dto.AdminLoginRequest;
import com.revpay.authservice.dto.AdminLoginResponse;
import com.revpay.authservice.enums.Role;
import com.revpay.authservice.model.User;
import com.revpay.authservice.repository.UserRepository;
import com.revpay.authservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public AdminLoginResponse login(AdminLoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid admin credentials"));

        if (user.getRole() != Role.ADMIN) {
            logger.warn("Non-admin login attempt: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid admin credentials");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Admin account is disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Wrong password for admin: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid admin credentials");
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getAccountType().name());

        logger.info("Admin login successful: {}", user.getEmail());

        return AdminLoginResponse.builder()
                .token(token)
                .adminId(String.valueOf(user.getId()))
                .email(user.getEmail())
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String email = jwtUtil.extractUsername(token);
                logger.info("Admin logout: {}", email);
            } catch (Exception ignored) {}
        }
    }
}
