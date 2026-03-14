package com.revpay.authservice.dto;

public class AdminLoginResponse {
    private String token;

    private String adminId;

    private String email;

    public AdminLoginResponse() {}

    public AdminLoginResponse(String token, String adminId, String email) {
        this.token = token; this.adminId = adminId; this.email = email;
    }

    public static Builder builder() { return new Builder(); }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }

    public String getAdminId() { return adminId; }

    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public static class Builder {
        private String token; private String adminId; private String email;
        public Builder token(String token) { this.token = token; return this; }
        public Builder adminId(String adminId) { this.adminId = adminId; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public AdminLoginResponse build() { return new AdminLoginResponse(token, adminId, email); }
    }

}
