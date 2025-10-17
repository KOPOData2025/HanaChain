package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransactionListResponse;
import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.service.blockchain.BlockchainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 블록체인 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/blockchain")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Blockchain", description = "블록체인 관련 API")
public class BlockchainController {

    private final BlockchainService blockchainService;

    /**
     * 캠페인의 블록체인 트랜잭션 목록 조회 (공개 API)
     *
     * @param campaignId 캠페인 ID
     * @param limit 조회할 트랜잭션 수 (기본값: 10, 최대: 100)
     * @return 트랜잭션 목록 응답
     */
    @GetMapping("/campaigns/{campaignId}/transactions")
    @Operation(summary = "캠페인 블록체인 트랜잭션 조회", description = "캠페인의 블록체인 트랜잭션 목록을 조회합니다")
    public ResponseEntity<ApiResponse<BlockchainTransactionListResponse>> getCampaignTransactions(
            @Parameter(description = "캠페인 ID", required = true)
            @PathVariable Long campaignId,
            @Parameter(description = "조회할 트랜잭션 수 (기본: 10, 최대: 100)")
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("Blockchain transaction list request - campaignId: {}, limit: {}", campaignId, limit);

        try {
            // limit 범위 검증
            if (limit < 1) {
                limit = 10;
            } else if (limit > 100) {
                limit = 100;
            }

            BlockchainTransactionListResponse response = blockchainService.getCampaignTransactions(
                    campaignId,
                    limit
            );

            log.info("Successfully retrieved {} transactions for campaign {}", response.getTotalCount(), campaignId);

            return ResponseEntity.ok(
                    ApiResponse.success("블록체인 트랜잭션 목록 조회 성공", response)
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid campaign ID - campaignId: {}", campaignId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get blockchain transactions - campaignId: {}", campaignId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("블록체인 트랜잭션 조회에 실패했습니다: " + e.getMessage()));
        }
    }
}
