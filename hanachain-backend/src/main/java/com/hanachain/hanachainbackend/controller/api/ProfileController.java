package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.user.ProfileImageUploadResponse;
import com.hanachain.hanachainbackend.dto.user.ProfileResponse;
import com.hanachain.hanachainbackend.dto.user.ProfileUpdateRequest;
import com.hanachain.hanachainbackend.exception.ProfileNotFoundException;
import com.hanachain.hanachainbackend.service.ProfileService;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 프로필 관리 REST API 컨트롤러
 */
@Tag(name = "Profile Management", description = "사용자 프로필 관리 API")
@Slf4j
@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(
        summary = "프로필 정보 조회",
        description = "현재 사용자의 프로필 정보를 조회합니다."
    )
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        try {
            Long currentUserId = SecurityUtils.getCurrentUser()
                    .map(user -> {
                        log.debug("인증된 사용자 발견: ID={}, Email={}", user.getId(), user.getEmail());
                        return user.getId();
                    })
                    .orElseThrow(() -> {
                        log.warn("인증된 사용자를 찾을 수 없습니다. SecurityContext: {}", 
                                SecurityUtils.getCurrentUserEmail().orElse("No user"));
                        return new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
                    });
            
            log.debug("프로필 조회 요청 - 사용자 ID: {}", currentUserId);
            
            ProfileResponse profile = profileService.getProfile(currentUserId);
            
            return ResponseEntity.ok(
                ApiResponse.success("프로필 정보를 성공적으로 조회했습니다.", profile)
            );
            
        } catch (IllegalStateException e) {
            log.warn("프로필 조회 실패 - 인증 오류: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다."));
        } catch (ProfileNotFoundException e) {
            log.warn("프로필 조회 실패 - 사용자 없음: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("프로필 조회 중 서버 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
    }

    @Operation(
        summary = "프로필 정보 수정",
        description = "현재 사용자의 프로필 정보를 수정합니다."
    )
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUser()
                    .map(user -> user.getId())
                    .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."));
            
            log.info("프로필 수정 요청 - 사용자 ID: {}", currentUserId);
            
            ProfileResponse updatedProfile = profileService.updateProfile(currentUserId, request);
            
            return ResponseEntity.ok(
                ApiResponse.success("프로필이 성공적으로 수정되었습니다.", updatedProfile)
            );
            
        } catch (IllegalStateException e) {
            log.warn("프로필 수정 실패 - 인증 오류: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다."));
        } catch (Exception e) {
            log.error("프로필 수정 중 서버 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
    }

    @Operation(
        summary = "프로필 이미지 업로드",
        description = "프로필 이미지를 업로드합니다. 지원 형식: JPG, PNG, GIF (최대 5MB)"
    )
    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ProfileImageUploadResponse>> uploadProfileImage(
            @Parameter(description = "업로드할 이미지 파일") @RequestParam("image") MultipartFile image) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUser()
                    .map(user -> user.getId())
                    .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."));
            
            log.info("프로필 이미지 업로드 요청 - 사용자 ID: {}, 파일명: {}", currentUserId, image.getOriginalFilename());
            
            ProfileImageUploadResponse uploadResponse = profileService.uploadProfileImage(currentUserId, image);
            
            return ResponseEntity.ok(
                ApiResponse.success("프로필 이미지가 성공적으로 업로드되었습니다.", uploadResponse)
            );
            
        } catch (IllegalStateException e) {
            log.warn("프로필 이미지 업로드 실패 - 인증 오류: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다."));
        } catch (Exception e) {
            log.error("프로필 이미지 업로드 중 서버 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
    }

    @Operation(
        summary = "프로필 이미지 삭제",
        description = "현재 설정된 프로필 이미지를 삭제합니다."
    )
    @DeleteMapping("/profile/image")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> deleteProfileImage() {
        try {
            Long currentUserId = SecurityUtils.getCurrentUser()
                    .map(user -> user.getId())
                    .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."));
            
            log.info("프로필 이미지 삭제 요청 - 사용자 ID: {}", currentUserId);
            
            profileService.deleteProfileImage(currentUserId);
            
            return ResponseEntity.ok(
                ApiResponse.success("프로필 이미지가 성공적으로 삭제되었습니다.")
            );
            
        } catch (IllegalStateException e) {
            log.warn("프로필 이미지 삭제 실패 - 인증 오류: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다."));
        } catch (Exception e) {
            log.error("프로필 이미지 삭제 중 서버 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
    }

    @Operation(
        summary = "프로필 완성도 조회",
        description = "현재 사용자의 프로필 완성도를 조회합니다. (0.0 ~ 1.0)"
    )
    @GetMapping("/profile/completeness")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Double>> getProfileCompleteness() {
        try {
            Long currentUserId = SecurityUtils.getCurrentUser()
                    .map(user -> user.getId())
                    .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."));
            
            log.debug("프로필 완성도 조회 요청 - 사용자 ID: {}", currentUserId);
            
            double completeness = profileService.calculateProfileCompleteness(currentUserId);
            
            return ResponseEntity.ok(
                ApiResponse.success("프로필 완성도를 성공적으로 조회했습니다.", completeness)
            );
            
        } catch (IllegalStateException e) {
            log.warn("프로필 완성도 조회 실패 - 인증 오류: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다."));
        } catch (Exception e) {
            log.error("프로필 완성도 조회 중 서버 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
    }

    @Operation(
        summary = "프로필 완성도 업데이트",
        description = "프로필 완성도를 다시 계산하여 업데이트합니다."
    )
    @PostMapping("/profile/completeness/refresh")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> refreshProfileCompleteness() {
        try {
            Long currentUserId = SecurityUtils.getCurrentUser()
                    .map(user -> user.getId())
                    .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다."));
            
            log.info("프로필 완성도 업데이트 요청 - 사용자 ID: {}", currentUserId);
            
            profileService.updateProfileCompleteness(currentUserId);
            
            return ResponseEntity.ok(
                ApiResponse.success("프로필 완성도가 성공적으로 업데이트되었습니다.")
            );
            
        } catch (IllegalStateException e) {
            log.warn("프로필 완성도 업데이트 실패 - 인증 오류: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다."));
        } catch (Exception e) {
            log.error("프로필 완성도 업데이트 중 서버 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
    }
}