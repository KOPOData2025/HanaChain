package com.hanachain.hanachainbackend.security.exceptions;

import com.hanachain.hanachainbackend.entity.enums.Permission;

/**
 * Exception thrown when user lacks specific permissions
 */
public class InsufficientPermissionException extends SecurityException {
    
    private final Permission requiredPermission;
    private final Long organizationId;
    
    public InsufficientPermissionException(Permission requiredPermission) {
        super("권한이 부족합니다: " + requiredPermission.getDisplayName());
        this.requiredPermission = requiredPermission;
        this.organizationId = null;
    }
    
    public InsufficientPermissionException(Permission requiredPermission, Long organizationId) {
        super("단체에 대한 권한이 부족합니다: " + requiredPermission.getDisplayName());
        this.requiredPermission = requiredPermission;
        this.organizationId = organizationId;
    }
    
    public InsufficientPermissionException(String message, Permission requiredPermission) {
        super(message);
        this.requiredPermission = requiredPermission;
        this.organizationId = null;
    }
    
    public InsufficientPermissionException(String message, Permission requiredPermission, Long organizationId) {
        super(message);
        this.requiredPermission = requiredPermission;
        this.organizationId = organizationId;
    }
    
    public Permission getRequiredPermission() {
        return requiredPermission;
    }
    
    public Long getOrganizationId() {
        return organizationId;
    }
}