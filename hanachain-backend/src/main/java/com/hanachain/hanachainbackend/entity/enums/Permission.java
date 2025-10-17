package com.hanachain.hanachainbackend.entity.enums;

/**
 * Permission enumeration
 * Defines specific permissions that can be assigned to roles
 */
public enum Permission {
    // Organization Management Permissions
    CREATE_ORGANIZATION("단체 생성"),
    UPDATE_ANY_ORGANIZATION("모든 단체 수정"),
    UPDATE_OWN_ORGANIZATION("소속 단체 수정"),
    DELETE_ANY_ORGANIZATION("모든 단체 삭제"),
    DELETE_OWN_ORGANIZATION("소속 단체 삭제"),
    VIEW_ANY_ORGANIZATION("모든 단체 조회"),
    VIEW_OWN_ORGANIZATION("소속 단체 조회"),
    
    // Member Management Permissions
    MANAGE_ANY_ORGANIZATION_MEMBERS("모든 단체 멤버 관리"),
    MANAGE_OWN_ORGANIZATION_MEMBERS("소속 단체 멤버 관리"),
    VIEW_ANY_ORGANIZATION_MEMBERS("모든 단체 멤버 조회"),
    VIEW_OWN_ORGANIZATION_MEMBERS("소속 단체 멤버 조회"),
    
    // Campaign Management Permissions
    CREATE_CAMPAIGN("캠페인 생성"),
    UPDATE_ANY_CAMPAIGN("모든 캠페인 수정"),
    UPDATE_OWN_CAMPAIGN("본인 캠페인 수정"),
    DELETE_ANY_CAMPAIGN("모든 캠페인 삭제"),
    DELETE_OWN_CAMPAIGN("본인 캠페인 삭제"),
    VIEW_ANY_CAMPAIGN("모든 캠페인 조회"),
    VIEW_PUBLIC_CAMPAIGN("공개 캠페인 조회"),
    
    // User Management Permissions
    MANAGE_USERS("사용자 관리"),
    VIEW_USER_DETAILS("사용자 상세 조회"),
    
    // System Permissions
    SYSTEM_ADMIN("시스템 관리"),
    AUDIT_LOG_ACCESS("감사 로그 접근"),
    
    // Role Management Permissions
    ASSIGN_ORGANIZATION_ROLES("단체 역할 할당"),
    ASSIGN_SYSTEM_ROLES("시스템 역할 할당");
    
    private final String displayName;
    
    Permission(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}