package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.auth.AuthEmailCheckRequest;
import com.hanachain.hanachainbackend.dto.auth.LoginRequest;
import com.hanachain.hanachainbackend.dto.auth.AuthNicknameCheckRequest;
import com.hanachain.hanachainbackend.dto.auth.LoginResponse;
import com.hanachain.hanachainbackend.dto.auth.RegisterRequest;
import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.user.UserProfileResponse;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.service.UserService;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "사용자 인증 관련 API")
public class AuthController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "새 사용자를 등록합니다.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        
        try {
            User user = userService.registerUser(request);
            UserProfileResponse userProfile = UserProfileResponse.fromEntity(user);
            
            return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", userProfile));
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("회원가입에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인을 처리합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        
        try {
            LoginResponse response = userService.loginUser(request);
            
            return ResponseEntity.ok(ApiResponse.success("로그인이 완료되었습니다.", response));
        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("로그인에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새 액세스 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestBody Map<String, String> request) {
        
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("리프레시 토큰이 필요합니다."));
            }
            
            LoginResponse response = userService.refreshToken(refreshToken);
            
            return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", response));
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("토큰 갱신에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // JWT는 stateless이므로 서버에서 특별한 로그아웃 처리가 필요하지 않음
        // 클라이언트에서 토큰을 삭제하면 됨
        // 추후 토큰 블랙리스트 기능을 구현할 수 있음
        
        return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다."));
    }
    
    @PostMapping("/check-email")
    @Operation(summary = "이메일 중복 확인", description = "이메일 주소의 사용 가능 여부를 확인합니다.")
    public ResponseEntity<?> checkEmailAvailability(
            @Valid @RequestBody AuthEmailCheckRequest request) {
        
        try {
            boolean exists = userService.existsByEmail(request.getEmail());
            
            // 프론트엔드는 평면 구조를 기대함: { available: boolean, message?: string }
            Map<String, Object> response = Map.of(
                "available", !exists,
                "message", exists ? "이미 사용 중인 이메일입니다." : "사용 가능한 이메일입니다."
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Email check failed", e);
            // 동일한 평면 형식으로 오류 반환
            Map<String, Object> errorResponse = Map.of(
                "available", false,
                "message", "이메일 확인에 실패했습니다: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 확인", description = "닉네임의 사용 가능 여부를 확인합니다.")
    public ResponseEntity<?> checkNicknameAvailability(
            @Valid @RequestBody AuthNicknameCheckRequest request) {
        
        try {
            boolean exists = userService.existsByName(request.getNickname());
            
            // 프론트엔드는 평면 구조를 기대함: { available: boolean, message?: string }
            Map<String, Object> response = Map.of(
                "available", !exists,
                "message", exists ? "이미 사용 중인 닉네임입니다." : "사용 가능한 닉네임입니다."
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Nickname check failed", e);
            // 동일한 평면 형식으로 오류 반환
            Map<String, Object> errorResponse = Map.of(
                "available", false,
                "message", "닉네임 확인에 실패했습니다: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/validate")
    @Operation(summary = "토큰 유효성 검증", description = "현재 토큰의 유효성을 검증합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken() {
        
        try {
            log.debug("토큰 유효성 검증 요청 시작");
            
            // SecurityUtils를 통해 현재 인증된 사용자 확인
            User currentUser = SecurityUtils.getCurrentUser()
                    .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
            
            log.debug("토큰 검증 성공 - 사용자: {}", currentUser.getEmail());
            
            // 토큰이 유효하면 성공 응답 (추가 정보 포함)
            Map<String, Object> response = Map.of(
                "success", true,
                "userId", currentUser.getId(),
                "email", currentUser.getEmail(),
                "role", currentUser.getRole().name(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success("토큰이 유효합니다.", response));
            
        } catch (RuntimeException e) {
            // 인증 관련 예외는 경고 수준으로 로깅
            log.warn("토큰 검증 실패 - 인증 오류: {}", e.getMessage());
            
            Map<String, Object> response = Map.of(
                "success", false,
                "error", "AUTHENTICATION_FAILED",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("토큰 검증에 실패했습니다.", response));
                    
        } catch (Exception e) {
            // 시스템 오류는 에러 수준으로 로깅
            log.error("토큰 검증 중 시스템 오류 발생", e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "error", "SYSTEM_ERROR",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", response));
        }
    }
    
    @GetMapping("/profile")
    @Operation(summary = "현재 사용자 프로필 조회", description = "인증된 사용자의 프로필 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile() {
        
        try {
            Long currentUserId = SecurityUtils.getCurrentUser()
                    .map(user -> user.getId())
                    .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
            
            UserProfileResponse profile = userService.getUserProfile(currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success("프로필 정보를 성공적으로 조회했습니다.", profile));
        } catch (Exception e) {
            log.error("Failed to get current user profile", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("프로필 조회에 실패했습니다: " + e.getMessage()));
        }
    }
}
