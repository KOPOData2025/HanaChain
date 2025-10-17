package com.hanachain.hanachainbackend.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 배치 작업 상태 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobStatusResponse {

    /**
     * 작업 실행 ID
     */
    private Long jobExecutionId;

    /**
     * 캠페인 ID
     */
    private Long campaignId;

    /**
     * 작업 이름
     */
    private String jobName;

    /**
     * 작업 상태 (STARTING, STARTED, STOPPING, STOPPED, FAILED, COMPLETED, UNKNOWN)
     */
    private String status;

    /**
     * 시작 시간
     */
    private LocalDateTime startTime;

    /**
     * 종료 시간
     */
    private LocalDateTime endTime;

    /**
     * 처리된 총 기부 건수
     */
    private Integer totalProcessed;

    /**
     * 성공한 전송 건수
     */
    private Integer successfulTransfers;

    /**
     * 실패한 전송 건수
     */
    private Integer failedTransfers;

    /**
     * Skip된 건수
     */
    private Integer skippedCount;

    /**
     * 진행률 (0-100)
     */
    private Double progressPercentage;

    /**
     * Exit 코드
     */
    private String exitCode;

    /**
     * Exit 메시지
     */
    private String exitMessage;

    /**
     * 실행 중 여부
     */
    private Boolean running;

    /**
     * 예상 남은 시간 (초)
     */
    private Long estimatedRemainingSeconds;
}
