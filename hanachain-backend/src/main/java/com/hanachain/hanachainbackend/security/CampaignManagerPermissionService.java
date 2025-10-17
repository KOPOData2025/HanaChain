package com.hanachain.hanachainbackend.security;

import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.repository.CampaignManagerRepository;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CampaignManagerPermissionService {
    
    private final CampaignManagerRepository campaignManagerRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    
    /**
     * 사용자가 특정 캠페인의 관리 권한을 가지고 있는지 확인
     */
    public boolean hasManagementPermission(Long userId, Long campaignId) {
        if (userId == null || campaignId == null) {
            return false;
        }
        
        try {
            // 사용자 조회
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return false;
            }
            
            // 시스템 관리자는 모든 권한 보유
            if (user.getRole().isSystemLevelAdmin()) {
                log.debug("User {} has system-level admin access to campaign {}", userId, campaignId);
                return true;
            }
            
            // 캠페인 조회
            Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
            if (campaign == null) {
                return false;
            }
            
            // 캠페인 생성자인지 확인
            if (campaign.getUser().getId().equals(userId)) {
                log.debug("User {} is the creator of campaign {}", userId, campaignId);
                return true;
            }
            
            // 활성 담당자인지 확인
            boolean isActiveCampaignManager = campaignManagerRepository.existsByCampaignIdAndUserIdAndStatus(
                    campaignId, userId, com.hanachain.hanachainbackend.entity.CampaignManager.ManagerStatus.ACTIVE);
            
            if (isActiveCampaignManager) {
                log.debug("User {} is an active campaign manager for campaign {}", userId, campaignId);
                return true;
            }
            
            log.debug("User {} does not have management permission for campaign {}", userId, campaignId);
            return false;
            
        } catch (Exception e) {
            log.error("Error checking management permission for user {} and campaign {}: {}", 
                    userId, campaignId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 사용자가 특정 캠페인을 생성한 사람인지 확인
     */
    public boolean isCampaignCreator(Long userId, Long campaignId) {
        if (userId == null || campaignId == null) {
            return false;
        }
        
        try {
            Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
            return campaign != null && campaign.getUser().getId().equals(userId);
        } catch (Exception e) {
            log.error("Error checking campaign creator for user {} and campaign {}: {}", 
                    userId, campaignId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 사용자가 특정 캠페인의 활성 담당자인지 확인
     */
    public boolean isActiveCampaignManager(Long userId, Long campaignId) {
        if (userId == null || campaignId == null) {
            return false;
        }
        
        try {
            return campaignManagerRepository.existsByCampaignIdAndUserIdAndStatus(
                    campaignId, userId, com.hanachain.hanachainbackend.entity.CampaignManager.ManagerStatus.ACTIVE);
        } catch (Exception e) {
            log.error("Error checking active campaign manager for user {} and campaign {}: {}", 
                    userId, campaignId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 사용자가 시스템 관리자인지 확인
     */
    public boolean isSystemAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        
        try {
            User user = userRepository.findById(userId).orElse(null);
            return user != null && user.getRole().isSystemLevelAdmin();
        } catch (Exception e) {
            log.error("Error checking system admin for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 캠페인 관리 권한 부족 시 예외 발생
     */
    public void requireManagementPermission(Long userId, Long campaignId) {
        if (!hasManagementPermission(userId, campaignId)) {
            throw new IllegalStateException("캠페인 관리 권한이 없습니다. 캠페인 ID: " + campaignId + ", 사용자 ID: " + userId);
        }
    }
    
    /**
     * 캠페인 생성자 권한 부족 시 예외 발생
     */
    public void requireCreatorPermission(Long userId, Long campaignId) {
        if (!isCampaignCreator(userId, campaignId)) {
            throw new IllegalStateException("캠페인 생성자만 접근할 수 있습니다. 캠페인 ID: " + campaignId + ", 사용자 ID: " + userId);
        }
    }
    
    /**
     * 시스템 관리자 권한 부족 시 예외 발생
     */
    public void requireSystemAdminPermission(Long userId) {
        if (!isSystemAdmin(userId)) {
            throw new IllegalStateException("시스템 관리자 권한이 필요합니다. 사용자 ID: " + userId);
        }
    }
}