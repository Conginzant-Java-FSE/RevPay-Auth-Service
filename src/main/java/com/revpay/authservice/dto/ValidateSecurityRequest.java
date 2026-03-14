package com.revpay.authservice.dto;

public class ValidateSecurityRequest {
    private String emailOrPhone;

    private String securityQuestion;

    private String securityAnswer;

    public String getEmailOrPhone() { return emailOrPhone; }

    public void setEmailOrPhone(String emailOrPhone) { this.emailOrPhone = emailOrPhone; }

    public String getSecurityQuestion() { return securityQuestion; }

    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }

    public String getSecurityAnswer() { return securityAnswer; }

    public void setSecurityAnswer(String securityAnswer) { this.securityAnswer = securityAnswer; }

}
