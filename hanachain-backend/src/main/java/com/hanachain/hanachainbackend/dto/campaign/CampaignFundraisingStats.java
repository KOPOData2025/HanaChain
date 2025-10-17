package com.hanachain.hanachainbackend.dto.campaign;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 캠페인 담당자용 모금 통계 응답
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignFundraisingStats {

    // 기본 모금 정보
    private Long currentAmount;           // 현재 모금액
    private Long targetAmount;            // 목표 금액
    private Double progressPercentage;    // 달성률 (%)
    private Integer donorCount;           // 총 기부자 수

    // 기간 정보
    private Integer daysLeft;             // 남은 일수
    private LocalDateTime startDate;      // 시작일
    private LocalDateTime endDate;        // 종료일

    // 통계 정보
    private Long averageDonationAmount;   // 평균 기부 금액
    private List<DailyDonationTrend> dailyDonationTrend;  // 일별 기부 추이
    private List<TopDonation> topDonations;               // 상위 기부 목록

    /**
     * 일별 기부 추이
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyDonationTrend {
        private String date;              // 날짜 (YYYY-MM-DD)
        private Long amount;              // 해당일 총 기부액
        private Integer count;            // 해당일 기부 건수
    }

    /**
     * 상위 기부 정보
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopDonation {
        private String donorName;         // 기부자명 (익명 처리 포함)
        private Long amount;              // 기부 금액
        private LocalDateTime donatedAt;  // 기부 날짜
        private Boolean anonymous;        // 익명 여부
    }
}
