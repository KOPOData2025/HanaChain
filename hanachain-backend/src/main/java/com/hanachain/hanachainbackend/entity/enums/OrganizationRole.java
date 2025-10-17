package com.hanachain.hanachainbackend.entity.enums;

/**
 * Organization role enumeration
 * Defines the roles a user can have within an organization
 */
public enum OrganizationRole {
    OWNER("소유자"),
    ADMIN("관리자"),
    MEMBER("멤버"),
    VOLUNTEER("자원봉사자"),
    ORG_ADMIN("관리자"),
    ORG_MEMBER("멤버");
    
    private final String displayName;
    
    OrganizationRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isAdmin() {
        return this == ORG_ADMIN || this == ADMIN || this == OWNER;
    }
    
    public boolean isMember() {
        return this == ORG_MEMBER || this == MEMBER;
    }
    
    public boolean canManageOrganization() {
        return this == ORG_ADMIN || this == ADMIN || this == OWNER;
    }
    
    public boolean canManageMembers() {
        return this == ORG_ADMIN || this == ADMIN || this == OWNER;
    }
    
    public boolean canCreateCampaigns() {
        return true; // Both admin and member can create campaigns
    }
}