package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 설정 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsResponse {
    
    // === 알림 설정 ===
    private Boolean emailNotifications;
    private Boolean donationUpdateNotifications;
    private Boolean campaignUpdateNotifications;
    private Boolean marketingNotifications;
    
    // === 개인정보 공개 설정 ===
    private Boolean showProfilePublicly;
    private Boolean showDonationHistory;
    private Boolean showDonationAmount;
    
    // === 언어 및 지역 설정 ===
    private String language;
    private String timezone;
    
    /**
     * UserSettings 엔티티로부터 DTO 생성
     */
    public static UserSettingsResponse from(com.hanachain.hanachainbackend.entity.UserSettings userSettings) {
        if (userSettings == null) {
            // 기본 설정값 반환
            return UserSettingsResponse.builder()
                    .emailNotifications(true)
                    .donationUpdateNotifications(true)
                    .campaignUpdateNotifications(true)
                    .marketingNotifications(false)
                    .showProfilePublicly(true)
                    .showDonationHistory(false)
                    .showDonationAmount(false)
                    .language("ko")
                    .timezone("Asia/Seoul")
                    .build();
        }
        
        return UserSettingsResponse.builder()
                .emailNotifications(userSettings.getEmailNotifications())
                .donationUpdateNotifications(userSettings.getDonationUpdateNotifications())
                .campaignUpdateNotifications(userSettings.getCampaignUpdateNotifications())
                .marketingNotifications(userSettings.getMarketingNotifications())
                .showProfilePublicly(userSettings.getShowProfilePublicly())
                .showDonationHistory(userSettings.getShowDonationHistory())
                .showDonationAmount(userSettings.getShowDonationAmount())
                .language(userSettings.getLanguage())
                .timezone(userSettings.getTimezone())
                .build();
    }
}