package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.campaignmanager.CampaignManagerCreateRequest;
import com.hanachain.hanachainbackend.dto.campaignmanager.CampaignManagerResponse;
import com.hanachain.hanachainbackend.dto.campaignmanager.CampaignManagerUpdateRequest;
import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.service.CampaignManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/campaign-managers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Campaign Manager", description = "캠페인 담당자 관리 API")
public class CampaignManagerController {
    
    private final CampaignManagerService campaignManagerService;
    
    @PostMapping
    @Operation(summary = "캠페인 담당자 등록", description = "특정 캠페인에 담당자를 등록합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('CAMPAIGN_ADMIN')")
    public ResponseEntity<ApiResponse<CampaignManagerResponse>> createCampaignManager(
            @Valid @RequestBody CampaignManagerCreateRequest requestDto,
            @AuthenticationPrincipal User currentUser) {
        
        log.info("Creating campaign manager for campaign {} and user {} by admin {}", 
                requestDto.getCampaignId(), requestDto.getUserId(), currentUser.getId());
        
        CampaignManagerResponse campaignManager = campaignManagerService.createCampaignManager(requestDto, currentUser.getId());
        
        return ResponseEntity.ok(ApiResponse.success("캠페인 담당자가 성공적으로 등록되었습니다", campaignManager));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "캠페인 담당자 정보 수정", description = "캠페인 담당자 정보를 수정합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('CAMPAIGN_ADMIN')")
    public ResponseEntity<ApiResponse<CampaignManagerResponse>> updateCampaignManager(
            @Parameter(description = "캠페인 담당자 ID") @PathVariable Long id,
            @Valid @RequestBody CampaignManagerUpdateRequest requestDto) {
        
        log.info("Updating campaign manager with ID: {}", id);
        
        CampaignManagerResponse updatedCampaignManager = campaignManagerService.updateCampaignManager(id, requestDto);
        
        return ResponseEntity.ok(ApiResponse.success("캠페인 담당자 정보가 성공적으로 수정되었습니다", updatedCampaignManager));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "캠페인 담당자 권한 해제", description = "캠페인 담당자 권한을 해제합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('CAMPAIGN_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCampaignManager(
            @Parameter(description = "캠페인 담당자 ID") @PathVariable Long id) {
        
        log.info("Deleting campaign manager with ID: {}", id);
        
        campaignManagerService.deleteCampaignManager(id);
        
        return ResponseEntity.ok(ApiResponse.success("캠페인 담당자 권한이 성공적으로 해제되었습니다"));
    }
    
    @PostMapping("/{id}/restore")
    @Operation(summary = "캠페인 담당자 권한 복원", description = "해제된 캠페인 담당자 권한을 복원합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('CAMPAIGN_ADMIN')")
    public ResponseEntity<ApiResponse<CampaignManagerResponse>> restoreCampaignManager(
            @Parameter(description = "캠페인 담당자 ID") @PathVariable Long id) {
        
        log.info("Restoring campaign manager with ID: {}", id);
        
        CampaignManagerResponse restoredCampaignManager = campaignManagerService.restoreCampaignManager(id);
        
        return ResponseEntity.ok(ApiResponse.success("캠페인 담당자 권한이 성공적으로 복원되었습니다", restoredCampaignManager));
    }
    
    @GetMapping("/campaigns/{campaignId}")
    @Operation(summary = "캠페인의 모든 담당자 조회", description = "특정 캠페인의 모든 담당자를 조회합니다")
    public ResponseEntity<ApiResponse<List<CampaignManagerResponse>>> getCampaignManagers(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId) {
        
        log.debug("Getting all campaign managers for campaign: {}", campaignId);
        
        List<CampaignManagerResponse> campaignManagers = campaignManagerService.getCampaignManagers(campaignId);
        
        return ResponseEntity.ok(ApiResponse.success("캠페인 담당자 목록 조회 성공", campaignManagers));
    }
    
    @GetMapping("/campaigns/{campaignId}/active")
    @Operation(summary = "캠페인의 활성 담당자 조회", description = "특정 캠페인의 활성 담당자만 조회합니다")
    public ResponseEntity<ApiResponse<List<CampaignManagerResponse>>> getActiveCampaignManagers(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId) {
        
        log.debug("Getting active campaign managers for campaign: {}", campaignId);
        
        List<CampaignManagerResponse> campaignManagers = campaignManagerService.getActiveCampaignManagers(campaignId);
        
        return ResponseEntity.ok(ApiResponse.success("활성 캠페인 담당자 목록 조회 성공", campaignManagers));
    }
    
    @GetMapping("/campaigns/{campaignId}/paginated")
    @Operation(summary = "캠페인 담당자 목록 (페이징)", description = "특정 캠페인의 담당자 목록을 페이징하여 조회합니다")
    public ResponseEntity<ApiResponse<Page<CampaignManagerResponse>>> getCampaignManagersPaginated(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId,
            @PageableDefault(size = 10, sort = "assignedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.debug("Getting paginated campaign managers for campaign: {}", campaignId);
        
        Page<CampaignManagerResponse> campaignManagers = campaignManagerService.getCampaignManagers(campaignId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success("페이징된 캠페인 담당자 목록 조회 성공", campaignManagers));
    }
    
    @GetMapping("/users/{userId}")
    @Operation(summary = "유저가 담당하는 모든 캠페인 조회", description = "특정 유저가 담당하는 모든 캠페인을 조회합니다")
    public ResponseEntity<ApiResponse<List<CampaignManagerResponse>>> getUserManagedCampaigns(
            @Parameter(description = "유저 ID") @PathVariable Long userId) {
        
        log.debug("Getting all managed campaigns for user: {}", userId);
        
        List<CampaignManagerResponse> managedCampaigns = campaignManagerService.getUserManagedCampaigns(userId);
        
        return ResponseEntity.ok(ApiResponse.success("유저의 담당 캠페인 목록 조회 성공", managedCampaigns));
    }
    
    @GetMapping("/users/{userId}/active")
    @Operation(summary = "유저가 담당하는 활성 캠페인 조회", description = "특정 유저가 담당하는 활성 캠페인만 조회합니다")
    public ResponseEntity<ApiResponse<List<CampaignManagerResponse>>> getUserActiveManagedCampaigns(
            @Parameter(description = "유저 ID") @PathVariable Long userId) {
        
        log.debug("Getting active managed campaigns for user: {}", userId);
        
        List<CampaignManagerResponse> managedCampaigns = campaignManagerService.getUserActiveManagedCampaigns(userId);
        
        return ResponseEntity.ok(ApiResponse.success("유저의 활성 담당 캠페인 목록 조회 성공", managedCampaigns));
    }
    
    @GetMapping("/users/{userId}/paginated")
    @Operation(summary = "유저의 담당 캠페인 목록 (페이징)", description = "특정 유저의 담당 캠페인 목록을 페이징하여 조회합니다")
    public ResponseEntity<ApiResponse<Page<CampaignManagerResponse>>> getUserManagedCampaignsPaginated(
            @Parameter(description = "유저 ID") @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "assignedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        log.debug("Getting paginated managed campaigns for user: {}", userId);
        
        Page<CampaignManagerResponse> managedCampaigns = campaignManagerService.getUserManagedCampaigns(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success("페이징된 유저의 담당 캠페인 목록 조회 성공", managedCampaigns));
    }
    
    @GetMapping("/campaigns/{campaignId}/users/{userId}")
    @Operation(summary = "특정 캠페인-유저 담당자 관계 조회", description = "특정 캠페인과 유저의 담당자 관계를 조회합니다")
    public ResponseEntity<ApiResponse<CampaignManagerResponse>> getCampaignManager(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId,
            @Parameter(description = "유저 ID") @PathVariable Long userId) {
        
        log.debug("Getting campaign manager for campaign {} and user {}", campaignId, userId);
        
        CampaignManagerResponse campaignManager = campaignManagerService.getCampaignManager(campaignId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("캠페인 담당자 정보 조회 성공", campaignManager));
    }
    
    @GetMapping("/campaigns/{campaignId}/users/{userId}/check")
    @Operation(summary = "캠페인 담당자 여부 확인", description = "특정 유저가 캠페인의 담당자인지 확인합니다")
    public ResponseEntity<ApiResponse<Boolean>> checkCampaignManager(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId,
            @Parameter(description = "유저 ID") @PathVariable Long userId,
            @Parameter(description = "활성 담당자만 확인") @RequestParam(defaultValue = "true") boolean activeOnly) {
        
        log.debug("Checking if user {} is manager of campaign {} (activeOnly: {})", userId, campaignId, activeOnly);
        
        boolean isManager = activeOnly 
                ? campaignManagerService.isActiveCampaignManager(campaignId, userId)
                : campaignManagerService.isCampaignManager(campaignId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("캠페인 담당자 여부 확인 완료", isManager));
    }
    
    @GetMapping("/campaigns/{campaignId}/count")
    @Operation(summary = "캠페인 담당자 수 조회", description = "특정 캠페인의 담당자 수를 조회합니다")
    public ResponseEntity<ApiResponse<Long>> countCampaignManagers(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId) {
        
        log.debug("Counting campaign managers for campaign: {}", campaignId);
        
        long count = campaignManagerService.countCampaignManagers(campaignId);
        
        return ResponseEntity.ok(ApiResponse.success("캠페인 담당자 수 조회 성공", count));
    }
    
    @GetMapping("/users/{userId}/count")
    @Operation(summary = "유저의 담당 캠페인 수 조회", description = "특정 유저가 담당하는 캠페인 수를 조회합니다")
    public ResponseEntity<ApiResponse<Long>> countUserManagedCampaigns(
            @Parameter(description = "유저 ID") @PathVariable Long userId) {
        
        log.debug("Counting managed campaigns for user: {}", userId);
        
        long count = campaignManagerService.countUserManagedCampaigns(userId);
        
        return ResponseEntity.ok(ApiResponse.success("유저의 담당 캠페인 수 조회 성공", count));
    }
}