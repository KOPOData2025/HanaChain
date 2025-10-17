package com.hanachain.hanachainbackend.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required permissions for accessing a method
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    /**
     * Required permissions to access the method
     */
    String[] value();
    
    /**
     * Optional organization ID for organization-specific permissions
     */
    String organizationId() default "";
}