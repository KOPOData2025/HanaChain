package com.hanachain.hanachainbackend.security;

import com.hanachain.hanachainbackend.entity.enums.Permission;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

/**
 * 권한 기반 접근 제어를 강제하는 Aspect
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessControlAspect {

    private final SecurityUtils securityUtils;

    /**
     * @RequirePermission 어노테이션이 있는 메서드를 가로챕니다
     */
    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        log.debug("Checking permissions for method: {}", joinPoint.getSignature().getName());

        Permission[] requiredPermissions = requirePermission.value();
        String organizationParam = requirePermission.organizationParam();
        boolean requireAll = requirePermission.requireAll();

        // 지정된 경우 조직 ID 가져오기
        Long organizationId = null;
        if (!organizationParam.isEmpty()) {
            organizationId = extractOrganizationId(joinPoint, organizationParam);
        }
        
        boolean hasAccess = false;

        if (organizationId != null) {
            // 조직별 권한 확인
            final Long finalOrganizationId = organizationId;
            if (requireAll) {
                hasAccess = Arrays.stream(requiredPermissions)
                        .allMatch(permission -> securityUtils.hasPermissionInOrganization(finalOrganizationId, permission));
            } else {
                hasAccess = Arrays.stream(requiredPermissions)
                        .anyMatch(permission -> securityUtils.hasPermissionInOrganization(finalOrganizationId, permission));
            }
        } else {
            // 시스템 레벨 권한 확인
            if (requireAll) {
                hasAccess = Arrays.stream(requiredPermissions)
                        .allMatch(securityUtils::hasPermission);
            } else {
                hasAccess = Arrays.stream(requiredPermissions)
                        .anyMatch(securityUtils::hasPermission);
            }
        }
        
        if (!hasAccess) {
            String resource = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
            String action = Arrays.toString(requiredPermissions);
            securityUtils.logAccessAttempt(resource, action, false);
            
            throw new AccessDeniedException(requirePermission.message());
        }

        // 성공적인 접근 로깅
        String resource = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        String action = Arrays.toString(requiredPermissions);
        securityUtils.logAccessAttempt(resource, action, true);
    }

    /**
     * @RequireOrganizationAccess 어노테이션이 있는 메서드를 가로챕니다
     */
    @Before("@annotation(requireOrganizationAccess)")
    public void checkOrganizationAccess(JoinPoint joinPoint, RequireOrganizationAccess requireOrganizationAccess) {
        log.debug("Checking organization access for method: {}", joinPoint.getSignature().getName());

        String organizationParam = requireOrganizationAccess.value();
        boolean adminRequired = requireOrganizationAccess.adminRequired();

        Long organizationId = extractOrganizationId(joinPoint, organizationParam);
        if (organizationId == null) {
            throw new AccessDeniedException("단체 ID가 제공되지 않았습니다.");
        }

        boolean hasAccess;
        if (adminRequired) {
            // 사용자가 조직의 관리자인지 확인
            hasAccess = securityUtils.isOrganizationAdmin(organizationId) ||
                       securityUtils.isSuperAdmin() ||
                       securityUtils.isCampaignAdmin();
        } else {
            // 사용자가 조직에 대한 접근 권한을 가지고 있는지 확인
            hasAccess = securityUtils.canAccessOrganization(organizationId);
        }
        
        if (!hasAccess) {
            String resource = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
            String action = adminRequired ? "ADMIN_ACCESS" : "ACCESS";
            securityUtils.logAccessAttempt(resource, action, false);
            
            throw new AccessDeniedException(requireOrganizationAccess.message());
        }

        // 성공적인 접근 로깅
        String resource = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        String action = adminRequired ? "ADMIN_ACCESS" : "ACCESS";
        securityUtils.logAccessAttempt(resource, action, true);
    }

    /**
     * 메서드 파라미터에서 조직 ID를 추출합니다
     */
    private Long extractOrganizationId(JoinPoint joinPoint, String parameterName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(parameterName)) {
                Object value = args[i];
                if (value instanceof Long) {
                    return (Long) value;
                } else if (value instanceof Integer) {
                    return ((Integer) value).longValue();
                } else if (value instanceof String) {
                    try {
                        return Long.parseLong((String) value);
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse organization ID from string: {}", value);
                        return null;
                    }
                }
            }
        }

        // 경로 변수 또는 요청 파라미터에서 찾기 시도
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.isAnnotationPresent(org.springframework.web.bind.annotation.PathVariable.class)) {
                org.springframework.web.bind.annotation.PathVariable pathVar = 
                    param.getAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
                if (pathVar.value().equals(parameterName) || pathVar.name().equals(parameterName)) {
                    Object value = args[i];
                    if (value instanceof Long) {
                        return (Long) value;
                    } else if (value instanceof String) {
                        try {
                            return Long.parseLong((String) value);
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse organization ID from path variable: {}", value);
                            return null;
                        }
                    }
                }
            }
        }
        
        log.warn("Could not find organization ID parameter: {}", parameterName);
        return null;
    }
}