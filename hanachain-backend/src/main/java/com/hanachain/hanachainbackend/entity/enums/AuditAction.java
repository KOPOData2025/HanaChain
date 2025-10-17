package com.hanachain.hanachainbackend.entity.enums;

/**
 * 감사 작업 열거형
 */
public enum AuditAction {
    // 인증 작업
    LOGIN_ATTEMPT("로그인 시도"),
    LOGIN_SUCCESS("로그인 성공"),
    LOGIN_FAILURE("로그인 실패"),
    LOGOUT("로그아웃"),
    PASSWORD_CHANGE("비밀번호 변경"),
    PASSWORD_RESET("비밀번호 재설정"),
    EMAIL_VERIFICATION("이메일 인증"),

    // 사용자 관리 작업
    USER_CREATE("사용자 생성"),
    USER_UPDATE("사용자 정보 수정"),
    USER_DELETE("사용자 삭제"),
    USER_ENABLE("사용자 활성화"),
    USER_DISABLE("사용자 비활성화"),

    // 역할 및 권한 작업
    ROLE_ASSIGN("역할 할당"),
    ROLE_REVOKE("역할 취소"),
    PERMISSION_GRANT("권한 부여"),
    PERMISSION_REVOKE("권한 취소"),
    ROLE_ELEVATION("권한 상승"),

    // 단체 작업
    ORGANIZATION_CREATE("단체 생성"),
    ORGANIZATION_UPDATE("단체 정보 수정"),
    ORGANIZATION_DELETE("단체 삭제"),
    ORGANIZATION_JOIN("단체 가입"),
    ORGANIZATION_LEAVE("단체 탈퇴"),
    ORGANIZATION_ROLE_ASSIGN("단체 역할 할당"),
    ORGANIZATION_ROLE_REVOKE("단체 역할 취소"),

    // 캠페인 작업
    CAMPAIGN_CREATE("캠페인 생성"),
    CAMPAIGN_UPDATE("캠페인 수정"),
    CAMPAIGN_DELETE("캠페인 삭제"),
    CAMPAIGN_PUBLISH("캠페인 발행"),
    CAMPAIGN_UNPUBLISH("캠페인 숨김"),

    // 기부 작업
    DONATION_CREATE("기부 생성"),
    DONATION_UPDATE("기부 수정"),
    DONATION_REFUND("기부 환불"),

    // 보안 작업
    ACCESS_DENIED("접근 거부"),
    PERMISSION_DENIED("권한 거부"),
    SECURITY_VIOLATION("보안 위반"),
    SUSPICIOUS_ACTIVITY("의심스러운 활동"),

    // 시스템 작업
    SYSTEM_CONFIG_CHANGE("시스템 설정 변경"),
    DATA_EXPORT("데이터 내보내기"),
    DATA_IMPORT("데이터 가져오기"),
    BACKUP_CREATE("백업 생성"),
    BACKUP_RESTORE("백업 복원"),

    // 관리자 작업
    ADMIN_LOGIN("관리자 로그인"),
    ADMIN_ACTION("관리자 작업"),
    AUDIT_LOG_VIEW("감사 로그 조회"),
    AUDIT_LOG_EXPORT("감사 로그 내보내기"),

    // 일반 작업
    CREATE("생성"),
    READ("조회"),
    UPDATE("수정"),
    DELETE("삭제");
    
    private final String displayName;
    
    AuditAction(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}