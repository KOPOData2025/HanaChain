package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.CampaignManager;
import com.hanachain.hanachainbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignManagerRepository extends JpaRepository<CampaignManager, Long> {
    
    /**
     * 특정 캠페인의 모든 담당자 조회
     */
    List<CampaignManager> findByCampaignId(Long campaignId);
    
    /**
     * 특정 캠페인의 활성 담당자만 조회
     */
    @Query("SELECT cm FROM CampaignManager cm WHERE cm.campaign.id = :campaignId AND cm.status = 'ACTIVE'")
    List<CampaignManager> findActiveByCampaignId(@Param("campaignId") Long campaignId);
    
    /**
     * 특정 유저가 담당하는 모든 캠페인 조회
     */
    List<CampaignManager> findByUserId(Long userId);
    
    /**
     * 특정 유저가 담당하는 활성 캠페인만 조회
     */
    @Query("SELECT cm FROM CampaignManager cm WHERE cm.user.id = :userId AND cm.status = 'ACTIVE'")
    List<CampaignManager> findActiveByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 캠페인과 유저의 담당자 관계 조회
     */
    Optional<CampaignManager> findByCampaignIdAndUserId(Long campaignId, Long userId);
    
    /**
     * 특정 캠페인과 유저의 활성 담당자 관계 조회
     */
    @Query("SELECT cm FROM CampaignManager cm WHERE cm.campaign.id = :campaignId AND cm.user.id = :userId AND cm.status = 'ACTIVE'")
    Optional<CampaignManager> findActiveByCampaignIdAndUserId(@Param("campaignId") Long campaignId, @Param("userId") Long userId);
    
    /**
     * 특정 캠페인에 유저가 담당자로 등록되어 있는지 확인
     */
    boolean existsByCampaignIdAndUserIdAndStatus(Long campaignId, Long userId, CampaignManager.ManagerStatus status);
    
    /**
     * 특정 캠페인의 담당자 수 조회
     */
    @Query("SELECT COUNT(cm) FROM CampaignManager cm WHERE cm.campaign.id = :campaignId AND cm.status = 'ACTIVE'")
    long countActiveByCampaignId(@Param("campaignId") Long campaignId);
    
    /**
     * 특정 유저의 담당 캠페인 수 조회
     */
    @Query("SELECT COUNT(cm) FROM CampaignManager cm WHERE cm.user.id = :userId AND cm.status = 'ACTIVE'")
    long countActiveByUserId(@Param("userId") Long userId);
    
    /**
     * 페이징된 캠페인 담당자 목록 조회
     */
    Page<CampaignManager> findByCampaignId(Long campaignId, Pageable pageable);
    
    /**
     * 페이징된 유저의 담당 캠페인 목록 조회
     */
    Page<CampaignManager> findByUserId(Long userId, Pageable pageable);
}