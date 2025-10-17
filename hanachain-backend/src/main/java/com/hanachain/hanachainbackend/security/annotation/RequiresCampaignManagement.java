package com.hanachain.hanachainbackend.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 캠페인 관리 권한이 필요한 메서드에 사용하는 어노테이션
 * 
 * 사용법:
 * @RequiresCampaignManagement
 * public ResponseEntity<?> updateCampaign(@PathVariable Long campaignId, ...) {
 *     // campaignId 파라미터가 자동으로 권한 검증에 사용됩니다
 * }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresCampaignManagement {
    
    /**
     * 캠페인 ID가 포함된 파라미터 이름
     * 기본값: "campaignId"
     */
    String campaignIdParam() default "campaignId";
    
    /**
     * 캠페인 생성자만 접근 가능한지 여부
     * true인 경우 담당자 권한이 아닌 생성자 권한만 허용
     */
    boolean creatorOnly() default false;
    
    /**
     * 에러 메시지 커스터마이징
     */
    String message() default "캠페인 관리 권한이 없습니다";
}