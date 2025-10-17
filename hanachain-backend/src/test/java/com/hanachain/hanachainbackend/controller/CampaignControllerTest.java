package com.hanachain.hanachainbackend.controller;

import com.hanachain.hanachainbackend.controller.api.CampaignController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanachain.hanachainbackend.dto.campaign.CampaignListResponse;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.service.CampaignService;
import com.hanachain.hanachainbackend.config.WebMvcTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CampaignController.class)
@Import(WebMvcTestConfiguration.class)
@ActiveProfiles({"test", "webmvc-test"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CampaignControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CampaignService campaignService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private CampaignListResponse createMockCampaignListResponse() {
        return CampaignListResponse.builder()
                .id(1L)
                .title("테스트 캠페인")
                .description("테스트 설명")
                .targetAmount(BigDecimal.valueOf(1000000))
                .currentAmount(BigDecimal.valueOf(500000))
                .category(Campaign.CampaignCategory.MEDICAL)
                .status(Campaign.CampaignStatus.ACTIVE)
                .creatorName("테스트 사용자")
                .createdAt(LocalDateTime.now())
                .progressPercentage(BigDecimal.valueOf(50))
                .isActive(true)
                .build();
    }
    
    @Test
    void testGetCampaignsWithFilters_AllParameters() throws Exception {
        // Given
        CampaignListResponse campaignDto = createMockCampaignListResponse();
        List<CampaignListResponse> campaigns = Arrays.asList(campaignDto);
        Page<CampaignListResponse> campaignPage = new PageImpl<>(campaigns, PageRequest.of(0, 10), campaigns.size());
        
        when(campaignService.getCampaignsWithFilters(
                eq(Campaign.CampaignCategory.MEDICAL), 
                eq(Campaign.CampaignStatus.ACTIVE), 
                eq("의료"), 
                eq("popular"), 
                any(Pageable.class)
        )).thenReturn(campaignPage);
        
        // When & Then
        mockMvc.perform(get("/campaigns")
                        .param("category", "MEDICAL")
                        .param("status", "ACTIVE")
                        .param("keyword", "의료")
                        .param("sort", "popular")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("테스트 캠페인"))
                .andExpect(jsonPath("$.data.content[0].category").value("MEDICAL"))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }
    
    @Test
    void testGetCampaignsWithFilters_NoParameters() throws Exception {
        // Given
        CampaignListResponse campaignDto = createMockCampaignListResponse();
        List<CampaignListResponse> campaigns = Arrays.asList(campaignDto);
        Page<CampaignListResponse> campaignPage = new PageImpl<>(campaigns, PageRequest.of(0, 10), campaigns.size());
        
        when(campaignService.getCampaignsWithFilters(
                eq(null), 
                eq(null), 
                eq(null), 
                eq("recent"), 
                any(Pageable.class)
        )).thenReturn(campaignPage);
        
        // When & Then
        mockMvc.perform(get("/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].title").value("테스트 캠페인"));
    }
    
    @Test
    void testGetCampaignsWithFilters_CategoryOnly() throws Exception {
        // Given
        CampaignListResponse campaignDto = createMockCampaignListResponse();
        campaignDto.setCategory(Campaign.CampaignCategory.EDUCATION);
        List<CampaignListResponse> campaigns = Arrays.asList(campaignDto);
        Page<CampaignListResponse> campaignPage = new PageImpl<>(campaigns, PageRequest.of(0, 10), campaigns.size());
        
        when(campaignService.getCampaignsWithFilters(
                eq(Campaign.CampaignCategory.EDUCATION), 
                eq(null), 
                eq(null), 
                eq("recent"), 
                any(Pageable.class)
        )).thenReturn(campaignPage);
        
        // When & Then
        mockMvc.perform(get("/campaigns")
                        .param("category", "EDUCATION")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].category").value("EDUCATION"));
    }
    
    @Test
    void testGetCampaignsWithFilters_KeywordOnly() throws Exception {
        // Given
        CampaignListResponse campaignDto = createMockCampaignListResponse();
        campaignDto.setTitle("교육 지원 캠페인");
        List<CampaignListResponse> campaigns = Arrays.asList(campaignDto);
        Page<CampaignListResponse> campaignPage = new PageImpl<>(campaigns, PageRequest.of(0, 10), campaigns.size());
        
        when(campaignService.getCampaignsWithFilters(
                eq(null), 
                eq(null), 
                eq("교육"), 
                eq("recent"), 
                any(Pageable.class)
        )).thenReturn(campaignPage);
        
        // When & Then
        mockMvc.perform(get("/campaigns")
                        .param("keyword", "교육")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("교육 지원 캠페인"));
    }
    
    @Test
    void testGetCampaignsWithFilters_SortPopular() throws Exception {
        // Given
        CampaignListResponse campaignDto = createMockCampaignListResponse();
        campaignDto.setCurrentAmount(BigDecimal.valueOf(800000));
        List<CampaignListResponse> campaigns = Arrays.asList(campaignDto);
        Page<CampaignListResponse> campaignPage = new PageImpl<>(campaigns, PageRequest.of(0, 10), campaigns.size());
        
        when(campaignService.getCampaignsWithFilters(
                eq(null), 
                eq(null), 
                eq(null), 
                eq("popular"), 
                any(Pageable.class)
        )).thenReturn(campaignPage);
        
        // When & Then
        mockMvc.perform(get("/campaigns")
                        .param("sort", "popular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].currentAmount").value(800000));
    }
    
    @Test
    void testGetCampaignsWithFilters_SortProgress() throws Exception {
        // Given
        CampaignListResponse campaignDto = createMockCampaignListResponse();
        campaignDto.setProgressPercentage(BigDecimal.valueOf(80));
        List<CampaignListResponse> campaigns = Arrays.asList(campaignDto);
        Page<CampaignListResponse> campaignPage = new PageImpl<>(campaigns, PageRequest.of(0, 10), campaigns.size());
        
        when(campaignService.getCampaignsWithFilters(
                eq(null), 
                eq(null), 
                eq(null), 
                eq("progress"), 
                any(Pageable.class)
        )).thenReturn(campaignPage);
        
        // When & Then
        mockMvc.perform(get("/campaigns")
                        .param("sort", "progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].progressPercentage").value(80));
    }
    
    @Test
    void testGetCampaignsWithFilters_CustomPagination() throws Exception {
        // Given
        CampaignListResponse campaignDto = createMockCampaignListResponse();
        List<CampaignListResponse> campaigns = Arrays.asList(campaignDto);
        Page<CampaignListResponse> campaignPage = new PageImpl<>(campaigns, PageRequest.of(1, 5), campaigns.size());
        
        when(campaignService.getCampaignsWithFilters(
                eq(null), 
                eq(null), 
                eq(null), 
                eq("recent"), 
                any(Pageable.class)
        )).thenReturn(campaignPage);
        
        // When & Then
        mockMvc.perform(get("/campaigns")
                        .param("page", "1")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.number").value(1));
    }
    
    @Test
    void testGetCampaignsWithFilters_ErrorHandling() throws Exception {
        // Given
        when(campaignService.getCampaignsWithFilters(
                any(), any(), any(), any(), any(Pageable.class)
        )).thenThrow(new RuntimeException("데이터베이스 오류"));
        
        // When & Then
        mockMvc.perform(get("/campaigns")
                        .param("category", "MEDICAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("캠페인 목록 조회에 실패했습니다: 데이터베이스 오류"));
    }
}