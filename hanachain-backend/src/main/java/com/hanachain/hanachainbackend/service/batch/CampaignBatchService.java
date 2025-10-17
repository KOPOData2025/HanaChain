package com.hanachain.hanachainbackend.service.batch;

import com.hanachain.hanachainbackend.dto.batch.BatchJobStatusResponse;
import com.hanachain.hanachainbackend.dto.batch.CampaignCloseResponse;

/**
 * 캠페인 배치 처리 서비스 인터페이스
 */
public interface CampaignBatchService {

    /**
     * 캠페인 마감 및 토큰 전송 배치 작업 시작
     *
     * @param campaignId 마감할 캠페인 ID
     * @return 배치 작업 시작 응답
     */
    CampaignCloseResponse closeCampaignAndStartBatch(Long campaignId);

    /**
     * 배치 작업 상태 조회
     *
     * @param jobExecutionId 작업 실행 ID
     * @return 배치 작업 상태
     */
    BatchJobStatusResponse getBatchJobStatus(Long jobExecutionId);

    /**
     * 캠페인의 최근 배치 작업 상태 조회
     *
     * @param campaignId 캠페인 ID
     * @return 배치 작업 상태
     */
    BatchJobStatusResponse getLatestBatchStatus(Long campaignId);
}
