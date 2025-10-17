package com.hanachain.hanachainbackend.controller.admin;

import com.hanachain.hanachainbackend.dto.campaign.CampaignCreateRequest;
import com.hanachain.hanachainbackend.dto.campaign.CampaignDetailResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignImageUploadResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignListResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignUpdateRequest;
import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.donation.DonationResponse;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.Donation;
import com.hanachain.hanachainbackend.repository.DonationRepository;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import com.hanachain.hanachainbackend.service.CampaignService;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/campaigns")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Campaign", description = "관리자 캠페인 관리 API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCampaignController {

    private final CampaignService campaignService;
    private final DonationRepository donationRepository;
    
    @Operation(
        summary = "관리자용 캠페인 목록 조회",
        description = "관리자가 모든 캠페인(삭제된 항목 포함)을 조회할 수 있습니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CampaignListResponse>>> getAdminCampaigns(
            @RequestParam(required = false) Campaign.CampaignCategory category,
            @RequestParam(required = false) Campaign.CampaignStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        
        log.info("🔍 관리자 캠페인 목록 조회 요청 - category: {}, status: {}, keyword: {}, page: {}",
            category, status, keyword, pageable.getPageNumber());
        
        Page<CampaignListResponse> campaigns = campaignService.getAdminCampaigns(category, status, keyword, pageable);
        
        log.info("✅ 관리자 캠페인 목록 조회 완료 - 총 {}개, 페이지 {}/{}",
            campaigns.getTotalElements(), campaigns.getNumber() + 1, campaigns.getTotalPages());
        
        return ResponseEntity.ok(ApiResponse.success(
            "관리자 캠페인 목록을 성공적으로 조회했습니다.",
            campaigns
        ));
    }
    
    @Operation(
        summary = "관리자용 캠페인 상세 조회",
        description = "관리자가 특정 캠페인의 상세 정보(삭제된 항목 포함)를 조회할 수 있습니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> getAdminCampaign(
            @Parameter(description = "캠페인 ID") @PathVariable Long id) {
        
        log.info("🔍 관리자 캠페인 상세 조회 요청 - campaignId: {}", id);
        
        CampaignDetailResponse campaign = campaignService.getAdminCampaignDetail(id);
        
        log.info("✅ 관리자 캠페인 상세 조회 완료 - campaignId: {}, title: {}", 
            id, campaign.getTitle());
        
        return ResponseEntity.ok(ApiResponse.success(
            "관리자 캠페인 상세 정보를 성공적으로 조회했습니다.",
            campaign
        ));
    }
    
    @Operation(
        summary = "캠페인 생성",
        description = "관리자가 새로운 캠페인을 생성할 수 있습니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> createCampaign(
            @Valid @RequestBody CampaignCreateRequest createDto) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.info("📝 관리자 캠페인 생성 요청 - userId: {}, title: {}", currentUserId, createDto.getTitle());
        log.info("📋 CampaignCreateRequest 상세 정보:");
        log.info("  - title: '{}' (길이: {})", createDto.getTitle(), createDto.getTitle() != null ? createDto.getTitle().length() : "null");
        log.info("  - subtitle: '{}' (길이: {})", createDto.getSubtitle(), createDto.getSubtitle() != null ? createDto.getSubtitle().length() : "null");
        log.info("  - description: '{}' (길이: {})", 
            createDto.getDescription() != null ? createDto.getDescription().substring(0, Math.min(100, createDto.getDescription().length())) + "..." : "null",
            createDto.getDescription() != null ? createDto.getDescription().length() : "null");
        log.info("  - organizer: '{}' (길이: {})", createDto.getOrganizer(), createDto.getOrganizer() != null ? createDto.getOrganizer().length() : "null");
        log.info("  - targetAmount: {}", createDto.getTargetAmount());
        log.info("  - imageUrl: '{}' (길이: {})", createDto.getImageUrl(), createDto.getImageUrl() != null ? createDto.getImageUrl().length() : "null");
        log.info("  - category: {}", createDto.getCategory());
        log.info("  - startDate: {}", createDto.getStartDate());
        log.info("  - endDate: {}", createDto.getEndDate());
        
        CampaignDetailResponse campaign = campaignService.createCampaign(createDto, currentUserId);
        
        log.info("✅ 관리자 캠페인 생성 완료 - campaignId: {}, title: {}", 
            campaign.getId(), campaign.getTitle());
        
        // 블록체인 등록 상태에 따른 메시지 생성
        String message = "캠페인이 성공적으로 생성되었습니다.";
        if (campaign.getBlockchainStatus() != null) {
            switch (campaign.getBlockchainStatus()) {
                case BLOCKCHAIN_PENDING:
                    message += " 블록체인 등록이 진행 중입니다.";
                    break;
                case BLOCKCHAIN_PROCESSING:
                    message += " 블록체인 처리가 진행 중입니다.";
                    break;
                case ACTIVE:
                    message += " 블록체인에 성공적으로 등록되었습니다.";
                    break;
                case BLOCKCHAIN_FAILED:
                    message += " 블록체인 등록에 실패했습니다. 나중에 다시 시도할 수 있습니다.";
                    break;
                case NONE:
                    message += " (수혜자 주소 설정 후 블록체인 등록 가능)";
                    break;
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success(message, campaign));
    }
    
    @Operation(
        summary = "캠페인 수정",
        description = "관리자가 캠페인 정보를 수정할 수 있습니다."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> updateCampaign(
            @Parameter(description = "캠페인 ID") @PathVariable Long id,
            @Valid @RequestBody CampaignUpdateRequest updateDto) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        log.info("📝 관리자 캠페인 수정 요청 - campaignId: {}, userId: {}", id, currentUserId);
        
        CampaignDetailResponse campaign = campaignService.updateCampaignAsAdmin(id, updateDto, currentUserId);
        
        log.info("✅ 관리자 캠페인 수정 완료 - campaignId: {}, title: {}", 
            id, campaign.getTitle());
        
        return ResponseEntity.ok(ApiResponse.success(
            "캠페인이 성공적으로 수정되었습니다.",
            campaign
        ));
    }
    
    @Operation(
        summary = "캠페인 소프트 삭제",
        description = "관리자가 캠페인을 소프트 삭제할 수 있습니다. 실제로 삭제되지 않고 deleted_at 필드가 설정됩니다."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> softDeleteCampaign(
            @Parameter(description = "캠페인 ID") @PathVariable Long id) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        log.info("🗑️ 관리자 캠페인 소프트 삭제 요청 - campaignId: {}, userId: {}", id, currentUserId);
        
        campaignService.softDeleteCampaignAsAdmin(id, currentUserId);
        
        log.info("✅ 관리자 캠페인 소프트 삭제 완료 - campaignId: {}", id);
        
        return ResponseEntity.ok(ApiResponse.success(
            "캠페인이 성공적으로 삭제되었습니다."
        ));
    }
    
    @Operation(
        summary = "캠페인 복구",
        description = "관리자가 소프트 삭제된 캠페인을 복구할 수 있습니다."
    )
    @PatchMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> restoreCampaign(
            @Parameter(description = "캠페인 ID") @PathVariable Long id) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        log.info("🔄 관리자 캠페인 복구 요청 - campaignId: {}, userId: {}", id, currentUserId);
        
        CampaignDetailResponse campaign = campaignService.restoreCampaignAsAdmin(id, currentUserId);
        
        log.info("✅ 관리자 캠페인 복구 완료 - campaignId: {}, title: {}", 
            id, campaign.getTitle());
        
        return ResponseEntity.ok(ApiResponse.success(
            "캠페인이 성공적으로 복구되었습니다.",
            campaign
        ));
    }
    
    @Operation(
        summary = "삭제된 캠페인 목록 조회",
        description = "관리자가 삭제된 캠페인들의 목록을 조회할 수 있습니다."
    )
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<Page<CampaignListResponse>>> getDeletedCampaigns(
            @PageableDefault(size = 20, sort = "deletedAt") Pageable pageable) {
        
        log.info("🔍 삭제된 캠페인 목록 조회 요청 - page: {}", pageable.getPageNumber());
        
        Page<CampaignListResponse> deletedCampaigns = campaignService.getDeletedCampaigns(pageable);
        
        log.info("✅ 삭제된 캠페인 목록 조회 완료 - 총 {}개", deletedCampaigns.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(
            "삭제된 캠페인 목록을 성공적으로 조회했습니다.",
            deletedCampaigns
        ));
    }
    
    @Operation(
        summary = "캠페인 상태 변경",
        description = "관리자가 캠페인의 상태를 변경할 수 있습니다."
    )
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> updateCampaignStatus(
            @Parameter(description = "캠페인 ID") @PathVariable Long id,
            @Parameter(description = "새로운 상태") @RequestParam Campaign.CampaignStatus status) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        log.info("📝 관리자 캠페인 상태 변경 요청 - campaignId: {}, newStatus: {}, userId: {}", 
            id, status, currentUserId);
        
        CampaignDetailResponse campaign = campaignService.updateCampaignStatusAsAdmin(id, status, currentUserId);
        
        log.info("✅ 관리자 캠페인 상태 변경 완료 - campaignId: {}, status: {}", id, status);
        
        return ResponseEntity.ok(ApiResponse.success(
            "캠페인 상태가 성공적으로 변경되었습니다.",
            campaign
        ));
    }
    
    @Operation(
        summary = "수혜자 주소 업데이트",
        description = "관리자가 캠페인의 수혜자 블록체인 주소를 업데이트합니다."
    )
    @PatchMapping("/{id}/beneficiary-address")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> updateBeneficiaryAddress(
            @Parameter(description = "캠페인 ID") @PathVariable Long id,
            @Parameter(description = "수혜자 이더리움 주소") @RequestParam String address) {
        
        Long currentUserId = SecurityUtils.getCurrentUser()
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        log.info("💰 수혜자 주소 업데이트 요청 - campaignId: {}, address: {}, userId: {}", 
            id, address, currentUserId);
        
        CampaignDetailResponse campaign = campaignService.updateBeneficiaryAddress(id, address, currentUserId);
        
        log.info("✅ 수혜자 주소 업데이트 완료 - campaignId: {}, address: {}", id, address);
        
        return ResponseEntity.ok(ApiResponse.success(
            "수혜자 주소가 성공적으로 업데이트되었습니다.",
            campaign
        ));
    }

    @Operation(
        summary = "캠페인 이미지 업로드",
        description = "관리자가 캠페인 에디터에서 사용할 이미지를 업로드합니다. 이미지는 Base64로 인코딩되어 데이터 URL로 반환됩니다."
    )
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CampaignImageUploadResponse>> uploadCampaignImage(
            @Parameter(description = "업로드할 이미지 파일") @RequestParam("image") MultipartFile image) {
        
        log.info("📸 관리자 캠페인 이미지 업로드 요청 - 파일명: {}, 크기: {}", 
            image.getOriginalFilename(), image.getSize());
        
        try {
            CampaignImageUploadResponse uploadResponse = campaignService.uploadCampaignImage(image);
            
            log.info("✅ 관리자 캠페인 이미지 업로드 완료 - 파일명: {}, 데이터 URL 크기: {} 문자", 
                image.getOriginalFilename(), uploadResponse.getImageUrl().length());
            
            return ResponseEntity.ok(ApiResponse.success(
                "캠페인 이미지가 성공적으로 업로드되었습니다.",
                uploadResponse
            ));
            
        } catch (Exception e) {
            log.error("❌ 관리자 캠페인 이미지 업로드 실패 - 파일명: {}", image.getOriginalFilename(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("이미지 업로드에 실패했습니다: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "캠페인의 FDS 검증 미통과 거래 목록 조회",
        description = "관리자가 캠페인에 속한 FDS 검증을 통과하지 못한 거래 목록을 조회합니다. " +
                     "캠페인 마감 시 이러한 거래가 있으면 마감이 차단되므로, 관리자가 해당 거래를 확인하고 환불 처리 후 재시도할 수 있습니다."
    )
    @GetMapping("/{campaignId}/unverified-fds-donations")
    public ResponseEntity<ApiResponse<List<DonationResponse>>> getUnverifiedFdsDonations(
            @Parameter(description = "캠페인 ID") @PathVariable Long campaignId) {

        log.info("🔍 FDS 검증 미통과 거래 목록 조회 요청 - campaignId: {}", campaignId);

        // FDS 검증을 통과하지 못한 거래 목록 조회
        List<Donation> unverifiedDonations = donationRepository.findUnverifiedFdsDonationsByCampaignId(campaignId);

        // DTO로 변환
        List<DonationResponse> donationDtos = unverifiedDonations.stream()
                .map(DonationResponse::fromEntity)
                .collect(Collectors.toList());

        log.info("✅ FDS 검증 미통과 거래 목록 조회 완료 - campaignId: {}, 미통과 건수: {}",
            campaignId, donationDtos.size());

        String message = donationDtos.isEmpty()
            ? "FDS 검증 미통과 거래가 없습니다."
            : String.format("FDS 검증 미통과 거래 %d건을 조회했습니다.", donationDtos.size());

        return ResponseEntity.ok(ApiResponse.success(message, donationDtos));
    }
}