package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.user.UserSettingsResponse;
import com.hanachain.hanachainbackend.dto.user.UserSettingsUpdateRequest;

/**
 * 사용자 설정 관리 서비스 인터페이스
 */
public interface UserSettingsService {
    
    /**
     * 사용자 설정 조회
     * @param userId 사용자 ID
     * @return 사용자 설정
     */
    UserSettingsResponse getUserSettings(Long userId);
    
    /**
     * 사용자 설정 수정
     * @param userId 사용자 ID
     * @param request 설정 수정 요청
     * @return 수정된 사용자 설정
     */
    UserSettingsResponse updateUserSettings(Long userId, UserSettingsUpdateRequest request);
    
    /**
     * 사용자 설정 초기화 (기본값으로 설정)
     * @param userId 사용자 ID
     * @return 초기화된 사용자 설정
     */
    UserSettingsResponse resetUserSettings(Long userId);
}