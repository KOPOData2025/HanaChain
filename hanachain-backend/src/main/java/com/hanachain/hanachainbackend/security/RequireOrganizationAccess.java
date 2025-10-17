package com.hanachain.hanachainbackend.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify that the user must have access to a specific organization
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireOrganizationAccess {
    
    /**
     * Parameter name that contains the organization ID
     */
    String value();
    
    /**
     * Whether admin role is required (ORG_ADMIN)
     */
    boolean adminRequired() default false;
    
    /**
     * Custom error message when access is denied
     */
    String message() default "해당 단체에 대한 접근 권한이 없습니다.";
}