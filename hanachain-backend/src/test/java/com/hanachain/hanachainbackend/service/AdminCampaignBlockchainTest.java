package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.campaign.CampaignCreateRequest;
import com.hanachain.hanachainbackend.dto.campaign.CampaignDetailResponse;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.service.blockchain.BlockchainService;
import com.hanachain.hanachainbackend.service.impl.CampaignServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin 캠페인 블록체인 통합 테스트")
class AdminCampaignBlockchainTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private UserService userService;

    @Mock
    private BlockchainService blockchainService;

    @InjectMocks
    private CampaignServiceImpl campaignService;

    private User adminUser;
    private CampaignCreateRequest createDto;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .email("admin@hanachain.com")
                .name("관리자")
                .role(User.Role.ADMIN)
                .build();

        createDto = new CampaignCreateRequest();
        createDto.setTitle("재난 구호 캠페인");
        createDto.setSubtitle("태풍 피해 지역 지원");
        createDto.setDescription("태풍으로 피해를 입은 지역을 위한 긴급 구호 캠페인입니다.");
        createDto.setOrganizer("한국재난구호협회");
        createDto.setTargetAmount(new BigDecimal("10000000"));
        createDto.setImageUrl("https://example.com/campaign.jpg");
        createDto.setCategory(Campaign.CampaignCategory.DISASTER_RELIEF);
        createDto.setStartDate(LocalDateTime.now().plusDays(1));
        createDto.setEndDate(LocalDateTime.now().plusDays(30));
        createDto.setBeneficiaryAddress("0x1234567890123456789012345678901234567890");
    }

    @Test
    @DisplayName("관리자 캠페인 생성 시 수혜자 주소가 있으면 자동으로 블록체인 등록")
    void createAdminCampaign_WithBeneficiaryAddress_ShouldRegisterOnBlockchain() {
        // Given
        when(userService.findById(1L)).thenReturn(adminUser);
        
        Campaign savedCampaign = createMockCampaign();
        when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);
        
        when(blockchainService.createCampaignAsync(
            anyLong(), anyString(), any(BigInteger.class), any(BigInteger.class)
        )).thenReturn(CompletableFuture.completedFuture("0x123hash"));

        // When
        CampaignDetailResponse result = campaignService.createCampaign(createDto, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("재난 구호 캠페인");
        
        // 블록체인 서비스 호출 검증
        verify(blockchainService).createCampaignAsync(
            eq(savedCampaign.getId()),
            eq("0x1234567890123456789012345678901234567890"),
            any(BigInteger.class),
            any(BigInteger.class)
        );
        
        // 캠페인 저장 호출 검증 (수혜자 주소 설정 포함)
        ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository, atLeast(2)).save(campaignCaptor.capture());
        
        Campaign capturedCampaign = campaignCaptor.getAllValues().get(1); // 두 번째 저장 (수혜자 주소 설정 후)
        assertThat(capturedCampaign.getBeneficiaryAddress()).isEqualTo("0x1234567890123456789012345678901234567890");
    }

    @Test
    @DisplayName("관리자 캠페인 생성 시 수혜자 주소가 없으면 블록체인 등록 건너뜀")
    void createAdminCampaign_WithoutBeneficiaryAddress_ShouldSkipBlockchainRegistration() {
        // Given
        createDto.setBeneficiaryAddress(null); // 수혜자 주소 없음
        
        when(userService.findById(1L)).thenReturn(adminUser);
        
        Campaign savedCampaign = createMockCampaign();
        when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

        // When
        CampaignDetailResponse result = campaignService.createCampaign(createDto, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("재난 구호 캠페인");
        
        // 블록체인 서비스 호출 안됨 검증
        verify(blockchainService, never()).createCampaignAsync(anyLong(), anyString(), any(), any());
        
        // NOT_REGISTERED 상태로 설정 검증
        ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository, atLeast(2)).save(campaignCaptor.capture());
    }

    @Test
    @DisplayName("관리자 캠페인 블록체인 등록 실패 시 실패 상태로 저장")
    void createAdminCampaign_BlockchainRegistrationFails_ShouldSaveFailedStatus() {
        // Given
        when(userService.findById(1L)).thenReturn(adminUser);
        
        Campaign savedCampaign = createMockCampaign();
        when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);
        
        // 블록체인 등록 실패 시뮬레이션
        when(blockchainService.createCampaignAsync(anyLong(), anyString(), any(), any()))
            .thenThrow(new RuntimeException("네트워크 연결 실패"));

        // When
        CampaignDetailResponse result = campaignService.createCampaign(createDto, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("재난 구호 캠페인");
        
        // 블록체인 서비스 호출 시도 검증
        verify(blockchainService).createCampaignAsync(anyLong(), anyString(), any(), any());
        
        // 실패 상태 저장 검증
        ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository, atLeast(3)).save(campaignCaptor.capture());
    }

    @Test
    @DisplayName("관리자 캠페인 생성 시 수혜자 주소 형식 검증")
    void createAdminCampaign_InvalidBeneficiaryAddress_ShouldStillCreateCampaign() {
        // Given
        createDto.setBeneficiaryAddress("invalid-address"); // 잘못된 주소 형식
        
        when(userService.findById(1L)).thenReturn(adminUser);
        
        Campaign savedCampaign = createMockCampaign();
        when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);

        // When
        CampaignDetailResponse result = campaignService.createCampaign(createDto, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("재난 구호 캠페인");
        
        // DTO 검증에서 걸러지므로 블록체인 등록 시도 안됨
        verify(blockchainService, never()).createCampaignAsync(anyLong(), anyString(), any(), any());
    }

    @Test
    @DisplayName("관리자 캠페인 블록체인 등록 상태 추적")
    void createAdminCampaign_ShouldTrackBlockchainStatus() {
        // Given
        when(userService.findById(1L)).thenReturn(adminUser);
        
        Campaign savedCampaign = createMockCampaign();
        when(campaignRepository.save(any(Campaign.class))).thenReturn(savedCampaign);
        
        when(blockchainService.createCampaignAsync(anyLong(), anyString(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture("0x123hash"));

        // When
        CampaignDetailResponse result = campaignService.createCampaign(createDto, 1L);

        // Then
        assertThat(result).isNotNull();
        
        // 캠페인 저장 과정 검증 (상태 변화 추적)
        ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository, atLeast(2)).save(campaignCaptor.capture());
        
        // 첫 번째: 기본 캠페인 생성
        Campaign firstSave = campaignCaptor.getAllValues().get(0);
        assertThat(firstSave.getTitle()).isEqualTo("재난 구호 캠페인");
        assertThat(firstSave.getUser()).isEqualTo(adminUser);
        
        // 두 번째: 수혜자 주소 설정 후
        Campaign secondSave = campaignCaptor.getAllValues().get(1);
        assertThat(secondSave.getBeneficiaryAddress()).isEqualTo("0x1234567890123456789012345678901234567890");
    }

    private Campaign createMockCampaign() {
        return Campaign.builder()
                .id(1L)
                .title("재난 구호 캠페인")
                .subtitle("태풍 피해 지역 지원")
                .description("태풍으로 피해를 입은 지역을 위한 긴급 구호 캠페인입니다.")
                .organizer("한국재난구호협회")
                .targetAmount(new BigDecimal("10000000"))
                .currentAmount(BigDecimal.ZERO)
                .imageUrl("https://example.com/campaign.jpg")
                .status(Campaign.CampaignStatus.DRAFT)
                .category(Campaign.CampaignCategory.DISASTER_RELIEF)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .user(adminUser)
                .build();
    }
}