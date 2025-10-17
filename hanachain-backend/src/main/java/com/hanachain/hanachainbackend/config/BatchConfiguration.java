package com.hanachain.hanachainbackend.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 설정 클래스
 *
 * 캠페인 마감 시 기부 내역을 일괄 처리하여 USDC 토큰을 전송하는 배치 작업을 설정합니다.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    // Batch Job 및 Step 설정은 별도의 Job Configuration 클래스에서 정의됩니다.
}
