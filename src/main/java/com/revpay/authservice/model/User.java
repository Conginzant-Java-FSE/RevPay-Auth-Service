package com.revpay.authservice.model;
import com.revpay.authservice.config.AuditConfig;
import com.revpay.authservice.enums.AccountType;
import com.revpay.authservice.enums.Role;
import com.revpay.authservice.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mt_pin")
    private String mtPin;

    @Column(unique = true)
    private String username;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String securityQuestion;

    @Column(nullable = true)
    private String securityAnswer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private AccountType accountType;

    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Column(name = "deactivation_reason", columnDefinition = "TEXT")
    private String deactivationReason;

    @Column(name = "daily_limit")
    private Double dailyLimit = 100000.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

}
