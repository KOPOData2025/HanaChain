package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.UserSettings;
import com.hanachain.hanachainbackend.dto.user.UserSettingsResponse;
import com.hanachain.hanachainbackend.dto.user.UserSettingsUpdateRequest;
import com.hanachain.hanachainbackend.exception.ProfileNotFoundException;
import com.hanachain.hanachainbackend.service.UserSettingsService;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 설정 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;

    @Override
    public UserSettingsResponse getUserSettings(Long userId) {
        log.debug("사용자 설정 조회 - 사용자 ID: {}", userId);
        
        validateUserExists(userId);
        
        UserSettings userSettings = userSettingsRepository.findByUserId(userId)
                .orElse(null);
        
        return UserSettingsResponse.from(userSettings);
    }

    @Override
    @Transactional
    public UserSettingsResponse updateUserSettings(Long userId, UserSettingsUpdateRequest request) {
        log.info("사용자 설정 업데이트 시작 - 사용자 ID: {}", userId);
        
        User user = findUserById(userId);
        
        // 기존 설정 조회 또는 새로 생성
        UserSettings userSettings = userSettingsRepository.findByUserId(userId)
                .orElse(createDefaultUserSettings(user));
        
        // 설정 업데이트
        updateSettingsFromRequest(userSettings, request);
        
        UserSettings savedSettings = userSettingsRepository.save(userSettings);
        
        log.info("사용자 설정 업데이트 완료 - 사용자 ID: {}", userId);
        
        return UserSettingsResponse.from(savedSettings);
    }

    @Override
    @Transactional
    public UserSettingsResponse resetUserSettings(Long userId) {
        log.info("사용자 설정 초기화 시작 - 사용자 ID: {}", userId);
        
        User user = findUserById(userId);
        
        // 기존 설정 삭제
        userSettingsRepository.findByUserId(userId)
                .ifPresent(userSettingsRepository::delete);
        
        // 기본 설정 생성 및 저장
        UserSettings defaultSettings = createDefaultUserSettings(user);
        UserSettings savedSettings = userSettingsRepository.save(defaultSettings);
        
        log.info("사용자 설정 초기화 완료 - 사용자 ID: {}", userId);
        
        return UserSettingsResponse.from(savedSettings);
    }

    /**
     * 사용자 ID로 사용자 조회
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 사용자 존재 여부 확인
     */
    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ProfileNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }
    }

    /**
     * 기본 사용자 설정 생성
     */
    private UserSettings createDefaultUserSettings(User user) {
        return UserSettings.builder()
                .user(user)
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

    /**
     * 요청으로부터 설정 업데이트
     */
    private void updateSettingsFromRequest(UserSettings userSettings, UserSettingsUpdateRequest request) {
        // 알림 설정
        if (request.getEmailNotifications() != null) {
            userSettings.setEmailNotifications(request.getEmailNotifications());
        }
        if (request.getDonationUpdateNotifications() != null) {
            userSettings.setDonationUpdateNotifications(request.getDonationUpdateNotifications());
        }
        if (request.getCampaignUpdateNotifications() != null) {
            userSettings.setCampaignUpdateNotifications(request.getCampaignUpdateNotifications());
        }
        if (request.getMarketingNotifications() != null) {
            userSettings.setMarketingNotifications(request.getMarketingNotifications());
        }
        
        // 공개 설정
        if (request.getShowProfilePublicly() != null) {
            userSettings.setShowProfilePublicly(request.getShowProfilePublicly());
        }
        if (request.getShowDonationHistory() != null) {
            userSettings.setShowDonationHistory(request.getShowDonationHistory());
        }
        if (request.getShowDonationAmount() != null) {
            userSettings.setShowDonationAmount(request.getShowDonationAmount());
        }
        
        // 언어/지역 설정
        if (request.getLanguage() != null) {
            userSettings.setLanguage(request.getLanguage());
        }
        if (request.getTimezone() != null) {
            userSettings.setTimezone(request.getTimezone());
        }
    }
}