package com.hanachain.hanachainbackend.dto.donation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 관리자 기부 금액 추이 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDonationTrendResponse {

    /**
     * 조회 기간
     * 가능한 값: 7d (최근 7일), 30d (최근 30일), 3m (최근 3개월), all (전체)
     */
    private String period;

    /**
     * 일별 기부 데이터 목록
     */
    private List<DonationTrendData> data;

    /**
     * 기간 내 총 기부액
     */
    private BigDecimal totalAmount;

    /**
     * 기간 내 총 기부 건수
     */
    private Long totalCount;

    /**
     * 기간 내 평균 기부액
     */
    private BigDecimal averageAmount;
}
