package com.hanachain.hanachainbackend.security.annotation;

import com.hanachain.hanachainbackend.entity.User;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify required roles for accessing a method
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    /**
     * Required roles to access the method
     */
    User.Role[] value();
}