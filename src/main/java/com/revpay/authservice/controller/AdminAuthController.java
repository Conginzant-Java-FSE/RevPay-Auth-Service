package com.revpay.authservice.controller;

import com.revpay.authservice.dto.AdminLoginRequest;
import com.revpay.authservice.dto.AdminLoginResponse;
import com.revpay.authservice.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminAuthController {

    @Autowired
    private AdminAuthService adminAuthService;

    // POST /api/admin/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminAuthService.login(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "token",   response.getToken(),
                "adminId", response.getAdminId(),
                "email",   response.getEmail()
        ));
    }

    // POST /api/admin/logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        adminAuthService.logout(authHeader);
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }
}
