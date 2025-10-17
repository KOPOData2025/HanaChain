package com.hanachain.hanachainbackend.dto.fds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FDS API 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdsRequest {

    /**
     * 기부 금액
     */
    private Double amount;

    /**
     * 캠페인 ID
     */
    private Long campaign_id;

    /**
     * 사용자 ID
     */
    private Long user_id;

    /**
     * 결제 수단
     */
    private String payment_method;
}
