package com.hanachain.hanachainbackend.dto.donation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 기부 통계
 * 캠페인 또는 사용자의 기부 통계 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationStats {
    
    /**
     * 총 기부 금액 (완료된 기부만)
     */
    private BigDecimal totalAmount;
    
    /**
     * 총 기부 건수
     */
    private Long totalCount;
    
    /**
     * 완료된 기부 건수
     */
    private Long completedCount;
    
    /**
     * 진행 중인 기부 건수 (PENDING, PROCESSING)
     */
    private Long pendingCount;
    
    /**
     * 실패한 기부 건수
     */
    private Long failedCount;
    
    /**
     * 평균 기부 금액
     */
    private BigDecimal averageAmount;
    
    /**
     * 고유 기부자 수 (캠페인별 통계에서만 사용)
     */
    private Long uniqueDonorCount;
    
    /**
     * 기부 성공률 (완료된 기부 / 전체 기부)
     */
    public BigDecimal getSuccessRate() {
        if (totalCount == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(completedCount)
                .divide(BigDecimal.valueOf(totalCount), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * 평균 기부 금액 계산
     */
    public void calculateAverageAmount() {
        if (completedCount > 0 && totalAmount != null) {
            this.averageAmount = totalAmount
                    .divide(BigDecimal.valueOf(completedCount), 2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.averageAmount = BigDecimal.ZERO;
        }
    }
}