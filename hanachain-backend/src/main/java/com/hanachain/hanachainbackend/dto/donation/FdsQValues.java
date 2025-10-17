package com.hanachain.hanachainbackend.dto.donation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FDS DQN Q-Values
 * DQN (Deep Q-Network) 모델의 3가지 액션별 Q-값
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdsQValues {

    /**
     * APPROVE 액션의 Q-값
     * 정상 기부로 판단하여 승인할 때의 예상 보상 값
     */
    private Double approve;

    /**
     * MANUAL_REVIEW 액션의 Q-값
     * 수동 검토를 요청할 때의 예상 보상 값
     */
    private Double manualReview;

    /**
     * BLOCK 액션의 Q-값
     * 사기 기부로 판단하여 차단할 때의 예상 보상 값
     */
    private Double block;
}
