package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.user.UserSettingsResponse;
import com.hanachain.hanachainbackend.dto.user.UserSettingsUpdateRequest;
import com.hanachain.hanachainbackend.service.UserSettingsService;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 설정 관리 REST API 컨트롤러
 */
@Tag(name = "User Settings", description = "사용자 설정 관리 API")
@Slf4j
@RestController
@RequestMapping("/mypage/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    @Operation(
        summary = "사용자 설정 조회",
        description = "현재 사용자의 설정 정보를 조회합니다."
    )
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<UserSettingsResponse> getUserSettings() {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.debug("사용자 설정 조회 요청 - 사용자 ID: {}", currentUserId);
        
        UserSettingsResponse settings = userSettingsService.getUserSettings(currentUserId);
        
        return ApiResponse.success("사용자 설정을 성공적으로 조회했습니다.", settings);
    }

    @Operation(
        summary = "사용자 설정 수정",
        description = "사용자의 설정 정보를 수정합니다."
    )
    @PutMapping
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<UserSettingsResponse> updateUserSettings(
            @Valid @RequestBody UserSettingsUpdateRequest request) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.info("사용자 설정 수정 요청 - 사용자 ID: {}", currentUserId);
        
        UserSettingsResponse updatedSettings = userSettingsService.updateUserSettings(currentUserId, request);
        
        return ApiResponse.success("사용자 설정이 성공적으로 수정되었습니다.", updatedSettings);
    }

    @Operation(
        summary = "사용자 설정 초기화",
        description = "사용자 설정을 기본값으로 초기화합니다."
    )
    @PostMapping("/reset")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<UserSettingsResponse> resetUserSettings() {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.info("사용자 설정 초기화 요청 - 사용자 ID: {}", currentUserId);
        
        UserSettingsResponse resetSettings = userSettingsService.resetUserSettings(currentUserId);
        
        return ApiResponse.success("사용자 설정이 성공적으로 초기화되었습니다.", resetSettings);
    }
}