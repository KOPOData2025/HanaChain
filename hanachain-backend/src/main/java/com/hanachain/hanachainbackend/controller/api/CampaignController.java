package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.campaign.CampaignCreateRequest;
import com.hanachain.hanachainbackend.dto.campaign.CampaignDetailResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignFundraisingStats;
import com.hanachain.hanachainbackend.dto.campaign.CampaignListResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignUpdateRequest;
import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import com.hanachain.hanachainbackend.security.annotation.RequiresCampaignManagement;
import com.hanachain.hanachainbackend.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;

import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/campaigns")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Campaign", description = "캠페인 관리 관련 API")
public class CampaignController {
    
    private final CampaignService campaignService;
    
    // 캠페인 엔티티에서 허용되는 정렬 필드들
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "title", "targetAmount", "currentAmount", "donorCount",
        "status", "category", "startDate", "endDate", "createdAt", "updatedAt"
    );
    
    /**
     * Pageable의 정렬 필드를 검증하고 안전한 Pageable 객체를 반환합니다.
     */
    private Pageable validateAndSanitizePageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        
        List<Sort.Order> validOrders = pageable.getSort().stream()
            .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
            .toList();
        
        if (validOrders.isEmpty()) {
            // 유효하지 않은 정렬 필드가 있는 경우 기본 정렬로 대체
            log.warn("Invalid sort fields detected in pageable. Using default sort: createdAt DESC");
            return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }
        
        if (validOrders.size() != pageable.getSort().stream().count()) {
            // 일부 유효하지 않은 필드가 있는 경우, 유효한 필드만 사용
            log.warn("Some invalid sort fields detected. Using only valid fields: {}", 
                validOrders.stream().map(Sort.Order::getProperty).toList());
            
            return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(validOrders)
            );
        }
        
        return pageable;
    }
    
    @PostMapping
    @Operation(summary = "캠페인 생성", description = "새 캠페인을 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> createCampaign(
            @Valid @RequestBody CampaignCreateRequest request) {

        log.info("캠페인 생성 요청");

        Long currentUserId = SecurityUtils.getCurrentUserId();
        Campaign campaign = campaignService.createCampaign(currentUserId, request);
        CampaignDetailResponse response = CampaignDetailResponse.fromEntity(campaign);

        log.info("캠페인이 성공적으로 생성되었습니다: {}", campaign.getId());
        return ResponseEntity.ok(ApiResponse.success("캠페인이 생성되었습니다.", response));
    }
    
    @PutMapping("/{campaignId}")
    @Operation(summary = "캠페인 수정", description = "기존 캠페인을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @RequiresCampaignManagement(message = "캠페인 수정 권한이 없습니다")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> updateCampaign(
            @PathVariable Long campaignId,
            @Valid @RequestBody CampaignUpdateRequest request) {

        log.info("캠페인 수정 요청 - 캠페인 ID: {}", campaignId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        Campaign campaign = campaignService.updateCampaign(campaignId, currentUserId, request);
        CampaignDetailResponse response = CampaignDetailResponse.fromEntity(campaign);

        log.info("캠페인이 성공적으로 수정되었습니다: {}", campaignId);
        return ResponseEntity.ok(ApiResponse.success("캠페인이 수정되었습니다.", response));
    }
    
    @DeleteMapping("/{campaignId}")
    @Operation(summary = "캠페인 삭제", description = "캠페인을 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @RequiresCampaignManagement(creatorOnly = true, message = "캠페인 삭제는 생성자만 가능합니다")
    public ResponseEntity<ApiResponse<Void>> deleteCampaign(@PathVariable Long campaignId) {

        log.info("캠페인 삭제 요청 - 캠페인 ID: {}", campaignId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        campaignService.deleteCampaign(campaignId, currentUserId);

        log.info("캠페인이 성공적으로 삭제되었습니다: {}", campaignId);
        return ResponseEntity.ok(ApiResponse.success("캠페인이 삭제되었습니다."));
    }
    
    @GetMapping("/{campaignId}")
    @Operation(summary = "캠페인 상세 조회", description = "특정 캠페인의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> getCampaignDetail(@PathVariable Long campaignId) {

        log.info("캠페인 상세 조회 요청 - 캠페인 ID: {}", campaignId);

        CampaignDetailResponse campaign = campaignService.getCampaignDetail(campaignId);

        return ResponseEntity.ok(ApiResponse.success(campaign));
    }
    
    @GetMapping
    @Operation(summary = "캠페인 목록 조회 (통합 필터링)", description = "다양한 필터 조건으로 캠페인 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<CampaignListResponse>>> getCampaigns(
            @Parameter(description = "카테고리 필터") @RequestParam(required = false) Campaign.CampaignCategory category,
            @Parameter(description = "상태 필터") @RequestParam(required = false) Campaign.CampaignStatus status,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "정렬 기준 (recent, popular, progress)") @RequestParam(required = false, defaultValue = "recent") String sort,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("캠페인 목록 조회 요청 - 카테고리: {}, 상태: {}, 키워드: {}, 정렬: {}", category, status, keyword, sort);

        // 정렬 필드 검증 및 정리
        Pageable validatedPageable = validateAndSanitizePageable(pageable);
        Page<CampaignListResponse> campaigns = campaignService.getCampaignsWithFilters(
                category, status, keyword, sort, validatedPageable);

        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @GetMapping("/public")
    @Operation(summary = "공개 캠페인 목록 조회", description = "활성 상태인 공개 캠페인 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<CampaignListResponse>>> getPublicCampaigns(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("공개 캠페인 목록 조회 요청");

        // 정렬 필드 검증 및 정리
        Pageable validatedPageable = validateAndSanitizePageable(pageable);
        Page<CampaignListResponse> campaigns = campaignService.getPublicCampaigns(validatedPageable);

        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }
    
    @GetMapping("/category/{category}")
    @Operation(summary = "카테고리별 캠페인 조회", description = "특정 카테고리의 캠페인 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<CampaignListResponse>>> getCampaignsByCategory(
            @PathVariable Campaign.CampaignCategory category,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("카테고리별 캠페인 조회 요청 - 카테고리: {}", category);

        Page<CampaignListResponse> campaigns = campaignService.getCampaignsByCategory(category, pageable);

        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @GetMapping("/my")
    @Operation(summary = "내 캠페인 목록 조회", description = "현재 사용자가 생성한 캠페인 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Page<CampaignListResponse>>> getMyCampaigns(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("내 캠페인 목록 조회 요청");

        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<CampaignListResponse> campaigns = campaignService.getUserCampaigns(currentUserId, pageable);

        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @GetMapping("/search")
    @Operation(summary = "캠페인 검색", description = "키워드로 캠페인을 검색합니다.")
    public ResponseEntity<ApiResponse<Page<CampaignListResponse>>> searchCampaigns(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("캠페인 검색 요청 - 키워드: {}", keyword);

        Page<CampaignListResponse> campaigns = campaignService.searchCampaigns(keyword, pageable);

        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @GetMapping("/popular")
    @Operation(summary = "인기 캠페인 조회", description = "모금액이 높은 순으로 캠페인을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<CampaignListResponse>>> getPopularCampaigns(
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("인기 캠페인 조회 요청");

        Page<CampaignListResponse> campaigns = campaignService.getPopularCampaigns(pageable);

        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @GetMapping("/recent")
    @Operation(summary = "최근 캠페인 조회", description = "최근 생성된 활성 캠페인을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<CampaignListResponse>>> getRecentCampaigns(
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("최근 캠페인 조회 요청");

        Page<CampaignListResponse> campaigns = campaignService.getRecentCampaigns(pageable);

        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }
    
    // ===== 블록체인 통합 엔드포인트들 =====
    
    @PostMapping("/blockchain")
    @Operation(summary = "블록체인 통합 캠페인 생성", description = "블록체인과 함께 캠페인을 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> createCampaignWithBlockchain(
            @Valid @RequestBody CampaignCreateRequest request,
            @Parameter(description = "즉시 블록체인 등록 여부") @RequestParam(defaultValue = "false") boolean forceBlockchainRegistration) {

        log.info("블록체인 통합 캠페인 생성 요청 - 즉시 등록: {}", forceBlockchainRegistration);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        Campaign campaign = campaignService.createCampaignWithBlockchain(currentUserId, request, forceBlockchainRegistration);
        CampaignDetailResponse response = CampaignDetailResponse.fromEntity(campaign);

        log.info("블록체인 통합 캠페인이 성공적으로 생성되었습니다: {}", campaign.getId());
        return ResponseEntity.ok(ApiResponse.success("블록체인 통합 캠페인이 생성되었습니다.", response));
    }

    @GetMapping("/{campaignId}/blockchain/status")
    @Operation(summary = "블록체인 상태 조회", description = "캠페인의 블록체인 연동 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> getBlockchainStatus(@PathVariable Long campaignId) {

        log.info("블록체인 상태 조회 요청 - 캠페인 ID: {}", campaignId);

        CampaignDetailResponse status = campaignService.getBlockchainStatus(campaignId);

        return ResponseEntity.ok(ApiResponse.success("블록체인 상태 조회 완료", status));
    }

    @PostMapping("/{campaignId}/blockchain/finalize")
    @Operation(summary = "블록체인 캠페인 완료", description = "블록체인에서 캠페인을 완료 처리합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @RequiresCampaignManagement(creatorOnly = true, message = "캠페인 완료는 생성자만 가능합니다")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> finalizeCampaignOnBlockchain(@PathVariable Long campaignId) {

        log.info("블록체인 캠페인 완료 요청 - 캠페인 ID: {}", campaignId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        Campaign campaign = campaignService.finalizeCampaignOnBlockchain(campaignId, currentUserId);
        CampaignDetailResponse response = CampaignDetailResponse.fromEntity(campaign);

        log.info("블록체인 캠페인 완료 처리가 시작되었습니다: {}", campaignId);
        return ResponseEntity.ok(ApiResponse.success("블록체인 캠페인 완료 처리가 시작되었습니다.", response));
    }

    @PostMapping("/{campaignId}/blockchain/retry")
    @Operation(summary = "블록체인 작업 재시도", description = "실패한 블록체인 작업을 재시도합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @RequiresCampaignManagement(creatorOnly = true, message = "블록체인 재시도는 생성자만 가능합니다")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> retryBlockchainOperation(@PathVariable Long campaignId) {

        log.info("블록체인 작업 재시도 요청 - 캠페인 ID: {}", campaignId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        Campaign campaign = campaignService.retryBlockchainOperation(campaignId, currentUserId);
        CampaignDetailResponse response = CampaignDetailResponse.fromEntity(campaign);

        log.info("블록체인 작업 재시도가 시작되었습니다: {}", campaignId);
        return ResponseEntity.ok(ApiResponse.success("블록체인 작업 재시도가 시작되었습니다.", response));
    }

    @PostMapping("/{campaignId}/blockchain/sync")
    @Operation(summary = "블록체인 상태 동기화", description = "캠페인의 블록체인 상태를 수동으로 동기화합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    @RequiresCampaignManagement(message = "블록체인 동기화 권한이 없습니다")
    public ResponseEntity<ApiResponse<Void>> syncBlockchainStatus(@PathVariable Long campaignId) {

        log.info("블록체인 상태 동기화 요청 - 캠페인 ID: {}", campaignId);

        campaignService.syncBlockchainStatus(campaignId);

        log.info("블록체인 상태 동기화가 완료되었습니다: {}", campaignId);
        return ResponseEntity.ok(ApiResponse.success("블록체인 상태 동기화가 완료되었습니다."));
    }

    @GetMapping("/{campaignId}/fundraising-stats")
    @Operation(summary = "캠페인 모금 통계 조회", description = "캠페인 담당자용 모금 통계를 조회합니다. (담당자 전용)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CampaignFundraisingStats>> getCampaignFundraisingStats(
            @PathVariable Long campaignId) {

        log.info("캠페인 모금 통계 조회 요청 - 캠페인 ID: {}", campaignId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        CampaignFundraisingStats stats = campaignService.getCampaignFundraisingStats(campaignId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
