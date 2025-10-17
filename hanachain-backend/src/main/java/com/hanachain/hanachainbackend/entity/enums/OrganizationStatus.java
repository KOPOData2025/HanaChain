package com.hanachain.hanachainbackend.entity.enums;

/**
 * Organization status enumeration
 * Defines the possible states of an organization
 */
public enum OrganizationStatus {
    ACTIVE("활성"),
    INACTIVE("비활성");
    
    private final String displayName;
    
    OrganizationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    public boolean isInactive() {
        return this == INACTIVE;
    }
}