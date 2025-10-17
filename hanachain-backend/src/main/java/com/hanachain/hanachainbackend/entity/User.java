package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", allocationSize = 1)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(nullable = false, length = 100)
    private String password;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Column(length = 20)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean termsAccepted = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean privacyAccepted = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean marketingAccepted = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
    
    // === 마이페이지 관련 필드 ===
    @Column(length = 50)
    private String nickname;
    
    @Column(length = 500)
    private String profileImage; // 프로필 이미지 URL
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean profileCompleted = false;
    
    @Column
    private LocalDateTime lastLoginAt;
    
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalDonatedAmount = BigDecimal.ZERO;
    
    @Column
    @Builder.Default
    private Long totalDonationCount = 0L;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<VerificationSession> verificationSessions;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Campaign> campaigns;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Donation> donations;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile userProfile;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserSettings userSettings;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserFavorite> userFavorites;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserWallet> userWallets;
    
    // UserDetails 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return enabled;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    // === 마이페이지 관련 헬퍼 메서드 ===
    
    /**
     * 프로필 완성도 계산
     * @return 완성도 (0.0 ~ 1.0)
     */
    public double calculateProfileCompleteness() {
        int totalFields = 4; // nickname, email, profileImage, phoneNumber
        int completedFields = 0;
        
        if (nickname != null && !nickname.trim().isEmpty()) completedFields++;
        if (email != null && !email.trim().isEmpty()) completedFields++;
        if (profileImage != null && !profileImage.trim().isEmpty()) completedFields++;
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) completedFields++;
        
        return (double) completedFields / totalFields;
    }
    
    /**
     * 프로필 완성도 업데이트
     */
    public void updateProfileCompleteness() {
        this.profileCompleted = calculateProfileCompleteness() >= 0.75; // 75% 이상 완성시 true
    }
    
    /**
     * 기부 통계 업데이트
     * @param amount 기부 금액
     */
    public void addDonation(BigDecimal amount) {
        this.totalDonatedAmount = this.totalDonatedAmount.add(amount);
        this.totalDonationCount++;
    }
    
    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }
    
    public enum Role {
        USER("일반 사용자"),
        ADMIN("시스템 관리자"),
        SUPER_ADMIN("슈퍼 관리자"),
        CAMPAIGN_ADMIN("캠페인 관리자");
        
        private final String displayName;
        
        Role(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isSuperAdmin() {
            return this == SUPER_ADMIN;
        }
        
        public boolean isCampaignAdmin() {
            return this == CAMPAIGN_ADMIN;
        }
        
        public boolean isSystemLevelAdmin() {
            return this == SUPER_ADMIN || this == ADMIN || this == CAMPAIGN_ADMIN;
        }
        
        public boolean canManageAnyOrganization() {
            return this == SUPER_ADMIN || this == CAMPAIGN_ADMIN;
        }
        
        public boolean canAccessAuditLogs() {
            return this == SUPER_ADMIN || this == ADMIN;
        }
    }
}
