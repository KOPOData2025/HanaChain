package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.campaign.CampaignCreateRequest;
import com.hanachain.hanachainbackend.dto.campaign.CampaignDetailResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignFundraisingStats;
import com.hanachain.hanachainbackend.dto.campaign.CampaignImageUploadResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignListResponse;
import com.hanachain.hanachainbackend.dto.campaign.CampaignUpdateRequest;
import com.hanachain.hanachainbackend.entity.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface CampaignService {
    
    /**
     * 새 캠페인을 생성합니다.
     */
    Campaign createCampaign(Long userId, CampaignCreateRequest request);
    
    /**
     * 캠페인을 수정합니다.
     */
    Campaign updateCampaign(Long campaignId, Long userId, CampaignUpdateRequest request);
    
    /**
     * 캠페인을 삭제합니다.
     */
    void deleteCampaign(Long campaignId, Long userId);
    
    /**
     * 캠페인 상세 정보를 조회합니다.
     */
    CampaignDetailResponse getCampaignDetail(Long campaignId);
    
    /**
     * 공개 캠페인 목록을 조회합니다.
     */
    Page<CampaignListResponse> getPublicCampaigns(Pageable pageable);
    
    /**
     * 통합 필터를 사용하여 캠페인 목록을 조회합니다.
     */
    Page<CampaignListResponse> getCampaignsWithFilters(
            Campaign.CampaignCategory category, 
            Campaign.CampaignStatus status, 
            String keyword, 
            String sort, 
            Pageable pageable);
    
    /**
     * 카테고리별 캠페인 목록을 조회합니다.
     */
    Page<CampaignListResponse> getCampaignsByCategory(Campaign.CampaignCategory category, Pageable pageable);
    
    /**
     * 사용자의 캠페인 목록을 조회합니다.
     */
    Page<CampaignListResponse> getUserCampaigns(Long userId, Pageable pageable);
    
    /**
     * 키워드로 캠페인을 검색합니다.
     */
    Page<CampaignListResponse> searchCampaigns(String keyword, Pageable pageable);
    
    /**
     * 인기 캠페인 목록을 조회합니다.
     */
    Page<CampaignListResponse> getPopularCampaigns(Pageable pageable);
    
    /**
     * 최근 캠페인 목록을 조회합니다.
     */
    Page<CampaignListResponse> getRecentCampaigns(Pageable pageable);
    
    /**
     * 캠페인 상태를 변경합니다.
     */
    Campaign updateCampaignStatus(Long campaignId, Campaign.CampaignStatus status);
    
    /**
     * 만료된 캠페인들의 상태를 완료로 변경합니다.
     */
    void completeExpiredCampaigns();
    
    /**
     * 캠페인 이미지를 업로드합니다. (Base64 인코딩하여 CLOB 저장)
     */
    CampaignImageUploadResponse uploadCampaignImage(MultipartFile image);
    
    // ===== 관리자 전용 메서드들 =====
    
    /**
     * 관리자용 캠페인 목록 조회 (삭제된 항목 포함)
     */
    Page<CampaignListResponse> getAdminCampaigns(
            Campaign.CampaignCategory category,
            Campaign.CampaignStatus status,
            String keyword,
            Pageable pageable);
    
    /**
     * 관리자용 캠페인 상세 조회 (삭제된 항목 포함)
     */
    CampaignDetailResponse getAdminCampaignDetail(Long campaignId);
    
    /**
     * 관리자가 캠페인을 생성합니다.
     */
    CampaignDetailResponse createCampaign(CampaignCreateRequest createDto, Long adminUserId);
    
    /**
     * 관리자가 캠페인을 수정합니다.
     */
    CampaignDetailResponse updateCampaignAsAdmin(Long campaignId, CampaignUpdateRequest updateDto, Long adminUserId);
    
    /**
     * 관리자가 캠페인을 소프트 삭제합니다.
     */
    void softDeleteCampaignAsAdmin(Long campaignId, Long adminUserId);
    
    /**
     * 관리자가 캠페인을 복구합니다.
     */
    CampaignDetailResponse restoreCampaignAsAdmin(Long campaignId, Long adminUserId);
    
    /**
     * 관리자가 캠페인 상태를 변경합니다.
     */
    CampaignDetailResponse updateCampaignStatusAsAdmin(Long campaignId, Campaign.CampaignStatus status, Long adminUserId);
    
    /**
     * 관리자가 수혜자 주소를 업데이트합니다.
     */
    CampaignDetailResponse updateBeneficiaryAddress(Long campaignId, String address, Long adminUserId);
    
    /**
     * 삭제된 캠페인 목록을 조회합니다.
     */
    Page<CampaignListResponse> getDeletedCampaigns(Pageable pageable);
    
    // ===== 블록체인 통합 메서드들 =====
    
    /**
     * 캠페인을 블록체인과 함께 생성합니다 (기존 캠페인의 블록체인 등록 포함)
     */
    Campaign createCampaignWithBlockchain(Long userId, CampaignCreateRequest request, boolean forceBlockchainRegistration);
    
    /**
     * 캠페인의 블록체인 상태를 조회합니다
     */
    CampaignDetailResponse getBlockchainStatus(Long campaignId);
    
    /**
     * 블록체인에서 캠페인을 완료 처리합니다
     */
    Campaign finalizeCampaignOnBlockchain(Long campaignId, Long userId);
    
    /**
     * 실패한 블록체인 작업을 재시도합니다
     */
    Campaign retryBlockchainOperation(Long campaignId, Long userId);
    
    /**
     * 블록체인과 데이터베이스 상태를 동기화합니다
     */
    void syncBlockchainStatus(Long campaignId);

    /**
     * 캠페인 담당자용 모금 통계를 조회합니다 (담당자 전용)
     * @param campaignId 캠페인 ID
     * @param userId 요청 사용자 ID (담당자 권한 확인용)
     * @return 모금 통계 정보
     */
    CampaignFundraisingStats getCampaignFundraisingStats(Long campaignId, Long userId);
}
