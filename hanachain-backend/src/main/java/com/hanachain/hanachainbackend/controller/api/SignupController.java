package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.auth.RegisterRequest;
import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.service.SignupService;
import com.hanachain.hanachainbackend.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/signup")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Signup", description = "단계별 회원가입 API")
public class SignupController {
    
    private final SignupService signupService;
    private final VerificationService verificationService;
    
    @PostMapping("/terms")
    @Operation(summary = "약관 동의 단계", description = "약관 동의하고 회원가입 세션을 시작합니다.")
    public ResponseEntity<ApiResponse<Map<String, String>>> acceptTerms(
            @Valid @RequestBody TermsRequestDto request) {
        
        try {
            String sessionId = signupService.createSignupSession(
                    request.getTermsAccepted(),
                    request.getPrivacyAccepted(),
                    request.getMarketingAccepted()
            );
            
            Map<String, String> response = Map.of("sessionId", sessionId);
            return ResponseEntity.ok(ApiResponse.success("약관 동의가 완료되었습니다.", response));
            
        } catch (Exception e) {
            log.error("Terms acceptance failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("약관 동의 처리에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/account")
    @Operation(summary = "계정 정보 단계", description = "이메일과 비밀번호를 저장합니다.")
    public ResponseEntity<ApiResponse<Void>> saveAccount(
            @Valid @RequestBody AccountRequestDto request) {
        
        try {
            signupService.saveAccountInfo(request.getSessionId(), request.getEmail(), request.getPassword());
            return ResponseEntity.ok(ApiResponse.success("계정 정보가 저장되었습니다."));
            
        } catch (Exception e) {
            log.error("Account info save failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("계정 정보 저장에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/verify-email")
    @Operation(summary = "이메일 인증 완료", description = "이메일 인증이 완료되었음을 세션에 기록합니다.")
    public ResponseEntity<ApiResponse<Void>> markEmailVerified(
            @Valid @RequestBody VerifyEmailRequestDto request) {
        
        try {
            signupService.markEmailVerified(request.getSessionId(), request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다."));
            
        } catch (Exception e) {
            log.error("Email verification marking failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("이메일 인증 처리에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/complete")
    @Operation(summary = "회원가입 완료", description = "닉네임을 설정하고 회원가입을 완료합니다. 지갑은 자동 생성됩니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeSignup(
            @Valid @RequestBody CompleteRequestDto request) {

        try {
            Long userId = signupService.completeSignup(
                    request.getSessionId(),
                    request.getNickname(),
                    request.getPhoneNumber()
            );

            Map<String, Object> response = Map.of(
                    "userId", userId,
                    "success", true
            );

            return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", response));

        } catch (Exception e) {
            log.error("Signup completion failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("회원가입 완료에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "세션 정보 조회", description = "회원가입 세션의 현재 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSession(
            @PathVariable String sessionId) {
        
        try {
            var session = signupService.getSession(sessionId);
            
            Map<String, Object> response = Map.of(
                    "email", session.getEmail() != null ? session.getEmail() : "",
                    "currentStep", session.getCurrentStep(),
                    "termsAccepted", session.getTermsAccepted(),
                    "privacyAccepted", session.getPrivacyAccepted(),
                    "marketingAccepted", session.getMarketingAccepted(),
                    "emailVerified", session.getEmailVerified(),
                    "hasNickname", session.getNickname() != null
            );
            
            return ResponseEntity.ok(ApiResponse.success("세션 정보를 조회했습니다.", response));
            
        } catch (Exception e) {
            log.error("Session lookup failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("세션 조회에 실패했습니다: " + e.getMessage()));
        }
    }
    
    // DTO 클래스들
    @Data
    public static class TermsRequestDto {
        private Boolean termsAccepted = false;
        private Boolean privacyAccepted = false;  
        private Boolean marketingAccepted = false;
    }
    
    @Data
    public static class AccountRequestDto {
        @NotBlank(message = "세션 ID는 필수입니다.")
        private String sessionId;
        
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;
        
        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
    }
    
    @Data
    public static class VerifyEmailRequestDto {
        @NotBlank(message = "세션 ID는 필수입니다.")
        private String sessionId;
        
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;
    }
    
    @Data
    public static class CompleteRequestDto {
        @NotBlank(message = "세션 ID는 필수입니다.")
        private String sessionId;

        @NotBlank(message = "닉네임은 필수입니다.")
        private String nickname;

        private String phoneNumber;
    }
}