package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.dto.user.ProfileImageUploadResponse;
import com.hanachain.hanachainbackend.dto.user.ProfileResponse;
import com.hanachain.hanachainbackend.dto.user.ProfileUpdateRequest;
import com.hanachain.hanachainbackend.exception.ProfileNotFoundException;
import com.hanachain.hanachainbackend.exception.ProfileUpdateException;
import com.hanachain.hanachainbackend.service.ProfileService;
import com.hanachain.hanachainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * 프로필 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;

    @Value("${app.upload.profile-images:uploads/profile-images}")
    private String profileImageUploadPath;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public ProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return ProfileResponse.from(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        log.info("프로필 업데이트 시작 - 사용자 ID: {}", userId);
        
        try {
            User user = findUserById(userId);
            
            // 닉네임 업데이트
            if (StringUtils.hasText(request.getNickname())) {
                user.setNickname(request.getNickname());
                log.debug("닉네임 업데이트: {}", request.getNickname());
            }
            
            // 프로필 완성도 업데이트
            user.updateProfileCompleteness();
            
            User updatedUser = userRepository.save(user);
            log.info("프로필 업데이트 완료 - 사용자 ID: {}, 완성도: {}", userId, updatedUser.getProfileCompleted());
            
            return ProfileResponse.from(updatedUser);
            
        } catch (Exception e) {
            log.error("프로필 업데이트 실패 - 사용자 ID: {}", userId, e);
            throw new ProfileUpdateException("프로필 업데이트에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ProfileImageUploadResponse uploadProfileImage(Long userId, MultipartFile image) {
        log.info("프로필 이미지 업로드 시작 - 사용자 ID: {}, 파일명: {}", userId, image.getOriginalFilename());
        
        try {
            // 파일 검증
            validateImageFile(image);
            
            // 기존 이미지 삭제
            deleteExistingProfileImage(userId);
            
            // 새 파일 저장
            String fileName = generateUniqueFileName(image.getOriginalFilename());
            Path uploadPath = Paths.get(profileImageUploadPath);
            Files.createDirectories(uploadPath);
            
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 사용자 프로필 이미지 URL 업데이트
            String imageUrl = baseUrl + "/api/files/profile-images/" + fileName;
            User user = findUserById(userId);
            user.setProfileImage(imageUrl);
            user.updateProfileCompleteness();
            userRepository.save(user);
            
            log.info("프로필 이미지 업로드 완료 - 사용자 ID: {}, 이미지 URL: {}", userId, imageUrl);
            
            return ProfileImageUploadResponse.success(imageUrl, image.getOriginalFilename(), image.getSize());
            
        } catch (IOException e) {
            log.error("프로필 이미지 업로드 실패 - 사용자 ID: {}", userId, e);
            throw new ProfileUpdateException("이미지 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteProfileImage(Long userId) {
        log.info("프로필 이미지 삭제 시작 - 사용자 ID: {}", userId);
        
        try {
            User user = findUserById(userId);
            
            // 기존 이미지 파일 삭제
            deleteExistingProfileImage(userId);
            
            // 사용자 프로필 이미지 URL 제거
            user.setProfileImage(null);
            user.updateProfileCompleteness();
            userRepository.save(user);
            
            log.info("프로필 이미지 삭제 완료 - 사용자 ID: {}", userId);
            
        } catch (Exception e) {
            log.error("프로필 이미지 삭제 실패 - 사용자 ID: {}", userId, e);
            throw new ProfileUpdateException("프로필 이미지 삭제에 실패했습니다: " + e.getMessage());
        }
    }

    @Override
    public double calculateProfileCompleteness(Long userId) {
        User user = findUserById(userId);
        return user.calculateProfileCompleteness();
    }

    @Override
    @Transactional
    public void updateProfileCompleteness(Long userId) {
        User user = findUserById(userId);
        user.updateProfileCompleteness();
        userRepository.save(user);
        log.debug("프로필 완성도 업데이트 완료 - 사용자 ID: {}, 완성도: {}", userId, user.getProfileCompleted());
    }

    /**
     * 사용자 ID로 사용자 조회
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 이미지 파일 검증
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ProfileUpdateException("이미지 파일이 비어있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ProfileUpdateException("이미지 파일만 업로드 가능합니다.");
        }

        // 파일 크기 제한 (5MB)
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new ProfileUpdateException("이미지 파일 크기는 5MB 이하여야 합니다.");
        }
    }

    /**
     * 고유한 파일명 생성
     */
    private String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (StringUtils.hasText(originalFilename)) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFilename.substring(dotIndex);
            }
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * 기존 프로필 이미지 파일 삭제
     */
    private void deleteExistingProfileImage(Long userId) {
        try {
            User user = findUserById(userId);
            String currentImageUrl = user.getProfileImage();
            
            if (StringUtils.hasText(currentImageUrl)) {
                // URL에서 파일명 추출
                String fileName = currentImageUrl.substring(currentImageUrl.lastIndexOf('/') + 1);
                Path filePath = Paths.get(profileImageUploadPath, fileName);
                
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.debug("기존 프로필 이미지 파일 삭제: {}", filePath);
                }
            }
        } catch (IOException e) {
            log.warn("기존 프로필 이미지 파일 삭제 실패", e);
            // 기존 파일 삭제 실패는 전체 작업을 중단시키지 않음
        }
    }
}