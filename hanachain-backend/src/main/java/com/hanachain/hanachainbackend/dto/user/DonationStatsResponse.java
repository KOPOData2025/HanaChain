package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 기부 통계 응답 DTO
 * 프론트엔드의 DonationStats 인터페이스와 매핑
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationStatsResponse {
    
    private BigDecimal totalAmount;     // 총 기부 금액
    private Long totalCount;            // 총 기부 횟수
    private Long completedCount;        // 완료된 기부
    private Long pendingCount;          // 진행중인 기부
    private Long failedCount;           // 실패한 기부
    
    /**
     * 사용자 기부 통계 배열로부터 DonationStatsResponse 생성
     * Repository getUserDonationStats 메서드의 결과를 변환
     */
    public static DonationStatsResponse from(Object[] stats) {
        if (stats == null || stats.length < 5) {
            return DonationStatsResponse.builder()
                    .totalAmount(BigDecimal.ZERO)
                    .totalCount(0L)
                    .completedCount(0L)
                    .pendingCount(0L)
                    .failedCount(0L)
                    .build();
        }
        
        return DonationStatsResponse.builder()
                .totalAmount((BigDecimal) (stats[0] != null ? stats[0] : BigDecimal.ZERO))
                .totalCount(((Number) (stats[1] != null ? stats[1] : 0)).longValue())
                .completedCount(((Number) (stats[2] != null ? stats[2] : 0)).longValue())
                .pendingCount(((Number) (stats[3] != null ? stats[3] : 0)).longValue())
                .failedCount(((Number) (stats[4] != null ? stats[4] : 0)).longValue())
                .build();
    }
    
    /**
     * User 엔티티로부터 간단한 통계 생성 (기본값)
     */
    public static DonationStatsResponse fromUser(com.hanachain.hanachainbackend.entity.User user) {
        // User 엔티티에는 캐시된 기부 통계가 없으므로 기본값 반환
        return DonationStatsResponse.builder()
                .totalAmount(BigDecimal.ZERO)
                .totalCount(0L)
                .completedCount(0L)
                .pendingCount(0L)
                .failedCount(0L)
                .build();
    }
}