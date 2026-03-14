package com.revpay.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ForgotPasswordRequest {

    @NotBlank(message = "Email or phone number is required")
    private String emailOrPhone;

    @NotBlank(message = "Security question is required")
    private String securityQuestion;

    @NotBlank(message = "Security answer is required")
    private String securityAnswer;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;

    public ForgotPasswordRequest() {}

    public String getEmailOrPhone() { return emailOrPhone; }
    public void setEmailOrPhone(String emailOrPhone) { this.emailOrPhone = emailOrPhone; }

    public String getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }

    public String getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(String securityAnswer) { this.securityAnswer = securityAnswer; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
