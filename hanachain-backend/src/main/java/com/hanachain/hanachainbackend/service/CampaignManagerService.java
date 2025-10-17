package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.campaignmanager.CampaignManagerCreateRequest;
import com.hanachain.hanachainbackend.dto.campaignmanager.CampaignManagerResponse;
import com.hanachain.hanachainbackend.dto.campaignmanager.CampaignManagerUpdateRequest;
import com.hanachain.hanachainbackend.entity.CampaignManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CampaignManagerService {
    
    /**
     * 캠페인 담당자 등록
     */
    CampaignManagerResponse createCampaignManager(CampaignManagerCreateRequest requestDto, Long assignedByUserId);

    /**
     * 캠페인 담당자 정보 수정
     */
    CampaignManagerResponse updateCampaignManager(Long id, CampaignManagerUpdateRequest requestDto);

    /**
     * 캠페인 담당자 삭제 (권한 해제)
     */
    void deleteCampaignManager(Long id);

    /**
     * 캠페인 담당자 권한 복원
     */
    CampaignManagerResponse restoreCampaignManager(Long id);

    /**
     * 특정 캠페인의 모든 담당자 조회
     */
    List<CampaignManagerResponse> getCampaignManagers(Long campaignId);

    /**
     * 특정 캠페인의 활성 담당자만 조회
     */
    List<CampaignManagerResponse> getActiveCampaignManagers(Long campaignId);

    /**
     * 특정 유저가 담당하는 모든 캠페인 조회
     */
    List<CampaignManagerResponse> getUserManagedCampaigns(Long userId);

    /**
     * 특정 유저가 담당하는 활성 캠페인만 조회
     */
    List<CampaignManagerResponse> getUserActiveManagedCampaigns(Long userId);

    /**
     * 페이징된 캠페인 담당자 목록 조회
     */
    Page<CampaignManagerResponse> getCampaignManagers(Long campaignId, Pageable pageable);

    /**
     * 페이징된 유저의 담당 캠페인 목록 조회
     */
    Page<CampaignManagerResponse> getUserManagedCampaigns(Long userId, Pageable pageable);

    /**
     * 특정 캠페인과 유저의 담당자 관계 조회
     */
    CampaignManagerResponse getCampaignManager(Long campaignId, Long userId);
    
    /**
     * 특정 캠페인에 유저가 담당자로 등록되어 있는지 확인
     */
    boolean isCampaignManager(Long campaignId, Long userId);
    
    /**
     * 특정 캠페인에 유저가 활성 담당자로 등록되어 있는지 확인
     */
    boolean isActiveCampaignManager(Long campaignId, Long userId);
    
    /**
     * 특정 캠페인의 담당자 수 조회
     */
    long countCampaignManagers(Long campaignId);
    
    /**
     * 특정 유저의 담당 캠페인 수 조회
     */
    long countUserManagedCampaigns(Long userId);
    
    /**
     * 캠페인 담당자 엔티티를 DTO로 변환
     */
    CampaignManagerResponse convertToDto(CampaignManager campaignManager);
}