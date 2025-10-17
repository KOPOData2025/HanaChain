package com.hanachain.hanachainbackend.dto.fds;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * FDS API 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdsResponse {

    /**
     * FDS 액션 (APPROVE, MANUAL_REVIEW, BLOCK)
     */
    private String action;

    /**
     * 액션 ID (0: APPROVE, 1: MANUAL_REVIEW, 2: BLOCK)
     */
    @JsonProperty("action_id")
    private Integer actionId;

    /**
     * 위험 점수 (0.0 ~ 1.0)
     */
    @JsonProperty("risk_score")
    private Double riskScore;

    /**
     * 신뢰도 (0.0 ~ 1.0)
     */
    private Double confidence;

    /**
     * 설명
     */
    private String explanation;

    /**
     * 추출된 특징 벡터
     */
    private List<Double> features;

    /**
     * Q-values (DQN 모델의 액션별 Q값)
     * - approve: APPROVE 액션의 Q-value
     * - manual_review: MANUAL_REVIEW 액션의 Q-value
     * - block: BLOCK 액션의 Q-value
     */
    @JsonProperty("q_values")
    private Map<String, Double> qValues;

    /**
     * 타임스탬프
     */
    private String timestamp;
}
