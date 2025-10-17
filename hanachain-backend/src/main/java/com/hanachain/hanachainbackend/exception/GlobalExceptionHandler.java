package com.hanachain.hanachainbackend.exception;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.common.SecurityErrorResponse;
import com.hanachain.hanachainbackend.security.exceptions.InsufficientPermissionException;
import com.hanachain.hanachainbackend.security.exceptions.OrganizationAccessDeniedException;
import com.hanachain.hanachainbackend.security.exceptions.RoleElevationDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    private String getCurrentUserRole() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getAuthorities() != null) {
                return authentication.getAuthorities().stream()
                        .findFirst()
                        .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                        .orElse("UNKNOWN");
            }
            return "ANONYMOUS";
        } catch (Exception e) {
            log.warn("Failed to extract user role from security context", e);
            return "UNKNOWN";
        }
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        
        log.error("❌ 입력값 검증 실패 상세 정보:");
        log.error("  - 객체명: {}", ex.getBindingResult().getObjectName());
        log.error("  - 총 에러 수: {}", ex.getBindingResult().getErrorCount());
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            Object rejectedValue = ((FieldError) error).getRejectedValue();
            errors.put(fieldName, errorMessage);
            
            log.error("  - 필드: '{}', 메시지: '{}', 거부된 값: '{}'", 
                fieldName, errorMessage, rejectedValue);
        });
        
        // 글로벌 에러도 확인
        if (ex.getBindingResult().hasGlobalErrors()) {
            ex.getBindingResult().getGlobalErrors().forEach(globalError -> {
                log.error("  - 글로벌 에러: '{}', 메시지: '{}'", 
                    globalError.getObjectName(), globalError.getDefaultMessage());
            });
        }
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("입력값 검증에 실패했습니다.", errors));
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<SecurityErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.error("Authentication error: ", ex);
        
        SecurityErrorResponse errorResponse = SecurityErrorResponse.createAuthenticationRequired(
                request.getRequestURI(), 
                request.getMethod()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<SecurityErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        log.error("Bad credentials: ", ex);
        
        SecurityErrorResponse errorResponse = SecurityErrorResponse.createAuthenticationRequired(
                request.getRequestURI(), 
                request.getMethod()
        );
        errorResponse.setMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<SecurityErrorResponse> handleInsufficientPermissionException(
            InsufficientPermissionException ex, HttpServletRequest request) {
        log.error("Insufficient permission: ", ex);
        
        SecurityErrorResponse errorResponse = SecurityErrorResponse.createInsufficientPermission(
                ex.getMessage(),
                ex.getRequiredPermission() != null ? ex.getRequiredPermission().getDisplayName() : "UNKNOWN",
                getCurrentUserRole(),
                request.getRequestURI(),
                request.getMethod()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    @ExceptionHandler(OrganizationAccessDeniedException.class)
    public ResponseEntity<SecurityErrorResponse> handleOrganizationAccessDeniedException(
            OrganizationAccessDeniedException ex, HttpServletRequest request) {
        log.error("Organization access denied: ", ex);
        
        SecurityErrorResponse errorResponse = SecurityErrorResponse.createOrganizationAccessDenied(
                ex.getMessage(),
                ex.getOrganizationId(),
                ex.getActionAttempted(),
                request.getRequestURI(),
                request.getMethod()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    @ExceptionHandler(RoleElevationDeniedException.class)
    public ResponseEntity<SecurityErrorResponse> handleRoleElevationDeniedException(
            RoleElevationDeniedException ex, HttpServletRequest request) {
        log.error("Role elevation denied: ", ex);
        
        String currentRole = ex.getCurrentUserRole() != null ? ex.getCurrentUserRole().name() : "UNKNOWN";
        String requiredRole = ex.getTargetRole() != null ? ex.getTargetRole().name() : 
                              ex.getTargetOrgRole() != null ? ex.getTargetOrgRole().name() : "UNKNOWN";
        
        SecurityErrorResponse errorResponse = SecurityErrorResponse.createRoleElevationDenied(
                ex.getMessage(),
                currentRole,
                requiredRole,
                request.getRequestURI(),
                request.getMethod()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<SecurityErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.error("Access denied: ", ex);
        
        SecurityErrorResponse errorResponse = SecurityErrorResponse.createAccessDenied(
                "접근 권한이 없습니다.",
                request.getRequestURI(),
                request.getMethod()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    // =========================== 지갑 관련 예외 처리 ===========================
    
    @ExceptionHandler(WalletCreationException.class)
    public ResponseEntity<ApiResponse<Void>> handleWalletCreationException(WalletCreationException ex) {
        log.error("Wallet creation failed: ", ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(WalletDecryptionException.class)
    public ResponseEntity<ApiResponse<Void>> handleWalletDecryptionException(WalletDecryptionException ex) {
        log.error("Wallet decryption failed: ", ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(WalletValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleWalletValidationException(WalletValidationException ex) {
        log.error("Wallet validation failed: ", ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(DuplicateWalletException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateWalletException(DuplicateWalletException ex) {
        log.error("Duplicate wallet registration attempt: ", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        log.error("Unauthorized access attempt: ", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    // =========================== 블록체인 관련 예외 처리 ===========================
    
    @ExceptionHandler(BlockchainException.class)
    public ResponseEntity<ApiResponse<Void>> handleBlockchainException(BlockchainException ex) {
        log.error("Blockchain operation failed: ", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    // =========================== 일반 예외 처리 ===========================
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.error("Business logic error: ", ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
        log.error("Validation error: ", ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NotFoundException ex) {
        log.error("Resource not found: ", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbiddenException(ForbiddenException ex) {
        log.error("Forbidden access: ", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ApiResponse<Void>> handleInternalServerErrorException(InternalServerErrorException ex) {
        log.error("Internal server error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: ", ex);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버에서 오류가 발생했습니다."));
    }
}
