package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 사용자 설정 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsUpdateRequest {
    
    // === 알림 설정 ===
    @NotNull
    private Boolean emailNotifications;
    
    @NotNull
    private Boolean donationUpdateNotifications;
    
    @NotNull
    private Boolean campaignUpdateNotifications;
    
    @NotNull
    private Boolean marketingNotifications;
    
    // === 개인정보 공개 설정 ===
    @NotNull
    private Boolean showProfilePublicly;
    
    @NotNull
    private Boolean showDonationHistory;
    
    @NotNull
    private Boolean showDonationAmount;
    
    // === 언어 및 지역 설정 ===
    private String language;
    private String timezone;
}