package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.UserFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {
    
    /**
     * 사용자별 즐겨찾기 캠페인 조회 (페이징)
     */
    @Query("SELECT uf FROM UserFavorite uf WHERE uf.user.id = :userId ORDER BY uf.createdAt DESC")
    Page<UserFavorite> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 사용자별 즐겨찾기 캠페인 전체 목록 조회
     */
    @Query("SELECT uf FROM UserFavorite uf WHERE uf.user.id = :userId ORDER BY uf.createdAt DESC")
    List<UserFavorite> findAllByUserId(@Param("userId") Long userId);
    
    /**
     * 특정 사용자의 특정 캠페인 즐겨찾기 여부 확인
     */
    @Query("SELECT uf FROM UserFavorite uf WHERE uf.user.id = :userId AND uf.campaign.id = :campaignId")
    Optional<UserFavorite> findByUserIdAndCampaignId(@Param("userId") Long userId, @Param("campaignId") Long campaignId);
    
    /**
     * 특정 사용자의 특정 캠페인 즐겨찾기 존재 여부 확인
     */
    @Query("SELECT COUNT(uf) > 0 FROM UserFavorite uf WHERE uf.user.id = :userId AND uf.campaign.id = :campaignId")
    boolean existsByUserIdAndCampaignId(@Param("userId") Long userId, @Param("campaignId") Long campaignId);
    
    /**
     * 사용자별 즐겨찾기 캠페인 수 조회
     */
    @Query("SELECT COUNT(uf) FROM UserFavorite uf WHERE uf.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    /**
     * 캠페인별 즐겨찾기 추가한 사용자 수 조회
     */
    @Query("SELECT COUNT(uf) FROM UserFavorite uf WHERE uf.campaign.id = :campaignId")
    long countByCampaignId(@Param("campaignId") Long campaignId);
    
    /**
     * 사용자별 즐겨찾기 캠페인 검색 (제목 기준)
     */
    @Query("SELECT uf FROM UserFavorite uf WHERE uf.user.id = :userId " +
           "AND (:search IS NULL OR LOWER(uf.campaign.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY uf.createdAt DESC")
    Page<UserFavorite> findByUserIdWithSearch(@Param("userId") Long userId, 
                                             @Param("search") String search, 
                                             Pageable pageable);
}