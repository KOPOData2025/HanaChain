package com.hanachain.hanachainbackend.security.exceptions;

/**
 * Exception thrown when user lacks access to a specific organization
 */
public class OrganizationAccessDeniedException extends SecurityException {
    
    private final Long organizationId;
    private final String actionAttempted;
    
    public OrganizationAccessDeniedException(Long organizationId) {
        super("해당 단체에 대한 접근 권한이 없습니다.");
        this.organizationId = organizationId;
        this.actionAttempted = null;
    }
    
    public OrganizationAccessDeniedException(Long organizationId, String actionAttempted) {
        super("해당 단체에 대한 접근 권한이 없습니다: " + actionAttempted);
        this.organizationId = organizationId;
        this.actionAttempted = actionAttempted;
    }
    
    public OrganizationAccessDeniedException(String message, Long organizationId) {
        super(message);
        this.organizationId = organizationId;
        this.actionAttempted = null;
    }
    
    public OrganizationAccessDeniedException(String message, Long organizationId, String actionAttempted) {
        super(message);
        this.organizationId = organizationId;
        this.actionAttempted = actionAttempted;
    }
    
    public Long getOrganizationId() {
        return organizationId;
    }
    
    public String getActionAttempted() {
        return actionAttempted;
    }
}