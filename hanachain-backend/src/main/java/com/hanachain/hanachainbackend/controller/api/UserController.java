package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.user.UserProfileResponse;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import com.hanachain.hanachainbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "사용자 관리 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/profile")
    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필을 조회합니다.")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {

        log.info("내 프로필 조회 요청");

        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileResponse profile = userService.getUserProfile(currentUserId);

        return ResponseEntity.ok(ApiResponse.success(profile));
    }
    
    @PutMapping("/profile")
    @Operation(summary = "내 프로필 수정", description = "현재 로그인한 사용자의 프로필을 수정합니다.")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @RequestBody UserProfileResponse request) {

        log.info("내 프로필 수정 요청");

        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserProfileResponse updatedProfile = userService.updateUserProfile(currentUserId, request);

        log.info("프로필이 성공적으로 수정되었습니다 - 사용자 ID: {}", currentUserId);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다.", updatedProfile));
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "사용자 프로필 조회", description = "특정 사용자의 프로필을 조회합니다. (관리자 또는 본인만 가능)")
    @PreAuthorize("hasRole('ADMIN') or @securityUtils.isCurrentUser(#userId)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable Long userId) {

        log.info("사용자 프로필 조회 요청 - 사용자 ID: {}", userId);

        UserProfileResponse profile = userService.getUserProfile(userId);

        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/list")
    @Operation(summary = "일반 유저 목록 조회", description = "캠페인 담당자 등록을 위한 일반 유저 목록을 조회합니다. (관리자만 가능)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('CAMPAIGN_ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserProfileResponse>>> getUserList(
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("일반 유저 목록 조회 요청 - 키워드: {}", keyword);

        Page<UserProfileResponse> users = userService.getUserList(keyword, pageable);

        return ResponseEntity.ok(ApiResponse.success("일반 유저 목록 조회 성공", users));
    }
}
