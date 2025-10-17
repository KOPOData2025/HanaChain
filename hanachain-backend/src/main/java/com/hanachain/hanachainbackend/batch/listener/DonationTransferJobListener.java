package com.hanachain.hanachainbackend.batch.listener;

import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 기부 토큰 전송 Job Listener
 *
 * Job 시작 및 종료 시 캠페인 상태를 업데이트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DonationTransferJobListener implements JobExecutionListener {

    private final CampaignRepository campaignRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        Long campaignId = getCampaignId(jobExecution);
        if (campaignId == null) {
            log.warn("Campaign ID not found in job parameters");
            return;
        }

        log.info("Starting donation token transfer job for campaign: {}", campaignId);

        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign != null) {
            campaign.setBatchJobExecutionId(jobExecution.getId());
            campaign.setBatchJobStatus("RUNNING");
            campaign.setBatchStartedAt(LocalDateTime.now());
            campaignRepository.save(campaign);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Long campaignId = getCampaignId(jobExecution);
        if (campaignId == null) {
            return;
        }

        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            log.warn("Campaign not found: {}", campaignId);
            return;
        }

        // Step 실행 통계 수집
        int totalProcessed = 0;
        int successCount = 0;
        int skipCount = 0;

        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            totalProcessed += stepExecution.getReadCount();
            successCount += stepExecution.getWriteCount();
            skipCount += stepExecution.getSkipCount();
        }

        int failedCount = totalProcessed - successCount - skipCount;

        // 캠페인 통계 업데이트
        campaign.setTotalDonationsProcessed(totalProcessed);
        campaign.setSuccessfulTransfers(successCount);
        campaign.setFailedTransfers(failedCount);
        campaign.setBatchCompletedAt(LocalDateTime.now());

        // 배치 상태 업데이트
        BatchStatus status = jobExecution.getStatus();
        if (status == BatchStatus.COMPLETED) {
            campaign.setBatchJobStatus("COMPLETED");
            log.info("Job completed successfully for campaign: {} - Processed: {}, Success: {}, Failed: {}, Skipped: {}",
                    campaignId, totalProcessed, successCount, failedCount, skipCount);
        } else if (status == BatchStatus.FAILED) {
            campaign.setBatchJobStatus("FAILED");
            log.error("Job failed for campaign: {} - Processed: {}, Success: {}, Failed: {}, Skipped: {}",
                    campaignId, totalProcessed, successCount, failedCount, skipCount);
        } else {
            campaign.setBatchJobStatus(status.name());
            log.warn("Job ended with status {} for campaign: {}", status, campaignId);
        }

        campaignRepository.save(campaign);
    }

    /**
     * Job Parameter에서 campaignId 추출
     */
    private Long getCampaignId(JobExecution jobExecution) {
        String campaignIdStr = jobExecution.getJobParameters().getString("campaignId");
        if (campaignIdStr == null) {
            return null;
        }
        try {
            return Long.parseLong(campaignIdStr);
        } catch (NumberFormatException e) {
            log.error("Invalid campaign ID format: {}", campaignIdStr);
            return null;
        }
    }
}
