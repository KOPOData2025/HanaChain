package com.hanachain.hanachainbackend.dto.donation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 기부 금액 추이 데이터 포인트
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationTrendData {

    /**
     * 날짜 (YYYY-MM-DD)
     */
    private String date;

    /**
     * 해당일 총 기부액
     */
    private BigDecimal amount;

    /**
     * 해당일 기부 건수
     */
    private Long count;
}
