package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    
    // 기본 조회 메서드들 (삭제되지 않은 항목만)
    @Query("SELECT c FROM Campaign c WHERE c.status = :status AND c.deletedAt IS NULL")
    Page<Campaign> findByStatus(@Param("status") Campaign.CampaignStatus status, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.category = :category AND c.status = 'ACTIVE' AND c.deletedAt IS NULL")
    Page<Campaign> findByCategoryAndActive(@Param("category") Campaign.CampaignCategory category, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.user.id = :userId AND c.deletedAt IS NULL")
    Page<Campaign> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' AND c.startDate <= :now AND c.endDate >= :now AND c.deletedAt IS NULL")
    Page<Campaign> findActiveCampaigns(@Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE (c.title LIKE %:keyword% OR c.description LIKE %:keyword%) AND c.deletedAt IS NULL")
    Page<Campaign> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.endDate < :now AND c.status = 'ACTIVE' AND c.deletedAt IS NULL")
    List<Campaign> findExpiredCampaigns(@Param("now") LocalDateTime now);
    
    @Query("SELECT SUM(c.currentAmount) FROM Campaign c WHERE c.status = 'COMPLETED' AND c.deletedAt IS NULL")
    BigDecimal getTotalRaisedAmount();
    
    @Query("SELECT COUNT(c) FROM Campaign c WHERE c.status = :status AND c.deletedAt IS NULL")
    long countByStatus(@Param("status") Campaign.CampaignStatus status);
    
    @Query("SELECT c FROM Campaign c WHERE c.deletedAt IS NULL ORDER BY c.currentAmount DESC")
    Page<Campaign> findTopFundedCampaigns(Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE c.status = 'ACTIVE' AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<Campaign> findRecentActiveCampaigns(Pageable pageable);
    
    @Query("SELECT c FROM Campaign c WHERE " +
           "(:category IS NULL OR c.category = :category) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:keyword IS NULL OR c.title LIKE %:keyword% OR c.description LIKE %:keyword%) AND " +
           "c.deletedAt IS NULL " +
           "ORDER BY " +
           "CASE WHEN :sort = 'popular' THEN c.currentAmount END DESC, " +
           "CASE WHEN :sort = 'progress' THEN (c.currentAmount * 100.0 / c.targetAmount) END DESC, " +
           "CASE WHEN :sort = 'recent' OR :sort IS NULL THEN c.createdAt END DESC")
    Page<Campaign> findWithFilters(
        @Param("category") Campaign.CampaignCategory category,
        @Param("status") Campaign.CampaignStatus status,
        @Param("keyword") String keyword,
        @Param("sort") String sort,
        Pageable pageable);
    
    // 관리자용 메서드들 (삭제된 항목 포함)
    @Query("SELECT c FROM Campaign c WHERE " +
           "(:category IS NULL OR c.category = :category) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:keyword IS NULL OR c.title LIKE %:keyword% OR c.description LIKE %:keyword%) " +
           "ORDER BY c.createdAt DESC")
    Page<Campaign> findAllWithFiltersForAdmin(
        @Param("category") Campaign.CampaignCategory category,
        @Param("status") Campaign.CampaignStatus status,
        @Param("keyword") String keyword,
        Pageable pageable);
    
    // ID로 조회 (삭제된 항목 포함) - 관리자용
    @Query("SELECT c FROM Campaign c WHERE c.id = :id")
    Optional<Campaign> findByIdForAdmin(@Param("id") Long id);
    
    // ID로 조회 (삭제되지 않은 항목만) - 일반용
    @Query("SELECT c FROM Campaign c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Campaign> findByIdAndNotDeleted(@Param("id") Long id);
    
    // Soft Delete 수행
    @Modifying
    @Query("UPDATE Campaign c SET c.deletedAt = :deletedAt, c.updatedAt = :updatedAt WHERE c.id = :id")
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt, @Param("updatedAt") LocalDateTime updatedAt);
    
    // Soft Delete 복구
    @Modifying
    @Query("UPDATE Campaign c SET c.deletedAt = NULL, c.updatedAt = :updatedAt WHERE c.id = :id")
    int restoreById(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);
    
    // 삭제된 항목들만 조회
    @Query("SELECT c FROM Campaign c WHERE c.deletedAt IS NOT NULL ORDER BY c.deletedAt DESC")
    Page<Campaign> findDeletedCampaigns(Pageable pageable);
    
    // ===== 블록체인 관련 쿼리 메서드 =====
    
    /**
     * 특정 블록체인 상태인 캠페인들을 조회합니다
     */
    @Query("SELECT c FROM Campaign c WHERE c.blockchainStatus = :blockchainStatus AND c.deletedAt IS NULL")
    List<Campaign> findByBlockchainStatus(@Param("blockchainStatus") com.hanachain.hanachainbackend.entity.BlockchainStatus blockchainStatus);
    
    /**
     * 블록체인 연동이 완료된 캠페인들을 조회합니다
     */
    @Query("SELECT c FROM Campaign c WHERE c.blockchainStatus = 'ACTIVE' AND c.blockchainCampaignId IS NOT NULL AND c.deletedAt IS NULL")
    Page<Campaign> findBlockchainActiveCampaigns(Pageable pageable);
    
    /**
     * 블록체인 연동이 실패한 캠페인들을 조회합니다
     */
    @Query("SELECT c FROM Campaign c WHERE c.blockchainStatus = 'BLOCKCHAIN_FAILED' AND c.deletedAt IS NULL ORDER BY c.blockchainProcessedAt DESC")
    Page<Campaign> findBlockchainFailedCampaigns(Pageable pageable);
}
