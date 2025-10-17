package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.dto.campaignmanager.CampaignManagerCreateRequest;
import com.hanachain.hanachainbackend.dto.campaignmanager.CampaignManagerResponse;
import com.hanachain.hanachainbackend.dto.campaignmanager.CampaignManagerUpdateRequest;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.CampaignManager;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.repository.CampaignManagerRepository;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.service.CampaignManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CampaignManagerServiceImpl implements CampaignManagerService {
    
    private final CampaignManagerRepository campaignManagerRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public CampaignManagerResponse createCampaignManager(CampaignManagerCreateRequest requestDto, Long assignedByUserId) {
        log.info("Creating campaign manager for campaign {} and user {} assigned by user {}", 
                requestDto.getCampaignId(), requestDto.getUserId(), assignedByUserId);
        
        // 캠페인 존재 확인
        Campaign campaign = campaignRepository.findById(requestDto.getCampaignId())
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다: " + requestDto.getCampaignId()));
        
        // 유저 존재 확인
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다: " + requestDto.getUserId()));
        
        // 할당자 존재 확인
        User assignedByUser = userRepository.findById(assignedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("할당자를 찾을 수 없습니다: " + assignedByUserId));
        
        // 이미 담당자로 등록되어 있는지 확인
        if (campaignManagerRepository.existsByCampaignIdAndUserIdAndStatus(
                requestDto.getCampaignId(), requestDto.getUserId(), CampaignManager.ManagerStatus.ACTIVE)) {
            throw new IllegalStateException("이미 해당 캠페인의 담당자로 등록되어 있습니다");
        }
        
        // 캠페인 매니저 생성
        CampaignManager campaignManager = CampaignManager.builder()
                .campaign(campaign)
                .user(user)
                .role(requestDto.getRole())
                .status(CampaignManager.ManagerStatus.ACTIVE)
                .assignedAt(LocalDateTime.now())
                .assignedBy(assignedByUser)
                .notes(requestDto.getNotes())
                .build();
        
        CampaignManager savedCampaignManager = campaignManagerRepository.save(campaignManager);
        log.info("Campaign manager created with ID: {}", savedCampaignManager.getId());
        
        return convertToDto(savedCampaignManager);
    }
    
    @Override
    @Transactional
    public CampaignManagerResponse updateCampaignManager(Long id, CampaignManagerUpdateRequest requestDto) {
        log.info("Updating campaign manager with ID: {}", id);
        
        CampaignManager campaignManager = campaignManagerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("캠페인 담당자를 찾을 수 없습니다: " + id));
        
        // 업데이트할 필드들 설정
        if (requestDto.getRole() != null) {
            campaignManager.setRole(requestDto.getRole());
        }
        
        if (requestDto.getStatus() != null) {
            campaignManager.setStatus(requestDto.getStatus());
            if (requestDto.getStatus() == CampaignManager.ManagerStatus.REVOKED && campaignManager.getRevokedAt() == null) {
                campaignManager.setRevokedAt(LocalDateTime.now());
            } else if (requestDto.getStatus() == CampaignManager.ManagerStatus.ACTIVE) {
                campaignManager.setRevokedAt(null);
            }
        }
        
        if (requestDto.getNotes() != null) {
            campaignManager.setNotes(requestDto.getNotes());
        }
        
        CampaignManager updatedCampaignManager = campaignManagerRepository.save(campaignManager);
        log.info("Campaign manager updated with ID: {}", updatedCampaignManager.getId());
        
        return convertToDto(updatedCampaignManager);
    }
    
    @Override
    @Transactional
    public void deleteCampaignManager(Long id) {
        log.info("Deleting (revoking) campaign manager with ID: {}", id);
        
        CampaignManager campaignManager = campaignManagerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("캠페인 담당자를 찾을 수 없습니다: " + id));
        
        campaignManager.revoke();
        campaignManagerRepository.save(campaignManager);
        
        log.info("Campaign manager revoked with ID: {}", id);
    }
    
    @Override
    @Transactional
    public CampaignManagerResponse restoreCampaignManager(Long id) {
        log.info("Restoring campaign manager with ID: {}", id);
        
        CampaignManager campaignManager = campaignManagerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("캠페인 담당자를 찾을 수 없습니다: " + id));
        
        campaignManager.restore();
        CampaignManager restoredCampaignManager = campaignManagerRepository.save(campaignManager);
        
        log.info("Campaign manager restored with ID: {}", id);
        return convertToDto(restoredCampaignManager);
    }
    
    @Override
    public List<CampaignManagerResponse> getCampaignManagers(Long campaignId) {
        log.debug("Getting all campaign managers for campaign: {}", campaignId);
        
        List<CampaignManager> campaignManagers = campaignManagerRepository.findByCampaignId(campaignId);
        return campaignManagers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CampaignManagerResponse> getActiveCampaignManagers(Long campaignId) {
        log.debug("Getting active campaign managers for campaign: {}", campaignId);
        
        List<CampaignManager> campaignManagers = campaignManagerRepository.findActiveByCampaignId(campaignId);
        return campaignManagers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CampaignManagerResponse> getUserManagedCampaigns(Long userId) {
        log.debug("Getting all managed campaigns for user: {}", userId);
        
        List<CampaignManager> campaignManagers = campaignManagerRepository.findByUserId(userId);
        return campaignManagers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CampaignManagerResponse> getUserActiveManagedCampaigns(Long userId) {
        log.debug("Getting active managed campaigns for user: {}", userId);
        
        List<CampaignManager> campaignManagers = campaignManagerRepository.findActiveByUserId(userId);
        return campaignManagers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<CampaignManagerResponse> getCampaignManagers(Long campaignId, Pageable pageable) {
        log.debug("Getting paginated campaign managers for campaign: {}", campaignId);
        
        Page<CampaignManager> campaignManagers = campaignManagerRepository.findByCampaignId(campaignId, pageable);
        return campaignManagers.map(this::convertToDto);
    }
    
    @Override
    public Page<CampaignManagerResponse> getUserManagedCampaigns(Long userId, Pageable pageable) {
        log.debug("Getting paginated managed campaigns for user: {}", userId);
        
        Page<CampaignManager> campaignManagers = campaignManagerRepository.findByUserId(userId, pageable);
        return campaignManagers.map(this::convertToDto);
    }
    
    @Override
    public CampaignManagerResponse getCampaignManager(Long campaignId, Long userId) {
        log.debug("Getting campaign manager for campaign {} and user {}", campaignId, userId);
        
        CampaignManager campaignManager = campaignManagerRepository
                .findByCampaignIdAndUserId(campaignId, userId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인 담당자를 찾을 수 없습니다. 캠페인 ID: " + campaignId + ", 유저 ID: " + userId));
        
        return convertToDto(campaignManager);
    }
    
    @Override
    public boolean isCampaignManager(Long campaignId, Long userId) {
        return campaignManagerRepository.findByCampaignIdAndUserId(campaignId, userId).isPresent();
    }
    
    @Override
    public boolean isActiveCampaignManager(Long campaignId, Long userId) {
        return campaignManagerRepository.existsByCampaignIdAndUserIdAndStatus(
                campaignId, userId, CampaignManager.ManagerStatus.ACTIVE);
    }
    
    @Override
    public long countCampaignManagers(Long campaignId) {
        return campaignManagerRepository.countActiveByCampaignId(campaignId);
    }
    
    @Override
    public long countUserManagedCampaigns(Long userId) {
        return campaignManagerRepository.countActiveByUserId(userId);
    }
    
    @Override
    public CampaignManagerResponse convertToDto(CampaignManager campaignManager) {
        if (campaignManager == null) {
            return null;
        }

        return CampaignManagerResponse.builder()
                .id(campaignManager.getId())
                .campaignId(campaignManager.getCampaign().getId())
                .campaignTitle(campaignManager.getCampaign().getTitle())
                .userId(campaignManager.getUser().getId())
                .userName(campaignManager.getUser().getName())
                .userEmail(campaignManager.getUser().getEmail())
                .userNickname(campaignManager.getUser().getNickname())
                .role(campaignManager.getRole())
                .status(campaignManager.getStatus())
                .assignedAt(campaignManager.getAssignedAt())
                .revokedAt(campaignManager.getRevokedAt())
                .assignedByUserId(campaignManager.getAssignedBy().getId())
                .assignedByUserName(campaignManager.getAssignedBy().getName())
                .notes(campaignManager.getNotes())
                .build();
    }
}