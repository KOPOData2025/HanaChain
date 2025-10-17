package com.hanachain.hanachainbackend.security;

import com.hanachain.hanachainbackend.entity.enums.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required permissions for accessing a method or class
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * Required permissions. User must have at least one of these permissions.
     */
    Permission[] value();
    
    /**
     * Organization ID parameter name for organization-specific permissions
     * If specified, the permission check will be performed within the context of the organization
     */
    String organizationParam() default "";
    
    /**
     * Whether to require ALL permissions (true) or ANY permission (false)
     */
    boolean requireAll() default false;
    
    /**
     * Custom error message when access is denied
     */
    String message() default "접근 권한이 없습니다.";
}