package com.hanachain.hanachainbackend.controller;

import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
@DisplayName("Campaign Filter Integration Test")
class CampaignFilterIntegrationTest {
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Test
    @DisplayName("Repository findWithFilters method works correctly")
    void testRepositoryFindWithFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When - Test with all null filters (should not throw exception)
        Page<Campaign> result = campaignRepository.findWithFilters(
                null, null, null, "recent", pageable);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
    }
    
    @Test
    @DisplayName("Repository findWithFilters handles category filter")
    void testRepositoryFindWithFilters_CategoryFilter() {
        // Given
        Campaign.CampaignCategory category = Campaign.CampaignCategory.MEDICAL;
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Campaign> result = campaignRepository.findWithFilters(
                category, null, null, "recent", pageable);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
    }
    
    @Test
    @DisplayName("Repository findWithFilters handles status filter")
    void testRepositoryFindWithFilters_StatusFilter() {
        // Given
        Campaign.CampaignStatus status = Campaign.CampaignStatus.ACTIVE;
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Campaign> result = campaignRepository.findWithFilters(
                null, status, null, "recent", pageable);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
    }
    
    @Test
    @DisplayName("Repository findWithFilters handles keyword filter")
    void testRepositoryFindWithFilters_KeywordFilter() {
        // Given
        String keyword = "test";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Campaign> result = campaignRepository.findWithFilters(
                null, null, keyword, "recent", pageable);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
    }
    
    @Test
    @DisplayName("Repository findWithFilters handles different sort options")
    void testRepositoryFindWithFilters_SortOptions() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        // When & Then - Test all sort options
        String[] sortOptions = {"recent", "popular", "progress"};
        
        for (String sort : sortOptions) {
            Page<Campaign> result = campaignRepository.findWithFilters(
                    null, null, null, sort, pageable);
            
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotNull();
        }
    }
    
    @Test
    @DisplayName("Repository findWithFilters handles combined filters")
    void testRepositoryFindWithFilters_CombinedFilters() {
        // Given
        Campaign.CampaignCategory category = Campaign.CampaignCategory.MEDICAL;
        Campaign.CampaignStatus status = Campaign.CampaignStatus.ACTIVE;
        String keyword = "medical";
        String sort = "popular";
        Pageable pageable = PageRequest.of(0, 10);
        
        // When
        Page<Campaign> result = campaignRepository.findWithFilters(
                category, status, keyword, sort, pageable);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isNotNull();
    }
}