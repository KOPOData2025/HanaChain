package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.dto.user.DashboardResponse;
import com.hanachain.hanachainbackend.service.MyPageService;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 마이페이지 대시보드 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
@Tag(name = "MyPage", description = "마이페이지 관리 API")
public class MyPageController {
    
    private final MyPageService myPageService;
    
    /**
     * 인증 상태 확인용 디버그 엔드포인트
     */
    @GetMapping("/debug/auth")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "인증 상태 디버그", description = "현재 사용자의 인증 상태를 확인합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugAuth() {
        Map<String, Object> authInfo = new HashMap<>();
        
        try {
            // SecurityContext 정보
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            authInfo.put("authenticated", auth != null && auth.isAuthenticated());
            authInfo.put("principal_type", auth != null ? auth.getPrincipal().getClass().getSimpleName() : "null");
            authInfo.put("authorities", auth != null ? auth.getAuthorities().toString() : "null");
            
            // 현재 사용자 정보
            Optional<User> currentUser = SecurityUtils.getCurrentUser();
            authInfo.put("current_user_found", currentUser.isPresent());
            if (currentUser.isPresent()) {
                User user = currentUser.get();
                authInfo.put("user_id", user.getId());
                authInfo.put("user_email", user.getEmail());
                authInfo.put("user_role", user.getRole());
            }
            
            // 현재 사용자 이메일 정보
            Optional<String> currentEmail = SecurityUtils.getCurrentUserEmail();
            authInfo.put("current_email_found", currentEmail.isPresent());
            authInfo.put("current_email", currentEmail.orElse("not found"));
            
            return ResponseEntity.ok(ApiResponse.success("인증 디버그 정보", authInfo));
            
        } catch (Exception e) {
            log.error("인증 디버그 중 오류 발생", e);
            authInfo.put("error", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("디버그 중 오류 발생", authInfo));
        }
    }
    
    /**
     * 대시보드 종합 정보 조회
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "대시보드 종합 정보 조회",
        description = "현재 로그인한 사용자의 마이페이지 대시보드 종합 정보를 조회합니다. 프로필, 기부 통계, 최근 기부 내역, 즐겨찾기 수 등을 포함합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대시보드 정보 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        log.info("📊 대시보드 API 요청 수신 - /api/mypage/dashboard");
        
        try {
            // SecurityContext 디버그 정보
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.debug("🔍 SecurityContext 상태: authenticated={}, principal={}, authorities={}", 
                    auth != null && auth.isAuthenticated(),
                    auth != null ? auth.getPrincipal().getClass().getSimpleName() : "null",
                    auth != null ? auth.getAuthorities() : "null");
            
            // 현재 인증된 사용자 조회
            User currentUser = SecurityUtils.getCurrentUser()
                    .orElseThrow(() -> {
                        log.warn("⚠️ 인증된 사용자 정보를 찾을 수 없습니다. SecurityContext: {}", 
                                SecurityUtils.getCurrentUserEmail().orElse("No user"));
                        return new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
                    });
            
            log.debug("대시보드 정보 조회 요청: userId={}, email={}", currentUser.getId(), currentUser.getEmail());
            
            // 대시보드 정보 조회
            DashboardResponse dashboard = myPageService.getDashboard(currentUser.getId());
            
            return ResponseEntity.ok(
                ApiResponse.success("대시보드 정보를 성공적으로 조회했습니다.", dashboard)
            );
            
        } catch (IllegalStateException e) {
            log.warn("대시보드 조회 실패 - 인증 오류: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다."));
        } catch (IllegalArgumentException e) {
            log.warn("대시보드 조회 실패 - 사용자 없음: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("대시보드 조회 중 서버 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
    }
    
    /**
     * 대시보드 요약 정보 조회
     */
    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "대시보드 요약 정보 조회",
        description = "현재 로그인한 사용자의 마이페이지 대시보드 요약 정보를 조회합니다. 빠른 응답을 위해 최소한의 정보만 포함합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대시보드 요약 정보 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardSummary() {
        try {
            // 현재 인증된 사용자 조회
            User currentUser = SecurityUtils.getCurrentUser()
                    .orElseThrow(() -> new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다."));
            
            log.debug("대시보드 요약 정보 조회 요청: userId={}", currentUser.getId());
            
            // 대시보드 요약 정보 조회
            DashboardResponse summary = myPageService.getDashboardSummary(currentUser.getId());
            
            return ResponseEntity.ok(
                ApiResponse.success("대시보드 요약 정보를 성공적으로 조회했습니다.", summary)
            );
            
        } catch (IllegalStateException e) {
            log.warn("대시보드 요약 조회 실패 - 인증 오류: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증이 필요합니다."));
        } catch (IllegalArgumentException e) {
            log.warn("대시보드 요약 조회 실패 - 사용자 없음: {}", e.getMessage());
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("대시보드 요약 조회 중 서버 오류 발생", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
        }
    }
}