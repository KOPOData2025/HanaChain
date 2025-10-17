package com.hanachain.hanachainbackend.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 캠페인 마감 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCloseResponse {

    /**
     * 캠페인 ID
     */
    private Long campaignId;

    /**
     * 캠페인 제목
     */
    private String campaignTitle;

    /**
     * 배치 작업 실행 ID
     */
    private Long jobExecutionId;

    /**
     * 처리 대상 기부 건수
     */
    private Integer totalDonations;

    /**
     * 배치 작업 상태
     */
    private String batchStatus;

    /**
     * 배치 시작 시간
     */
    private LocalDateTime batchStartedAt;

    /**
     * 메시지
     */
    private String message;
}
