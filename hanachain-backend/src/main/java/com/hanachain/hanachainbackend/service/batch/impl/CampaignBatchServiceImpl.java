package com.hanachain.hanachainbackend.service.batch.impl;

import com.hanachain.hanachainbackend.dto.batch.BatchJobStatusResponse;
import com.hanachain.hanachainbackend.dto.batch.CampaignCloseResponse;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.repository.DonationRepository;
import com.hanachain.hanachainbackend.service.batch.CampaignBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

/**
 * 캠페인 배치 처리 서비스 구현
 *
 * Spring Batch를 사용하여 캠페인 마감 시 기부 내역을
 * 블록체인 트랜잭션으로 전송하는 배치 작업을 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignBatchServiceImpl implements CampaignBatchService {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job donationTokenTransferJob;
    private final CampaignRepository campaignRepository;
    private final DonationRepository donationRepository;

    @Override
    public CampaignCloseResponse closeCampaignAndStartBatch(Long campaignId) {
        log.info("🚀 캠페인 마감 및 배치 작업 시작 - campaignId: {}", campaignId);

        // 1. 캠페인 조회 및 검증
        Campaign campaign = campaignRepository.findByIdForAdmin(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다: " + campaignId));

        // 2. 캠페인 마감 가능 여부 검증
        validateCampaignForClosing(campaign);

        // 3. 캠페인 상태 업데이트 (별도 트랜잭션으로 처리)
        updateCampaignStatusToCompleted(campaign);

        // 4. 배치 처리 대상 기부 건수 조회
        long totalDonations = donationRepository.countPendingBlockchainRecords(campaignId);
        log.info("📊 배치 처리 대상 기부 건수: {} 건", totalDonations);

        if (totalDonations == 0) {
            log.warn("⚠️ 배치 처리 대상 기부 내역이 없습니다 - campaignId: {}", campaignId);
            return CampaignCloseResponse.builder()
                    .campaignId(campaignId)
                    .campaignTitle(campaign.getTitle())
                    .jobExecutionId(null)
                    .totalDonations(0)
                    .batchStatus("NO_DONATIONS")
                    .batchStartedAt(LocalDateTime.now())
                    .message("마감되었으나 처리할 기부 내역이 없습니다.")
                    .build();
        }

        // 5. Spring Batch Job 실행
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("campaignId", campaignId.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(donationTokenTransferJob, jobParameters);
            Long jobExecutionId = jobExecution.getId();

            log.info("✅ 배치 작업 시작 성공 - jobExecutionId: {}, campaignId: {}",
                    jobExecutionId, campaignId);

            return CampaignCloseResponse.builder()
                    .campaignId(campaignId)
                    .campaignTitle(campaign.getTitle())
                    .jobExecutionId(jobExecutionId)
                    .totalDonations((int) totalDonations)
                    .batchStatus(jobExecution.getStatus().name())
                    .batchStartedAt(LocalDateTime.now())
                    .message("캠페인이 마감되고 블록체인 전송 배치 작업이 시작되었습니다.")
                    .build();

        } catch (JobExecutionAlreadyRunningException e) {
            log.error("❌ 이미 실행 중인 배치 작업 - campaignId: {}", campaignId, e);
            throw new IllegalStateException("이미 실행 중인 배치 작업이 있습니다.", e);
        } catch (JobRestartException e) {
            log.error("❌ 배치 작업 재시작 실패 - campaignId: {}", campaignId, e);
            throw new IllegalStateException("배치 작업을 재시작할 수 없습니다.", e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("❌ 이미 완료된 배치 작업 - campaignId: {}", campaignId, e);
            throw new IllegalStateException("이미 완료된 배치 작업입니다.", e);
        } catch (JobParametersInvalidException e) {
            log.error("❌ 잘못된 Job 파라미터 - campaignId: {}", campaignId, e);
            throw new IllegalArgumentException("잘못된 배치 작업 파라미터입니다.", e);
        }
    }

    @Override
    public BatchJobStatusResponse getBatchJobStatus(Long jobExecutionId) {
        log.debug("🔍 배치 작업 상태 조회 - jobExecutionId: {}", jobExecutionId);

        // JobExplorer를 사용하여 JobExecution 조회
        JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);

        if (jobExecution == null) {
            throw new IllegalArgumentException("배치 작업을 찾을 수 없습니다: " + jobExecutionId);
        }

        // JobExecution에서 campaignId 추출
        Long campaignId = extractCampaignId(jobExecution);

        return buildBatchJobStatusResponse(jobExecution, campaignId);
    }

    @Override
    public BatchJobStatusResponse getLatestBatchStatus(Long campaignId) {
        log.info("🔍 최신 배치 작업 상태 조회 - campaignId: {}", campaignId);

        // 캠페인 조회
        Campaign campaign = campaignRepository.findByIdForAdmin(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다: " + campaignId));

        // 저장된 jobExecutionId가 있으면 조회
        if (campaign.getBatchJobExecutionId() != null) {
            return getBatchJobStatus(campaign.getBatchJobExecutionId());
        }

        // 저장된 jobExecutionId가 없으면 JobExplorer로 검색
        Set<JobExecution> jobExecutions = jobExplorer.findRunningJobExecutions("donationTokenTransferJob");

        // 실행 중인 작업 중 campaignId가 일치하는 것 찾기
        JobExecution matchingExecution = jobExecutions.stream()
                .filter(je -> campaignId.equals(extractCampaignId(je)))
                .findFirst()
                .orElse(null);

        if (matchingExecution != null) {
            return buildBatchJobStatusResponse(matchingExecution, campaignId);
        }

        throw new IllegalArgumentException("해당 캠페인의 배치 작업 이력이 없습니다: " + campaignId);
    }

    /**
     * 캠페인 마감 가능 여부 검증
     */
    private void validateCampaignForClosing(Campaign campaign) {
        // 이미 마감된 캠페인 체크
        if (campaign.getStatus() == Campaign.CampaignStatus.COMPLETED) {
            throw new IllegalStateException("이미 마감된 캠페인입니다.");
        }

        // 취소된 캠페인 체크
        if (campaign.getStatus() == Campaign.CampaignStatus.CANCELLED) {
            throw new IllegalStateException("취소된 캠페인은 마감할 수 없습니다.");
        }

        // 삭제된 캠페인 체크
        if (campaign.getDeletedAt() != null) {
            throw new IllegalStateException("삭제된 캠페인은 마감할 수 없습니다.");
        }

        // 이미 실행 중인 배치 작업이 있는지 체크
        if ("RUNNING".equals(campaign.getBatchJobStatus()) ||
            "STARTING".equals(campaign.getBatchJobStatus()) ||
            "STARTED".equals(campaign.getBatchJobStatus())) {
            throw new IllegalStateException("이미 실행 중인 배치 작업이 있습니다.");
        }

        // FDS 검증 미통과 거래 체크
        if (donationRepository.existsUnverifiedFdsDonations(campaign.getId())) {
            long unverifiedCount = donationRepository.countUnverifiedFdsDonations(campaign.getId());
            log.error("❌ FDS 검증 미통과 거래 존재 - campaignId: {}, unverifiedCount: {}",
                    campaign.getId(), unverifiedCount);
            throw new IllegalStateException(
                    String.format("FDS 검증을 통과하지 못한 거래가 %d건 존재하여 캠페인을 마감할 수 없습니다. " +
                            "관리자 페이지에서 해당 거래를 확인하고 환불 처리 후 다시 시도해주세요.",
                            unverifiedCount)
            );
        }

        log.info("✅ FDS 검증 완료 - 모든 거래가 검증 통과 - campaignId: {}", campaign.getId());
    }

    /**
     * JobExecution에서 campaignId 추출
     */
    private Long extractCampaignId(JobExecution jobExecution) {
        String campaignIdStr = jobExecution.getJobParameters().getString("campaignId");
        if (campaignIdStr == null) {
            return null;
        }
        try {
            return Long.parseLong(campaignIdStr);
        } catch (NumberFormatException e) {
            log.error("❌ campaignId 파싱 실패: {}", campaignIdStr, e);
            return null;
        }
    }

    /**
     * JobExecution 정보로 BatchJobStatusResponse 생성
     */
    private BatchJobStatusResponse buildBatchJobStatusResponse(JobExecution jobExecution, Long campaignId) {
        // StepExecution에서 통계 추출
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();

        int totalProcessed = 0;
        int successfulTransfers = 0;
        int failedTransfers = 0;
        int skippedCount = 0;

        for (StepExecution stepExecution : stepExecutions) {
            totalProcessed += stepExecution.getReadCount();
            successfulTransfers += stepExecution.getWriteCount();
            skippedCount += stepExecution.getSkipCount();
        }

        failedTransfers = totalProcessed - successfulTransfers - skippedCount;

        // 진행률 계산
        StepExecution latestStep = stepExecutions.stream()
                .max(Comparator.comparing(StepExecution::getLastUpdated))
                .orElse(null);

        double progressPercentage = 0.0;
        if (latestStep != null && latestStep.getReadCount() > 0) {
            long processed = latestStep.getWriteCount() + latestStep.getSkipCount();
            progressPercentage = (processed * 100.0) / latestStep.getReadCount();
        }

        // 실행 중 여부 확인
        boolean running = jobExecution.getStatus().isRunning();

        return BatchJobStatusResponse.builder()
                .jobExecutionId(jobExecution.getId())
                .campaignId(campaignId)
                .jobName(jobExecution.getJobInstance().getJobName())
                .status(jobExecution.getStatus().name())
                .startTime(jobExecution.getStartTime())
                .endTime(jobExecution.getEndTime())
                .totalProcessed(totalProcessed)
                .successfulTransfers(successfulTransfers)
                .failedTransfers(failedTransfers)
                .skippedCount(skippedCount)
                .progressPercentage(progressPercentage)
                .exitCode(jobExecution.getExitStatus().getExitCode())
                .exitMessage(jobExecution.getExitStatus().getExitDescription())
                .running(running)
                .build();
    }

    /**
     * 캠페인 상태를 COMPLETED로 업데이트 (별도 트랜잭션)
     * JobLauncher 호출 전에 트랜잭션을 완료하기 위해 별도 메서드로 분리
     */
    @Transactional
    private void updateCampaignStatusToCompleted(Campaign campaign) {
        campaign.setStatus(Campaign.CampaignStatus.COMPLETED);
        campaign.setUpdatedAt(LocalDateTime.now());
        campaignRepository.save(campaign);
        log.info("✅ 캠페인 상태를 COMPLETED로 변경 - campaignId: {}", campaign.getId());
    }
}
