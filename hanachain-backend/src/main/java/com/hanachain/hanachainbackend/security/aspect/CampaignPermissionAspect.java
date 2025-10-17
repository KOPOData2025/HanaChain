package com.hanachain.hanachainbackend.security.aspect;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.security.CampaignManagerPermissionService;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import com.hanachain.hanachainbackend.security.annotation.RequiresCampaignManagement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignPermissionAspect {
    
    private final CampaignManagerPermissionService permissionService;
    
    @Before("@annotation(requiresCampaignManagement)")
    public void checkCampaignPermission(JoinPoint joinPoint, RequiresCampaignManagement requiresCampaignManagement) {
        try {
            // 현재 사용자 조회
            User currentUser = SecurityUtils.getCurrentUser()
                    .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다"));
            
            // 캠페인 ID 추출
            Long campaignId = extractCampaignId(joinPoint, requiresCampaignManagement.campaignIdParam());
            
            if (campaignId == null) {
                throw new IllegalArgumentException("캠페인 ID를 찾을 수 없습니다. 파라미터: " + requiresCampaignManagement.campaignIdParam());
            }
            
            log.debug("Checking campaign permission for user {} on campaign {}", currentUser.getId(), campaignId);
            
            // 권한 검증
            if (requiresCampaignManagement.creatorOnly()) {
                // 생성자만 허용
                if (!permissionService.isCampaignCreator(currentUser.getId(), campaignId)) {
                    throw new IllegalStateException(requiresCampaignManagement.message() + " (생성자 권한 필요)");
                }
            } else {
                // 관리 권한 검증 (생성자 + 담당자 + 시스템 관리자)
                if (!permissionService.hasManagementPermission(currentUser.getId(), campaignId)) {
                    throw new IllegalStateException(requiresCampaignManagement.message());
                }
            }
            
            log.debug("Campaign permission check passed for user {} on campaign {}", currentUser.getId(), campaignId);
            
        } catch (Exception e) {
            log.error("Campaign permission check failed: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * 메서드 파라미터에서 캠페인 ID 추출
     */
    private Long extractCampaignId(JoinPoint joinPoint, String parameterName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            
            // @PathVariable, @RequestParam 등의 어노테이션에서 이름 확인
            String actualParameterName = getParameterName(parameter);
            
            if (parameterName.equals(actualParameterName)) {
                Object value = args[i];
                if (value instanceof Long) {
                    return (Long) value;
                } else if (value instanceof String) {
                    try {
                        return Long.parseLong((String) value);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("캠페인 ID를 숫자로 변환할 수 없습니다: " + value);
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 파라미터의 실제 이름 조회
     */
    private String getParameterName(Parameter parameter) {
        // @PathVariable 어노테이션 확인
        org.springframework.web.bind.annotation.PathVariable pathVariable = 
                parameter.getAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
        if (pathVariable != null && !pathVariable.value().isEmpty()) {
            return pathVariable.value();
        }
        if (pathVariable != null && !pathVariable.name().isEmpty()) {
            return pathVariable.name();
        }
        
        // @RequestParam 어노테이션 확인
        org.springframework.web.bind.annotation.RequestParam requestParam = 
                parameter.getAnnotation(org.springframework.web.bind.annotation.RequestParam.class);
        if (requestParam != null && !requestParam.value().isEmpty()) {
            return requestParam.value();
        }
        if (requestParam != null && !requestParam.name().isEmpty()) {
            return requestParam.name();
        }
        
        // 파라미터 이름 자체 사용 (컴파일 시 -parameters 옵션 필요)
        return parameter.getName();
    }
}