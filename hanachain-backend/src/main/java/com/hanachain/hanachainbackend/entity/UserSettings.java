package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 설정 엔티티
 */
@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings extends BaseEntity {
    
    @Id
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;
    
    // === 알림 설정 ===
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailNotifications = true;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean donationUpdateNotifications = true;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean campaignUpdateNotifications = true;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean marketingNotifications = false;
    
    // === 개인정보 공개 설정 ===
    @Column(nullable = false)
    @Builder.Default
    private Boolean showProfilePublicly = true;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean showDonationHistory = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean showDonationAmount = false;
    
    // === 언어 및 지역 설정 ===
    @Column(length = 10)
    @Builder.Default
    private String language = "ko";
    
    @Column(length = 50)
    @Builder.Default
    private String timezone = "Asia/Seoul";
}