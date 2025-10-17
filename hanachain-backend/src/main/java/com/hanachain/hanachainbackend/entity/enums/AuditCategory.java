package com.hanachain.hanachainbackend.entity.enums;

/**
 * Enumeration of audit categories
 */
public enum AuditCategory {
    AUTHENTICATION("인증"),
    AUTHORIZATION("권한 부여"),
    USER_MANAGEMENT("사용자 관리"),
    ROLE_MANAGEMENT("역할 관리"),
    ORGANIZATION_MANAGEMENT("단체 관리"),
    CAMPAIGN_MANAGEMENT("캠페인 관리"),
    DONATION_MANAGEMENT("기부 관리"),
    SECURITY("보안"),
    SYSTEM("시스템"),
    ADMIN("관리자"),
    DATA_ACCESS("데이터 접근"),
    CONFIGURATION("설정"),
    COMPLIANCE("규정 준수"),
    PRIVACY("개인정보보호");
    
    private final String displayName;
    
    AuditCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}