package com.hanachain.hanachainbackend.controller;

import com.hanachain.hanachainbackend.controller.api.CampaignController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanachain.hanachainbackend.dto.campaign.CampaignCreateRequest;
import com.hanachain.hanachainbackend.dto.campaign.CampaignDetailResponse;
import com.hanachain.hanachainbackend.entity.BlockchainStatus;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import com.hanachain.hanachainbackend.service.CampaignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CampaignController 블록체인 통합 엔드포인트 테스트
 */
@WebMvcTest(CampaignController.class)
class CampaignBlockchainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CampaignService campaignService;

    private User testUser;
    private Campaign testCampaign;
    private CampaignCreateRequest createDto;
    private CampaignDetailResponse detailDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        testCampaign = Campaign.builder()
                .id(1L)
                .title("Test Campaign")
                .description("Test Description")
                .targetAmount(new BigDecimal("10000"))
                .currentAmount(new BigDecimal("5000"))
                .donorCount(10)
                .status(Campaign.CampaignStatus.ACTIVE)
                .category(Campaign.CampaignCategory.MEDICAL)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .user(testUser)
                .blockchainStatus(BlockchainStatus.ACTIVE)
                .blockchainCampaignId(BigInteger.valueOf(123))
                .blockchainTransactionHash("0xabc123")
                .beneficiaryAddress("0x1234567890123456789012345678901234567890")
                .build();

        createDto = new CampaignCreateRequest();
        createDto.setTitle("Test Campaign");
        createDto.setDescription("Test Description");
        createDto.setOrganizer("Test Organizer");
        createDto.setTargetAmount(new BigDecimal("10000"));
        createDto.setCategory(Campaign.CampaignCategory.MEDICAL);
        createDto.setStartDate(LocalDateTime.now().plusDays(1));
        createDto.setEndDate(LocalDateTime.now().plusDays(30));
        createDto.setBeneficiaryAddress("0x1234567890123456789012345678901234567890");

        detailDto = CampaignDetailResponse.fromEntity(testCampaign);
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCampaignWithBlockchain_성공_강제등록() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUser).thenReturn(Optional.of(testUser));
            
            when(campaignService.createCampaignWithBlockchain(eq(1L), any(CampaignCreateRequest.class), eq(true)))
                    .thenReturn(testCampaign);

            // When & Then
            mockMvc.perform(post("/campaigns/blockchain")
                            .with(csrf())
                            .param("forceBlockchainRegistration", "true")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("블록체인 통합 캠페인이 생성되었습니다."))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.title").value("Test Campaign"))
                    .andExpect(jsonPath("$.data.blockchainStatus").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.blockchainCampaignId").value(123))
                    .andExpect(jsonPath("$.data.beneficiaryAddress").value("0x1234567890123456789012345678901234567890"));

            verify(campaignService).createCampaignWithBlockchain(eq(1L), any(CampaignCreateRequest.class), eq(true));
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCampaignWithBlockchain_성공_일반등록() throws Exception {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.NONE);
        testCampaign.setBlockchainCampaignId(null);
        
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUser).thenReturn(Optional.of(testUser));
            
            when(campaignService.createCampaignWithBlockchain(eq(1L), any(CampaignCreateRequest.class), eq(false)))
                    .thenReturn(testCampaign);

            // When & Then
            mockMvc.perform(post("/campaigns/blockchain")
                            .with(csrf())
                            .param("forceBlockchainRegistration", "false")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.blockchainStatus").value("NONE"));

            verify(campaignService).createCampaignWithBlockchain(eq(1L), any(CampaignCreateRequest.class), eq(false));
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCampaignWithBlockchain_실패_유효성검증() throws Exception {
        // Given
        createDto.setTitle(""); // 빈 제목
        createDto.setBeneficiaryAddress("invalid-address"); // 잘못된 주소

        // When & Then
        mockMvc.perform(post("/campaigns/blockchain")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(campaignService);
    }

    @Test
    void createCampaignWithBlockchain_실패_인증없음() throws Exception {
        // When & Then
        mockMvc.perform(post("/campaigns/blockchain")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(campaignService);
    }

    @Test
    void getBlockchainStatus_성공() throws Exception {
        // Given
        when(campaignService.getBlockchainStatus(1L)).thenReturn(detailDto);

        // When & Then
        mockMvc.perform(get("/campaigns/1/blockchain/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("블록체인 상태 조회 완료"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.blockchainStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.blockchainCampaignId").value(123))
                .andExpect(jsonPath("$.data.blockchainTransactionHash").value("0xabc123"));

        verify(campaignService).getBlockchainStatus(1L);
    }

    @Test
    void getBlockchainStatus_실패_캠페인없음() throws Exception {
        // Given
        when(campaignService.getBlockchainStatus(999L))
                .thenThrow(new RuntimeException("캠페인을 찾을 수 없습니다: 999"));

        // When & Then
        mockMvc.perform(get("/campaigns/999/blockchain/status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("블록체인 상태 조회에 실패했습니다: 캠페인을 찾을 수 없습니다: 999"));

        verify(campaignService).getBlockchainStatus(999L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void finalizeCampaignOnBlockchain_성공() throws Exception {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.BLOCKCHAIN_PROCESSING);
        testCampaign.setBlockchainErrorMessage("캠페인 완료 처리 중");
        
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUser).thenReturn(Optional.of(testUser));
            
            when(campaignService.finalizeCampaignOnBlockchain(1L, 1L)).thenReturn(testCampaign);

            // When & Then
            mockMvc.perform(post("/campaigns/1/blockchain/finalize")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("블록체인 캠페인 완료 처리가 시작되었습니다."))
                    .andExpect(jsonPath("$.data.blockchainStatus").value("BLOCKCHAIN_PROCESSING"));

            verify(campaignService).finalizeCampaignOnBlockchain(1L, 1L);
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void finalizeCampaignOnBlockchain_실패_권한없음() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUser).thenReturn(Optional.of(testUser));
            
            when(campaignService.finalizeCampaignOnBlockchain(1L, 1L))
                    .thenThrow(new RuntimeException("캠페인을 완료할 권한이 없습니다."));

            // When & Then
            mockMvc.perform(post("/campaigns/1/blockchain/finalize")
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("블록체인 캠페인 완료 처리에 실패했습니다: 캠페인을 완료할 권한이 없습니다."));

            verify(campaignService).finalizeCampaignOnBlockchain(1L, 1L);
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void retryBlockchainOperation_성공() throws Exception {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.BLOCKCHAIN_PENDING);
        
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUser).thenReturn(Optional.of(testUser));
            
            when(campaignService.retryBlockchainOperation(1L, 1L)).thenReturn(testCampaign);

            // When & Then
            mockMvc.perform(post("/campaigns/1/blockchain/retry")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("블록체인 작업 재시도가 시작되었습니다."))
                    .andExpect(jsonPath("$.data.blockchainStatus").value("BLOCKCHAIN_PENDING"));

            verify(campaignService).retryBlockchainOperation(1L, 1L);
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void retryBlockchainOperation_실패_실패상태아님() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUser).thenReturn(Optional.of(testUser));
            
            when(campaignService.retryBlockchainOperation(1L, 1L))
                    .thenThrow(new RuntimeException("실패 상태의 캠페인만 재시도할 수 있습니다."));

            // When & Then
            mockMvc.perform(post("/campaigns/1/blockchain/retry")
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("블록체인 작업 재시도에 실패했습니다: 실패 상태의 캠페인만 재시도할 수 있습니다."));

            verify(campaignService).retryBlockchainOperation(1L, 1L);
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void syncBlockchainStatus_성공() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUser).thenReturn(Optional.of(testUser));
            
            doNothing().when(campaignService).syncBlockchainStatus(1L);

            // When & Then
            mockMvc.perform(post("/campaigns/1/blockchain/sync")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("블록체인 상태 동기화가 완료되었습니다."));

            verify(campaignService).syncBlockchainStatus(1L);
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    void syncBlockchainStatus_실패_동기화오류() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUser).thenReturn(Optional.of(testUser));
            
            doThrow(new RuntimeException("블록체인 연결 실패"))
                    .when(campaignService).syncBlockchainStatus(1L);

            // When & Then
            mockMvc.perform(post("/campaigns/1/blockchain/sync")
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("블록체인 상태 동기화에 실패했습니다: 블록체인 연결 실패"));

            verify(campaignService).syncBlockchainStatus(1L);
        }
    }

    @Test
    void syncBlockchainStatus_실패_인증없음() throws Exception {
        // When & Then
        mockMvc.perform(post("/campaigns/1/blockchain/sync")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(campaignService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void 블록체인엔드포인트_통합테스트_전체플로우() throws Exception {
        // Given
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUser).thenReturn(Optional.of(testUser));

            // 1. 캠페인 생성 (블록체인 등록 요청)
            when(campaignService.createCampaignWithBlockchain(eq(1L), any(CampaignCreateRequest.class), eq(true)))
                    .thenReturn(testCampaign);

            mockMvc.perform(post("/campaigns/blockchain")
                            .with(csrf())
                            .param("forceBlockchainRegistration", "true")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.blockchainStatus").value("ACTIVE"));

            // 2. 블록체인 상태 조회
            when(campaignService.getBlockchainStatus(1L)).thenReturn(detailDto);

            mockMvc.perform(get("/campaigns/1/blockchain/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.blockchainStatus").value("ACTIVE"));

            // 3. 블록체인 동기화
            doNothing().when(campaignService).syncBlockchainStatus(1L);

            mockMvc.perform(post("/campaigns/1/blockchain/sync")
                            .with(csrf()))
                    .andExpect(status().isOk());

            // 4. 캠페인 완료
            when(campaignService.finalizeCampaignOnBlockchain(1L, 1L)).thenReturn(testCampaign);

            mockMvc.perform(post("/campaigns/1/blockchain/finalize")
                            .with(csrf()))
                    .andExpect(status().isOk());

            // Verify all service calls
            verify(campaignService).createCampaignWithBlockchain(eq(1L), any(CampaignCreateRequest.class), eq(true));
            verify(campaignService).getBlockchainStatus(1L);
            verify(campaignService).syncBlockchainStatus(1L);
            verify(campaignService).finalizeCampaignOnBlockchain(1L, 1L);
        }
    }
}