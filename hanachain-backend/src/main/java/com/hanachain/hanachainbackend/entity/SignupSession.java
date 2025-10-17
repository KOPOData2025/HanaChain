package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "signup_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class SignupSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "signup_session_seq")
    @SequenceGenerator(name = "signup_session_seq", sequenceName = "signup_session_seq", allocationSize = 1)
    private Long id;
    
    @Column(name = "session_id", nullable = false, unique = true, length = 36)
    private String sessionId;
    
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    @Column(name = "password_hash", length = 255)
    private String passwordHash;
    
    @Column(name = "nickname", length = 50)
    private String nickname;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "terms_accepted", nullable = false)
    @Builder.Default
    private Boolean termsAccepted = false;
    
    @Column(name = "privacy_accepted", nullable = false)
    @Builder.Default
    private Boolean privacyAccepted = false;
    
    @Column(name = "marketing_accepted")
    @Builder.Default
    private Boolean marketingAccepted = false;
    
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    @Builder.Default
    private SignupStep currentStep = SignupStep.TERMS;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(30); // 30분 후 만료
        }
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum SignupStep {
        TERMS,
        ACCOUNT,
        VERIFICATION,
        NICKNAME,
        COMPLETED
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean canProceedToStep(SignupStep targetStep) {
        if (isExpired()) {
            return false;
        }
        
        return switch (targetStep) {
            case TERMS -> true;
            case ACCOUNT -> currentStep.ordinal() >= SignupStep.TERMS.ordinal() && termsAccepted && privacyAccepted;
            case VERIFICATION -> currentStep.ordinal() >= SignupStep.ACCOUNT.ordinal() && passwordHash != null;
            case NICKNAME -> currentStep.ordinal() >= SignupStep.VERIFICATION.ordinal() && emailVerified;
            case COMPLETED -> currentStep.ordinal() >= SignupStep.VERIFICATION.ordinal() && emailVerified;
        };
    }
}