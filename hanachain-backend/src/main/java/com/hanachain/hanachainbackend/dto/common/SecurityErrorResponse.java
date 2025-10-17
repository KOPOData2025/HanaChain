package com.hanachain.hanachainbackend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enhanced error response for security-related exceptions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityErrorResponse {
    
    private String error;
    private String message;
    private String errorCode;
    private LocalDateTime timestamp;
    private String path;
    private String method;
    private SecurityErrorDetails details;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SecurityErrorDetails {
        private String requiredPermission;
        private List<String> requiredPermissions;
        private String currentUserRole;
        private String requiredRole;
        private Long organizationId;
        private String organizationRole;
        private String actionAttempted;
        private String suggestion;
        private String contactInfo;
    }
    
    public static SecurityErrorResponse createAccessDenied(String message, String path, String method) {
        return SecurityErrorResponse.builder()
                .error("ACCESS_DENIED")
                .message(message)
                .errorCode("SEC-001")
                .timestamp(LocalDateTime.now())
                .path(path)
                .method(method)
                .build();
    }
    
    public static SecurityErrorResponse createInsufficientPermission(
            String message, 
            String requiredPermission, 
            String currentUserRole,
            String path, 
            String method) {
        return SecurityErrorResponse.builder()
                .error("INSUFFICIENT_PERMISSION")
                .message(message)
                .errorCode("SEC-002")
                .timestamp(LocalDateTime.now())
                .path(path)
                .method(method)
                .details(SecurityErrorDetails.builder()
                        .requiredPermission(requiredPermission)
                        .currentUserRole(currentUserRole)
                        .suggestion("필요한 권한을 얻기 위해 관리자에게 문의하세요.")
                        .contactInfo("시스템 관리자에게 문의하시기 바랍니다.")
                        .build())
                .build();
    }
    
    public static SecurityErrorResponse createOrganizationAccessDenied(
            String message,
            Long organizationId,
            String actionAttempted,
            String path,
            String method) {
        return SecurityErrorResponse.builder()
                .error("ORGANIZATION_ACCESS_DENIED")
                .message(message)
                .errorCode("SEC-003")
                .timestamp(LocalDateTime.now())
                .path(path)
                .method(method)
                .details(SecurityErrorDetails.builder()
                        .organizationId(organizationId)
                        .actionAttempted(actionAttempted)
                        .suggestion("해당 단체의 멤버이거나 관리자 권한이 필요합니다.")
                        .contactInfo("단체 관리자에게 멤버 추가를 요청하세요.")
                        .build())
                .build();
    }
    
    public static SecurityErrorResponse createRoleElevationDenied(
            String message,
            String currentUserRole,
            String requiredRole,
            String path,
            String method) {
        return SecurityErrorResponse.builder()
                .error("ROLE_ELEVATION_DENIED")
                .message(message)
                .errorCode("SEC-004")
                .timestamp(LocalDateTime.now())
                .path(path)
                .method(method)
                .details(SecurityErrorDetails.builder()
                        .currentUserRole(currentUserRole)
                        .requiredRole(requiredRole)
                        .suggestion("상위 권한이 필요한 작업입니다.")
                        .contactInfo("시스템 관리자에게 권한 상승을 요청하세요.")
                        .build())
                .build();
    }
    
    public static SecurityErrorResponse createAuthenticationRequired(String path, String method) {
        return SecurityErrorResponse.builder()
                .error("AUTHENTICATION_REQUIRED")
                .message("인증이 필요합니다.")
                .errorCode("SEC-000")
                .timestamp(LocalDateTime.now())
                .path(path)
                .method(method)
                .details(SecurityErrorDetails.builder()
                        .suggestion("로그인을 하신 후 다시 시도해주세요.")
                        .contactInfo("계정이 없으시면 회원가입을 진행해주세요.")
                        .build())
                .build();
    }
}