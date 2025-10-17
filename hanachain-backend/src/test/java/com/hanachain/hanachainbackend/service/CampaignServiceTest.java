package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.campaign.CampaignCreateRequest;
import com.hanachain.hanachainbackend.dto.campaign.CampaignListResponse;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.service.impl.CampaignServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CampaignServiceTest {
    
    @Mock
    private CampaignRepository campaignRepository;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private CampaignServiceImpl campaignService;
    
    @Test
    void testCreateCampaign() {
        // Given
        Long userId = 1L;
        CampaignCreateRequest request = new CampaignCreateRequest();
        request.setTitle("Test Campaign");
        request.setDescription("Test Description");
        request.setTargetAmount(BigDecimal.valueOf(1000000));
        request.setCategory(Campaign.CampaignCategory.MEDICAL);
        request.setStartDate(LocalDateTime.now().plusDays(1));
        request.setEndDate(LocalDateTime.now().plusDays(30));
        
        User mockUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .build();
        
        Campaign mockCampaign = Campaign.builder()
                .id(1L)
                .title(request.getTitle())
                .description(request.getDescription())
                .targetAmount(request.getTargetAmount())
                .category(request.getCategory())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .user(mockUser)
                .status(Campaign.CampaignStatus.DRAFT)
                .build();
        
        when(userService.findById(userId)).thenReturn(mockUser);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(mockCampaign);
        
        // When
        Campaign result = campaignService.createCampaign(userId, request);
        
        // Then
        assertThat(result.getTitle()).isEqualTo(request.getTitle());
        assertThat(result.getTargetAmount()).isEqualTo(request.getTargetAmount());
        assertThat(result.getStatus()).isEqualTo(Campaign.CampaignStatus.DRAFT);
        assertThat(result.getUser().getId()).isEqualTo(userId);
    }
    
    @Test
    void testGetCampaignsWithFilters_AllFilters() {
        // Given
        Campaign.CampaignCategory category = Campaign.CampaignCategory.MEDICAL;
        Campaign.CampaignStatus status = Campaign.CampaignStatus.ACTIVE;
        String keyword = "의료";
        String sort = "popular";
        Pageable pageable = PageRequest.of(0, 10);
        
        User mockUser = User.builder()
                .id(1L)
                .name("Test User")
                .build();
        
        Campaign mockCampaign = Campaign.builder()
                .id(1L)
                .title("의료 캠페인")
                .description("의료비 지원")
                .category(category)
                .status(status)
                .user(mockUser)
                .targetAmount(BigDecimal.valueOf(1000000))
                .currentAmount(BigDecimal.valueOf(500000))
                .donorCount(10)
                .imageUrl("test-image.jpg")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                                .build();
        
        List<Campaign> campaigns = Arrays.asList(mockCampaign);
        Page<Campaign> campaignPage = new PageImpl<>(campaigns, pageable, campaigns.size());
        
        when(campaignRepository.findWithFilters(category, status, keyword, sort, pageable))
                .thenReturn(campaignPage);
        
        // When
        Page<CampaignListResponse> result = campaignService.getCampaignsWithFilters(
                category, status, keyword, sort, pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("의료 캠페인");
        assertThat(result.getContent().get(0).getCategory()).isEqualTo(category);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(status);
        
        verify(campaignRepository).findWithFilters(category, status, keyword, sort, pageable);
    }
    
    @Test
    void testGetCampaignsWithFilters_NullFilters() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        
        User mockUser = User.builder()
                .id(1L)
                .name("Test User")
                .build();
        
        Campaign mockCampaign = Campaign.builder()
                .id(1L)
                .title("테스트 캠페인")
                .description("테스트 설명")
                .category(Campaign.CampaignCategory.EDUCATION)
                .status(Campaign.CampaignStatus.ACTIVE)
                .user(mockUser)
                .targetAmount(BigDecimal.valueOf(1000000))
                .currentAmount(BigDecimal.valueOf(300000))
                .donorCount(5)
                .imageUrl("test-image.jpg")
                .startDate(LocalDateTime.now().minusDays(2))
                .endDate(LocalDateTime.now().plusDays(28))
                                .build();
        
        List<Campaign> campaigns = Arrays.asList(mockCampaign);
        Page<Campaign> campaignPage = new PageImpl<>(campaigns, pageable, campaigns.size());
        
        when(campaignRepository.findWithFilters(null, null, null, "recent", pageable))
                .thenReturn(campaignPage);
        
        // When
        Page<CampaignListResponse> result = campaignService.getCampaignsWithFilters(
                null, null, null, null, pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        
        verify(campaignRepository).findWithFilters(null, null, null, "recent", pageable);
    }
    
    @Test
    void testGetCampaignsWithFilters_EmptyKeyword() {
        // Given
        String emptyKeyword = "   ";
        Pageable pageable = PageRequest.of(0, 10);
        
        User mockUser = User.builder()
                .id(1L)
                .name("Test User")
                .build();
        
        Campaign mockCampaign = Campaign.builder()
                .id(1L)
                .title("테스트 캠페인")
                .description("테스트 설명")
                .user(mockUser)
                .category(Campaign.CampaignCategory.MEDICAL)
                .status(Campaign.CampaignStatus.ACTIVE)
                .targetAmount(BigDecimal.valueOf(1000000))
                .currentAmount(BigDecimal.valueOf(200000))
                .donorCount(3)
                .imageUrl("test-image.jpg")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(29))
                                .build();
        
        List<Campaign> campaigns = Arrays.asList(mockCampaign);
        Page<Campaign> campaignPage = new PageImpl<>(campaigns, pageable, campaigns.size());
        
        when(campaignRepository.findWithFilters(null, null, null, "recent", pageable))
                .thenReturn(campaignPage);
        
        // When
        Page<CampaignListResponse> result = campaignService.getCampaignsWithFilters(
                null, null, emptyKeyword, null, pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        
        // 빈 문자열 키워드는 null로 처리되어야 함
        verify(campaignRepository).findWithFilters(null, null, null, "recent", pageable);
    }
    
    @Test
    void testGetCampaignsWithFilters_InvalidSort() {
        // Given
        String invalidSort = "invalid";
        Pageable pageable = PageRequest.of(0, 10);
        
        User mockUser = User.builder()
                .id(1L)
                .name("Test User")
                .build();
        
        Campaign mockCampaign = Campaign.builder()
                .id(1L)
                .title("테스트 캠페인")
                .description("테스트 설명")
                .user(mockUser)
                .category(Campaign.CampaignCategory.MEDICAL)
                .status(Campaign.CampaignStatus.ACTIVE)
                .targetAmount(BigDecimal.valueOf(1000000))
                .currentAmount(BigDecimal.valueOf(200000))
                .donorCount(3)
                .imageUrl("test-image.jpg")
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(29))
                                .build();
        
        List<Campaign> campaigns = Arrays.asList(mockCampaign);
        Page<Campaign> campaignPage = new PageImpl<>(campaigns, pageable, campaigns.size());
        
        when(campaignRepository.findWithFilters(null, null, null, "recent", pageable))
                .thenReturn(campaignPage);
        
        // When
        Page<CampaignListResponse> result = campaignService.getCampaignsWithFilters(
                null, null, null, invalidSort, pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        
        // 잘못된 정렬 기준은 "recent"로 기본값 처리되어야 함
        verify(campaignRepository).findWithFilters(null, null, null, "recent", pageable);
    }
    
    @Test
    void testGetCampaignsWithFilters_PopularSort() {
        // Given
        String sort = "popular";
        Pageable pageable = PageRequest.of(0, 10);
        
        User mockUser = User.builder()
                .id(1L)
                .name("Test User")
                .build();
        
        Campaign mockCampaign = Campaign.builder()
                .id(1L)
                .title("인기 캠페인")
                .description("인기 있는 캠페인")
                .user(mockUser)
                .category(Campaign.CampaignCategory.MEDICAL)
                .status(Campaign.CampaignStatus.ACTIVE)
                .targetAmount(BigDecimal.valueOf(2000000))
                .currentAmount(BigDecimal.valueOf(1000000))
                .donorCount(50)
                .imageUrl("popular-campaign.jpg")
                .startDate(LocalDateTime.now().minusDays(10))
                .endDate(LocalDateTime.now().plusDays(20))
                                .build();
        
        List<Campaign> campaigns = Arrays.asList(mockCampaign);
        Page<Campaign> campaignPage = new PageImpl<>(campaigns, pageable, campaigns.size());
        
        when(campaignRepository.findWithFilters(null, null, null, sort, pageable))
                .thenReturn(campaignPage);
        
        // When
        Page<CampaignListResponse> result = campaignService.getCampaignsWithFilters(
                null, null, null, sort, pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("인기 캠페인");
        
        verify(campaignRepository).findWithFilters(null, null, null, sort, pageable);
    }
    
    @Test
    void testGetCampaignsWithFilters_ProgressSort() {
        // Given
        String sort = "progress";
        Pageable pageable = PageRequest.of(0, 10);
        
        User mockUser = User.builder()
                .id(1L)
                .name("Test User")
                .build();
        
        Campaign mockCampaign = Campaign.builder()
                .id(1L)
                .title("진행률 높은 캠페인")
                .description("진행률이 높은 캠페인")
                .user(mockUser)
                .category(Campaign.CampaignCategory.MEDICAL)
                .status(Campaign.CampaignStatus.ACTIVE)
                .targetAmount(BigDecimal.valueOf(1000000))
                .currentAmount(BigDecimal.valueOf(800000))
                .donorCount(40)
                .imageUrl("progress-campaign.jpg")
                .startDate(LocalDateTime.now().minusDays(5))
                .endDate(LocalDateTime.now().plusDays(25))
                                .build();
        
        List<Campaign> campaigns = Arrays.asList(mockCampaign);
        Page<Campaign> campaignPage = new PageImpl<>(campaigns, pageable, campaigns.size());
        
        when(campaignRepository.findWithFilters(null, null, null, sort, pageable))
                .thenReturn(campaignPage);
        
        // When
        Page<CampaignListResponse> result = campaignService.getCampaignsWithFilters(
                null, null, null, sort, pageable);
        
        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("진행률 높은 캠페인");
        
        verify(campaignRepository).findWithFilters(null, null, null, sort, pageable);
    }
}
