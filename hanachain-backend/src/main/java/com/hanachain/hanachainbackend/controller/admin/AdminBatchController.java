package com.hanachain.hanachainbackend.controller.admin;

import com.hanachain.hanachainbackend.dto.batch.BatchJobStatusResponse;
import com.hanachain.hanachainbackend.dto.batch.CampaignCloseResponse;
import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.service.batch.CampaignBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 배치 작업 컨트롤러
 *
 * 캠페인 마감 시 기부 내역을 블록체인 트랜잭션으로 전송하는
 * Spring Batch 작업을 관리하는 API 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Batch", description = "관리자 배치 작업 관리 API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBatchController {

    private final CampaignBatchService batchService;

    @Operation(
        summary = "캠페인 마감 및 배치 작업 시작",
        description = "캠페인을 마감하고 기부 내역을 블록체인으로 전송하는 배치 작업을 시작합니다."
    )
    @PostMapping("/campaigns/{campaignId}/close")
    public ResponseEntity<ApiResponse<CampaignCloseResponse>> closeCampaignAndStartBatch(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId) {

        log.info("🚀 캠페인 마감 및 배치 작업 시작 요청 - campaignId: {}", campaignId);

        try {
            CampaignCloseResponse response = batchService.closeCampaignAndStartBatch(campaignId);

            log.info("✅ 캠페인 마감 및 배치 작업 시작 성공 - campaignId: {}, jobExecutionId: {}, totalDonations: {}",
                campaignId, response.getJobExecutionId(), response.getTotalDonations());

            return ResponseEntity.ok(ApiResponse.success(
                response.getMessage(),
                response
            ));

        } catch (Exception e) {
            log.error("❌ 캠페인 마감 및 배치 작업 시작 실패 - campaignId: {}", campaignId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("배치 작업 시작에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "배치 작업 상태 조회",
        description = "실행 중이거나 완료된 배치 작업의 상태를 조회합니다."
    )
    @GetMapping("/batch/jobs/{jobExecutionId}/status")
    public ResponseEntity<ApiResponse<BatchJobStatusResponse>> getBatchJobStatus(
            @Parameter(description = "작업 실행 ID") @PathVariable Long jobExecutionId) {

        log.debug("🔍 배치 작업 상태 조회 요청 - jobExecutionId: {}", jobExecutionId);

        try {
            BatchJobStatusResponse response = batchService.getBatchJobStatus(jobExecutionId);

            log.debug("✅ 배치 작업 상태 조회 성공 - jobExecutionId: {}, status: {}, progress: {}%",
                jobExecutionId, response.getStatus(), response.getProgressPercentage());

            return ResponseEntity.ok(ApiResponse.success(
                "배치 작업 상태를 조회했습니다.",
                response
            ));

        } catch (Exception e) {
            log.error("❌ 배치 작업 상태 조회 실패 - jobExecutionId: {}", jobExecutionId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("배치 작업 상태 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "캠페인의 최신 배치 작업 상태 조회",
        description = "특정 캠페인에서 실행된 가장 최근 배치 작업의 상태를 조회합니다."
    )
    @GetMapping("/campaigns/{campaignId}/batch/latest")
    public ResponseEntity<ApiResponse<BatchJobStatusResponse>> getLatestBatchStatus(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId) {

        log.info("🔍 최신 배치 작업 상태 조회 요청 - campaignId: {}", campaignId);

        try {
            BatchJobStatusResponse response = batchService.getLatestBatchStatus(campaignId);

            log.info("✅ 최신 배치 작업 상태 조회 성공 - campaignId: {}, jobExecutionId: {}, status: {}",
                campaignId, response.getJobExecutionId(), response.getStatus());

            return ResponseEntity.ok(ApiResponse.success(
                "최신 배치 작업 상태를 조회했습니다.",
                response
            ));

        } catch (Exception e) {
            log.error("❌ 최신 배치 작업 상태 조회 실패 - campaignId: {}", campaignId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("최신 배치 작업 상태 조회에 실패했습니다: " + e.getMessage()));
        }
    }
}
