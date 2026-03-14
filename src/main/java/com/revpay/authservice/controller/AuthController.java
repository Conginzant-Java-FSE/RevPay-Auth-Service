package com.revpay.authservice.controller;

import com.revpay.authservice.dto.*;
import com.revpay.authservice.service.AuthService;
import com.revpay.authservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {
        UserRegistrationResponse response = userService.register(request);
        if (response.getMessage().contains("already")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid authorization header"));
            }
            String token = authHeader.substring(7);
            authService.logout(token);
            return ResponseEntity.ok(new LogoutResponse("Logout successful", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/auth/users
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<UserListResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // POST /api/auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        logger.info("Forgot-password request for: {}", request.getEmailOrPhone());
        ForgotPasswordResponse response = userService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    // POST /api/auth/forgot-password/verify-identity
    @PostMapping("/forgot-password/verify-identity")
    public ResponseEntity<VerifyIdentityResponse> verifyIdentity(
            @RequestBody VerifyIdentityRequest request) {
        return ResponseEntity.ok(authService.verifyIdentity(request));
    }

    // POST /api/auth/forgot-password/validate-security
    @PostMapping("/forgot-password/validate-security")
    public ResponseEntity<ValidateSecurityResponse> validateSecurity(
            @RequestBody ValidateSecurityRequest request) {
        return ResponseEntity.ok(authService.validateSecurity(request));
    }

    // POST /api/auth/forgot-password/reset
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Password reset successfully"));
    }
}
