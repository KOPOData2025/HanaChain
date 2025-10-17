package com.hanachain.hanachainbackend.dto.donation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FDS 입력 특성
 * 17개 특성을 4개 카테고리로 분류
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdsFeatures {

    // ===== 거래 정보 (4개) =====

    /**
     * 정규화된 기부 금액 (0.0 ~ 1.0)
     */
    private Double amountNormalized;

    /**
     * 시간대 (0~23)
     */
    private Integer hourOfDay;

    /**
     * 요일 (0=월요일 ~ 6=일요일)
     */
    private Integer dayOfWeek;

    /**
     * 주말 여부 (0=평일, 1=주말)
     */
    private Integer isWeekend;

    // ===== 계정 정보 (5개) =====

    /**
     * 계정 생성 후 경과일 (0 ~ 1000+)
     */
    private Integer accountAge;

    /**
     * 신규 사용자 여부 (0=기존, 1=신규)
     */
    private Integer isNewUser;

    /**
     * 이메일 인증 여부 (0=미인증, 1=인증)
     */
    private Integer emailVerified;

    /**
     * 전화번호 인증 여부 (0=미인증, 1=인증)
     */
    private Integer phoneVerified;

    /**
     * 프로필 작성 여부 (0=미작성, 1=작성)
     */
    private Integer hasProfile;

    // ===== 기부 이력 (5개) =====

    /**
     * 총 기부 횟수
     */
    private Integer donationCount;

    /**
     * 평균 기부 금액
     */
    private Double avgDonationAmount;

    /**
     * 마지막 기부 후 경과일
     */
    private Integer daysSinceLastDonation;

    /**
     * 기부한 캠페인 수
     */
    private Integer uniqueCampaigns;

    /**
     * 의심스러운 패턴 점수 (0 ~ 10)
     */
    private Integer suspiciousPatterns;

    // ===== 결제 수단 (3개) =====

    /**
     * 결제 수단 위험도 (0=안전 ~ 5=고위험)
     */
    private Integer paymentMethodRisk;

    /**
     * 신규 결제 수단 여부 (0=기존, 1=신규)
     */
    private Integer isNewPaymentMethod;

    /**
     * 결제 실패 횟수
     */
    private Integer paymentFailures;
}
