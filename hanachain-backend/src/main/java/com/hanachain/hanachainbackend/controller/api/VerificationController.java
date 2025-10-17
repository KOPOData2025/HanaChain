package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.auth.VerificationRequest;
import com.hanachain.hanachainbackend.dto.auth.VerificationVerifyRequest;
import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/verification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Verification", description = "이메일 인증 관련 API")
public class VerificationController {
    
    private final VerificationService verificationService;
    
    @PostMapping("/send")
    @Operation(summary = "인증 코드 발송", description = "지정된 이메일로 인증 코드를 발송합니다.")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(
            @Valid @RequestBody VerificationRequest request) {

        log.info("인증 코드 발송 요청 - 이메일: {}, 타입: {}", request.getEmail(), request.getType());

        // 서비스 레이어의 예외를 GlobalExceptionHandler에서 일관되게 처리하도록 위임
        verificationService.createAndSendVerificationCode(request.getEmail(), request.getType());

        log.info("인증 코드가 성공적으로 발송되었습니다: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 이메일로 발송되었습니다."));
    }
    
    @PostMapping("/verify")
    @Operation(summary = "인증 코드 확인", description = "이메일로 받은 인증 코드를 확인합니다.")
    public ResponseEntity<ApiResponse<Void>> verifyCode(
            @Valid @RequestBody VerificationVerifyRequest request) {

        log.info("인증 코드 확인 요청 - 이메일: {}, 타입: {}", request.getEmail(), request.getType());

        boolean isVerified = verificationService.verifyCode(
                request.getEmail(),
                request.getCode(),
                request.getType()
        );

        if (isVerified) {
            log.info("인증이 성공적으로 완료되었습니다: {}", request.getEmail());
            return ResponseEntity.ok(ApiResponse.success("인증이 성공적으로 완료되었습니다."));
        } else {
            log.warn("인증 실패 - 이메일: {}, 이유: 코드 불일치 또는 만료", request.getEmail());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("인증 코드가 올바르지 않거나 만료되었습니다."));
        }
    }
    
    @PostMapping("/resend")
    @Operation(summary = "인증 코드 재발송", description = "인증 코드를 다시 발송합니다.")
    public ResponseEntity<ApiResponse<Void>> resendVerificationCode(
            @Valid @RequestBody VerificationRequest request) {

        log.info("인증 코드 재발송 요청 - 이메일: {}, 타입: {}", request.getEmail(), request.getType());

        // 서비스 레이어의 예외를 GlobalExceptionHandler에서 일관되게 처리하도록 위임
        verificationService.resendVerificationCode(request.getEmail(), request.getType());

        log.info("인증 코드가 재발송되었습니다: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 재발송되었습니다."));
    }
}
