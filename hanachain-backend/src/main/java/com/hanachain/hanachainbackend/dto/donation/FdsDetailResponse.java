package com.hanachain.hanachainbackend.dto.donation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FDS 검증 상세 정보 응답 DTO
 * 프론트엔드의 FdsDetailResult 타입과 매칭
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdsDetailResponse {

    /**
     * 기부 ID
     */
    private Long donationId;

    /**
     * FDS 액션 (APPROVE, MANUAL_REVIEW, BLOCK)
     */
    private String action;

    /**
     * 액션 ID (0=APPROVE, 1=MANUAL_REVIEW, 2=BLOCK)
     */
    private Integer actionId;

    /**
     * 위험 점수 (0.0000 ~ 1.0000)
     */
    private BigDecimal riskScore;

    /**
     * 신뢰도 (0.0000 ~ 1.0000)
     */
    private BigDecimal confidence;

    /**
     * 설명
     */
    private String explanation;

    /**
     * 검증 시각
     */
    private LocalDateTime checkedAt;

    /**
     * FDS 입력 특성 (17개)
     */
    private FdsFeatures features;

    /**
     * DQN Q-Values (3개)
     */
    private FdsQValues qValues;

    /**
     * 타임스탬프 (검증 시각과 동일)
     */
    private LocalDateTime timestamp;
}
