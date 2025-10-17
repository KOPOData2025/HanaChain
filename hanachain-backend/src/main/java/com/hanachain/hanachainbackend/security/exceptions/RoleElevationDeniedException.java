package com.hanachain.hanachainbackend.security.exceptions;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;

/**
 * Exception thrown when user attempts to assign or modify roles they don't have permission for
 */
public class RoleElevationDeniedException extends SecurityException {
    
    private final User.Role currentUserRole;
    private final User.Role targetRole;
    private final OrganizationRole targetOrgRole;
    private final Long organizationId;
    
    public RoleElevationDeniedException(User.Role currentUserRole, User.Role targetRole) {
        super("현재 권한으로는 해당 역할을 할당할 수 없습니다.");
        this.currentUserRole = currentUserRole;
        this.targetRole = targetRole;
        this.targetOrgRole = null;
        this.organizationId = null;
    }
    
    public RoleElevationDeniedException(User.Role currentUserRole, OrganizationRole targetOrgRole, Long organizationId) {
        super("현재 권한으로는 해당 단체 역할을 할당할 수 없습니다.");
        this.currentUserRole = currentUserRole;
        this.targetRole = null;
        this.targetOrgRole = targetOrgRole;
        this.organizationId = organizationId;
    }
    
    public RoleElevationDeniedException(String message, User.Role currentUserRole, User.Role targetRole) {
        super(message);
        this.currentUserRole = currentUserRole;
        this.targetRole = targetRole;
        this.targetOrgRole = null;
        this.organizationId = null;
    }
    
    public RoleElevationDeniedException(String message, User.Role currentUserRole, OrganizationRole targetOrgRole, Long organizationId) {
        super(message);
        this.currentUserRole = currentUserRole;
        this.targetRole = null;
        this.targetOrgRole = targetOrgRole;
        this.organizationId = organizationId;
    }
    
    public User.Role getCurrentUserRole() {
        return currentUserRole;
    }
    
    public User.Role getTargetRole() {
        return targetRole;
    }
    
    public OrganizationRole getTargetOrgRole() {
        return targetOrgRole;
    }
    
    public Long getOrganizationId() {
        return organizationId;
    }
}