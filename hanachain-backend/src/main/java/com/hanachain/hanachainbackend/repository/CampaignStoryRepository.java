package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.CampaignStory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignStoryRepository extends JpaRepository<CampaignStory, Long> {
    
    @Query("SELECT cs FROM CampaignStory cs WHERE cs.campaign.id = :campaignId AND cs.published = true ORDER BY cs.displayOrder ASC, cs.createdAt DESC")
    List<CampaignStory> findByCampaignIdAndPublishedOrderByDisplayOrder(@Param("campaignId") Long campaignId);
    
    @Query("SELECT cs FROM CampaignStory cs WHERE cs.campaign.id = :campaignId ORDER BY cs.displayOrder ASC, cs.createdAt DESC")
    List<CampaignStory> findByCampaignIdOrderByDisplayOrder(@Param("campaignId") Long campaignId);
    
    @Query("SELECT cs FROM CampaignStory cs WHERE cs.campaign.id = :campaignId AND cs.type = :type AND cs.published = true")
    List<CampaignStory> findByCampaignIdAndTypeAndPublished(
            @Param("campaignId") Long campaignId, 
            @Param("type") CampaignStory.StoryType type);
    
    @Query("SELECT cs FROM CampaignStory cs WHERE cs.campaign.user.id = :userId")
    Page<CampaignStory> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(cs) FROM CampaignStory cs WHERE cs.campaign.id = :campaignId AND cs.published = true")
    long countByCampaignIdAndPublished(@Param("campaignId") Long campaignId);
}
