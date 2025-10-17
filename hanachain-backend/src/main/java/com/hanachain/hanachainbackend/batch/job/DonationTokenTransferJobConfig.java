package com.hanachain.hanachainbackend.batch.job;

import com.hanachain.hanachainbackend.batch.listener.DonationTransferJobListener;
import com.hanachain.hanachainbackend.batch.processor.DonationTokenProcessor;
import com.hanachain.hanachainbackend.batch.reader.DonationItemReader;
import com.hanachain.hanachainbackend.batch.writer.DonationTokenWriter;
import com.hanachain.hanachainbackend.dto.batch.DonationTransferResult;
import com.hanachain.hanachainbackend.entity.Donation;
import com.hanachain.hanachainbackend.exception.batch.BlockchainNetworkException;
import com.hanachain.hanachainbackend.exception.batch.InsufficientBalanceException;
import com.hanachain.hanachainbackend.exception.batch.WalletNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 기부 토큰 전송 Batch Job 설정
 *
 * 캠페인 마감 시 완료된 기부 내역을 처리하여 USDC 토큰을 일괄 전송합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DonationTokenTransferJobConfig {

    private final DonationItemReader donationItemReader;
    private final DonationTokenProcessor donationTokenProcessor;
    private final DonationTokenWriter donationTokenWriter;
    private final DonationTransferJobListener donationTransferJobListener;

    @Value("${batch.donation-transfer.chunk-size:100}")
    private int chunkSize;

    @Value("${batch.donation-transfer.retry-limit:3}")
    private int retryLimit;

    @Value("${batch.donation-transfer.skip-limit:1000}")
    private int skipLimit;

    public static final String JOB_NAME = "donationTokenTransferJob";
    public static final String STEP_NAME = "donationProcessingStep";

    /**
     * 기부 토큰 전송 Job
     */
    @Bean(name = JOB_NAME)
    public Job donationTokenTransferJob(
            JobRepository jobRepository,
            Step donationProcessingStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(donationTransferJobListener)
                .start(donationProcessingStep)
                .build();
    }

    /**
     * 기부 처리 Step
     *
     * Reader → Processor → Writer 순서로 처리
     * - Chunk 크기: 100건 (설정 가능)
     * - Retry: 네트워크 오류 시 최대 3회
     * - Skip: 잔액 부족, 지갑 없음 등
     *
     * 참고: Reader는 Step Scope로 별도 Bean 등록 필요
     * JobLauncher에서 campaignId 파라미터를 전달받아 동적으로 생성
     */
    @Bean(name = STEP_NAME)
    public Step donationProcessingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RepositoryItemReader<Donation> donationReader
    ) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<Donation, DonationTransferResult>chunk(chunkSize, transactionManager)
                .reader(donationReader)
                .processor(donationTokenProcessor)
                .writer(donationTokenWriter)
                .faultTolerant()
                // Retry 설정: 네트워크 오류 시 재시도
                .retry(BlockchainNetworkException.class)
                .retryLimit(retryLimit)
                // Skip 설정: 복구 불가능한 오류는 건너뜀
                .skip(WalletNotFoundException.class)
                .skip(InsufficientBalanceException.class)
                .skip(IllegalStateException.class)
                .skipLimit(skipLimit)
                .build();
    }

    /**
     * Step Scope Donation Reader
     *
     * Job Parameter에서 campaignId를 받아 동적으로 생성
     */
    @Bean
    @org.springframework.context.annotation.Scope(value = "step", proxyMode = org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS)
    public RepositoryItemReader<Donation> donationReader(
            @org.springframework.beans.factory.annotation.Value("#{jobParameters['campaignId']}") Long campaignId
    ) {
        if (campaignId == null) {
            // 기본값 설정 (실제로는 사용되지 않음, Bean 생성을 위한 더미)
            campaignId = -1L;
        }
        return donationItemReader.createReader(campaignId, chunkSize);
    }
}
