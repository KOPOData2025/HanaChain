package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationSession extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "verification_seq")
    @SequenceGenerator(name = "verification_seq", sequenceName = "verification_sequence", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String email;
    
    @Column(nullable = false, length = 6)
    private String verificationCode;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationType type;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isMaxAttemptsReached() {
        return attemptCount >= 5; // 최대 5회 시도
    }
    
    public void incrementAttempt() {
        this.attemptCount++;
    }
    
    public enum VerificationType {
        EMAIL_REGISTRATION,
        PASSWORD_RESET,
        EMAIL_CHANGE
    }
}
