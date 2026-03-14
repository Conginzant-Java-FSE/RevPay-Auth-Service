package com.revpay.authservice.dto;
public class VerifyIdentityResponse {

    private boolean success;

    private String message;

    private String securityQuestion;

    public VerifyIdentityResponse() {}

    public VerifyIdentityResponse(boolean success, String message, String securityQuestion) {
        this.success = success; this.message = message; this.securityQuestion = securityQuestion;
    }

    public boolean isSuccess() { return success; }

    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }

    public void setMessage(String message) { this.message = message; }

    public String getSecurityQuestion() { return securityQuestion; }

    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }

}
