package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.user.ProfileImageUploadResponse;
import com.hanachain.hanachainbackend.dto.user.ProfileResponse;
import com.hanachain.hanachainbackend.dto.user.ProfileUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * 프로필 관리 서비스 인터페이스
 */
public interface ProfileService {
    
    /**
     * 사용자 프로필 정보 조회
     * @param userId 사용자 ID
     * @return 프로필 응답 DTO
     */
    ProfileResponse getProfile(Long userId);
    
    /**
     * 프로필 정보 수정
     * @param userId 사용자 ID
     * @param request 프로필 수정 요청 DTO
     * @return 수정된 프로필 응답 DTO
     */
    ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request);
    
    /**
     * 프로필 이미지 업로드
     * @param userId 사용자 ID
     * @param image 이미지 파일
     * @return 프로필 이미지 업로드 응답 DTO
     */
    ProfileImageUploadResponse uploadProfileImage(Long userId, MultipartFile image);
    
    /**
     * 프로필 이미지 삭제
     * @param userId 사용자 ID
     */
    void deleteProfileImage(Long userId);
    
    /**
     * 프로필 완성도 계산
     * @param userId 사용자 ID
     * @return 완성도 (0.0 ~ 1.0)
     */
    double calculateProfileCompleteness(Long userId);
    
    /**
     * 프로필 완성도 업데이트 (자동 완성도 계산 후 저장)
     * @param userId 사용자 ID
     */
    void updateProfileCompleteness(Long userId);
}