package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.campaign.CampaignCreateRequest;
import com.hanachain.hanachainbackend.dto.campaign.CampaignDetailResponse;
import com.hanachain.hanachainbackend.entity.BlockchainStatus;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.service.blockchain.BlockchainService;
import com.hanachain.hanachainbackend.service.impl.CampaignServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CampaignService 블록체인 통합 기능 테스트
 */
@ExtendWith(MockitoExtension.class)
class CampaignBlockchainIntegrationTest {

    @Mock
    private CampaignRepository campaignRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private BlockchainService blockchainService;
    
    @InjectMocks
    private CampaignServiceImpl campaignService;
    
    private User testUser;
    private Campaign testCampaign;
    private CampaignCreateRequest createDto;
    
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
                .currentAmount(BigDecimal.ZERO)
                .donorCount(0)
                .status(Campaign.CampaignStatus.DRAFT)
                .category(Campaign.CampaignCategory.MEDICAL)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(30))
                .user(testUser)
                .blockchainStatus(BlockchainStatus.NONE)
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
    }
    
    @Test
    void createCampaignWithBlockchain_성공_즉시등록() {
        // Given
        when(userService.findById(1L)).thenReturn(testUser);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(testCampaign);
        when(blockchainService.createCampaignAsync(anyLong(), anyString(), any(BigInteger.class), 
                any(BigInteger.class), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("0xabc123"));
        
        // When
        Campaign result = campaignService.createCampaignWithBlockchain(1L, createDto, true);
        
        // Then
        assertNotNull(result);
        assertEquals("Test Campaign", result.getTitle());
        assertEquals("0x1234567890123456789012345678901234567890", result.getBeneficiaryAddress());
        
        verify(userService).findById(1L);
        verify(campaignRepository, atLeast(1)).save(any(Campaign.class));
        verify(blockchainService).createCampaignAsync(anyLong(), eq("0x1234567890123456789012345678901234567890"), 
                any(BigInteger.class), any(BigInteger.class), eq("Test Campaign"), eq("Test Description"));
    }
    
    @Test
    void createCampaignWithBlockchain_성공_즉시등록없음() {
        // Given
        when(userService.findById(1L)).thenReturn(testUser);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(testCampaign);
        
        // When
        Campaign result = campaignService.createCampaignWithBlockchain(1L, createDto, false);
        
        // Then
        assertNotNull(result);
        assertEquals("Test Campaign", result.getTitle());
        
        verify(userService).findById(1L);
        verify(campaignRepository).save(any(Campaign.class));
        verifyNoInteractions(blockchainService);
    }
    
    @Test
    void createCampaignWithBlockchain_실패_수혜자주소없음() {
        // Given
        createDto.setBeneficiaryAddress(null);
        when(userService.findById(1L)).thenReturn(testUser);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(testCampaign);
        
        // When
        Campaign result = campaignService.createCampaignWithBlockchain(1L, createDto, true);
        
        // Then
        assertNotNull(result);
        verifyNoInteractions(blockchainService);
    }
    
    @Test
    void getBlockchainStatus_성공_활성상태() {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.ACTIVE);
        testCampaign.setBlockchainCampaignId(BigInteger.valueOf(123));
        
        BlockchainService.CampaignInfo blockchainInfo = new BlockchainService.CampaignInfo(
                BigInteger.valueOf(123),
                "0x1234567890123456789012345678901234567890",
                BigInteger.valueOf(10000000000L), // 10,000 USDC with 6 decimals
                BigInteger.valueOf(5000000000L),  // 5,000 USDC raised
                BigInteger.valueOf(System.currentTimeMillis() / 1000 + 86400), // deadline in 1 day
                false,
                true,
                "Test Campaign",
                "Test Description"
        );
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        when(blockchainService.getCampaignFromBlockchain(BigInteger.valueOf(123))).thenReturn(blockchainInfo);
        
        // When
        CampaignDetailResponse result = campaignService.getBlockchainStatus(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(BlockchainStatus.ACTIVE, result.getBlockchainStatus());
        assertEquals(BigInteger.valueOf(123), result.getBlockchainCampaignId());
        
        verify(blockchainService).getCampaignFromBlockchain(BigInteger.valueOf(123));
    }
    
    @Test
    void getBlockchainStatus_실패_블록체인조회오류() {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.ACTIVE);
        testCampaign.setBlockchainCampaignId(BigInteger.valueOf(123));
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        when(blockchainService.getCampaignFromBlockchain(BigInteger.valueOf(123)))
                .thenThrow(new RuntimeException("Blockchain connection failed"));
        
        // When
        CampaignDetailResponse result = campaignService.getBlockchainStatus(1L);
        
        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(BlockchainStatus.ACTIVE, result.getBlockchainStatus());
        // 오류 발생 시에도 데이터베이스 정보는 반환되어야 함
    }
    
    @Test
    void finalizeCampaignOnBlockchain_성공() {
        // Given
        testCampaign.setStatus(Campaign.CampaignStatus.ACTIVE);
        testCampaign.setBlockchainStatus(BlockchainStatus.ACTIVE);
        testCampaign.setBlockchainCampaignId(BigInteger.valueOf(123));
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(testCampaign);
        when(blockchainService.finalizeCampaignAsync(BigInteger.valueOf(123)))
                .thenReturn(CompletableFuture.completedFuture("0xdef456"));
        
        // When
        Campaign result = campaignService.finalizeCampaignOnBlockchain(1L, 1L);
        
        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(BlockchainStatus.BLOCKCHAIN_PROCESSING, result.getBlockchainStatus());
        assertEquals("캠페인 완료 처리 중", result.getBlockchainErrorMessage());
        
        verify(blockchainService).finalizeCampaignAsync(BigInteger.valueOf(123));
    }
    
    @Test
    void finalizeCampaignOnBlockchain_실패_권한없음() {
        // Given
        testCampaign.setStatus(Campaign.CampaignStatus.ACTIVE);
        testCampaign.setBlockchainStatus(BlockchainStatus.ACTIVE);
        testCampaign.setBlockchainCampaignId(BigInteger.valueOf(123));
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            campaignService.finalizeCampaignOnBlockchain(1L, 2L); // 다른 사용자 ID
        });
        
        assertEquals("캠페인을 완료할 권한이 없습니다.", exception.getMessage());
        verifyNoInteractions(blockchainService);
    }
    
    @Test
    void finalizeCampaignOnBlockchain_실패_블록체인미등록() {
        // Given
        testCampaign.setStatus(Campaign.CampaignStatus.ACTIVE);
        testCampaign.setBlockchainStatus(BlockchainStatus.NONE);
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            campaignService.finalizeCampaignOnBlockchain(1L, 1L);
        });
        
        assertEquals("블록체인에 등록되지 않은 캠페인입니다.", exception.getMessage());
        verifyNoInteractions(blockchainService);
    }
    
    @Test
    void retryBlockchainOperation_성공() {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED);
        testCampaign.setBlockchainErrorMessage("Previous error");
        testCampaign.setBeneficiaryAddress("0x1234567890123456789012345678901234567890");
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        when(campaignRepository.save(any(Campaign.class))).thenReturn(testCampaign);
        when(blockchainService.createCampaignAsync(anyLong(), anyString(), any(BigInteger.class), 
                any(BigInteger.class), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("0xabc123"));
        
        // When
        Campaign result = campaignService.retryBlockchainOperation(1L, 1L);
        
        // Then
        assertNotNull(result);
        assertEquals(BlockchainStatus.BLOCKCHAIN_PENDING, result.getBlockchainStatus());
        assertNull(result.getBlockchainTransactionHash());
        
        verify(campaignRepository, atLeast(1)).save(any(Campaign.class));
    }
    
    @Test
    void retryBlockchainOperation_실패_실패상태아님() {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.ACTIVE);
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            campaignService.retryBlockchainOperation(1L, 1L);
        });
        
        assertEquals("실패 상태의 캠페인만 재시도할 수 있습니다.", exception.getMessage());
        verifyNoInteractions(blockchainService);
    }
    
    @Test
    void retryBlockchainOperation_실패_수혜자주소없음() {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED);
        testCampaign.setBeneficiaryAddress(null);
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            campaignService.retryBlockchainOperation(1L, 1L);
        });
        
        assertEquals("수혜자 주소가 설정되지 않았습니다.", exception.getMessage());
        verifyNoInteractions(blockchainService);
    }
    
    @Test
    void syncBlockchainStatus_성공() {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.ACTIVE);
        testCampaign.setBlockchainCampaignId(BigInteger.valueOf(123));
        testCampaign.setCurrentAmount(new BigDecimal("5000"));
        testCampaign.setStatus(Campaign.CampaignStatus.ACTIVE);
        
        BlockchainService.CampaignInfo blockchainInfo = new BlockchainService.CampaignInfo(
                BigInteger.valueOf(123),
                "0x1234567890123456789012345678901234567890",
                BigInteger.valueOf(10000000000L), // 10,000 USDC
                BigInteger.valueOf(8000000000L),  // 8,000 USDC raised (different from DB)
                BigInteger.valueOf(System.currentTimeMillis() / 1000 + 86400),
                true, // finalized
                true,
                "Test Campaign",
                "Test Description"
        );
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        when(blockchainService.getCampaignFromBlockchain(BigInteger.valueOf(123))).thenReturn(blockchainInfo);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(testCampaign);
        
        // When
        campaignService.syncBlockchainStatus(1L);
        
        // Then
        ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(campaignCaptor.capture());
        
        Campaign savedCampaign = campaignCaptor.getValue();
        assertEquals(new BigDecimal("8000.00"), savedCampaign.getCurrentAmount()); // 동기화된 금액
        assertEquals(Campaign.CampaignStatus.COMPLETED, savedCampaign.getStatus()); // 완료 상태로 변경
        
        verify(blockchainService).getCampaignFromBlockchain(BigInteger.valueOf(123));
    }
    
    @Test
    void syncBlockchainStatus_건너뜀_블록체인미등록() {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.NONE);
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        
        // When
        campaignService.syncBlockchainStatus(1L);
        
        // Then
        verify(campaignRepository, never()).save(any(Campaign.class));
        verifyNoInteractions(blockchainService);
    }
    
    @Test
    void syncBlockchainStatus_실패_블록체인조회오류() {
        // Given
        testCampaign.setBlockchainStatus(BlockchainStatus.ACTIVE);
        testCampaign.setBlockchainCampaignId(BigInteger.valueOf(123));
        
        when(campaignRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(testCampaign));
        when(blockchainService.getCampaignFromBlockchain(BigInteger.valueOf(123)))
                .thenThrow(new RuntimeException("Blockchain error"));
        
        // When
        campaignService.syncBlockchainStatus(1L);
        
        // Then
        verify(campaignRepository, never()).save(any(Campaign.class));
        verify(blockchainService).getCampaignFromBlockchain(BigInteger.valueOf(123));
    }
    
    @Test
    void convertFromBlockchainAmount_정확한변환() {
        // Given & When
        BigDecimal result1 = ReflectionTestUtils.invokeMethod(campaignService, 
                "convertFromBlockchainAmount", BigInteger.valueOf(1000000L));
        BigDecimal result2 = ReflectionTestUtils.invokeMethod(campaignService, 
                "convertFromBlockchainAmount", BigInteger.valueOf(12345678L));
        
        // Then
        assertEquals(new BigDecimal("1.00"), result1);
        assertEquals(new BigDecimal("12.35"), result2); // 6 decimals 변환
    }
    
    @Test
    void convertToBlockchainAmount_정확한변환() {
        // Given & When
        BigInteger result1 = ReflectionTestUtils.invokeMethod(campaignService, 
                "convertToBlockchainAmount", new BigDecimal("1.00"));
        BigInteger result2 = ReflectionTestUtils.invokeMethod(campaignService, 
                "convertToBlockchainAmount", new BigDecimal("12.345"));
        
        // Then
        assertEquals(BigInteger.valueOf(1000000L), result1);
        assertEquals(BigInteger.valueOf(12345000L), result2); // 6 decimals 변환
    }
    
    @Test
    void classifyAndHandleBlockchainError_네트워크오류() {
        // Given
        RuntimeException networkError = new RuntimeException("Connection timeout");
        
        // When
        String result = ReflectionTestUtils.invokeMethod(campaignService, 
                "classifyAndHandleBlockchainError", networkError);
        
        // Then
        assertTrue(result.contains("네트워크 연결 오류"));
        assertTrue(result.contains("Connection timeout"));
    }
    
    @Test
    void classifyAndHandleBlockchainError_가스오류() {
        // Given
        RuntimeException gasError = new RuntimeException("Out of gas");
        
        // When
        String result = ReflectionTestUtils.invokeMethod(campaignService, 
                "classifyAndHandleBlockchainError", gasError);
        
        // Then
        assertTrue(result.contains("가스 부족 오류"));
        assertTrue(result.contains("Out of gas"));
    }
    
    @Test
    void classifyAndHandleBlockchainError_스마트컨트랙트오류() {
        // Given
        RuntimeException contractError = new RuntimeException("Transaction reverted");
        
        // When
        String result = ReflectionTestUtils.invokeMethod(campaignService, 
                "classifyAndHandleBlockchainError", contractError);
        
        // Then
        assertTrue(result.contains("스마트 컨트랙트 실행 오류"));
        assertTrue(result.contains("Transaction reverted"));
    }
    
    @Test
    void classifyAndHandleBlockchainError_기타오류() {
        // Given
        RuntimeException otherError = new RuntimeException("Unknown error");
        
        // When
        String result = ReflectionTestUtils.invokeMethod(campaignService, 
                "classifyAndHandleBlockchainError", otherError);
        
        // Then
        assertTrue(result.contains("블록체인 오류"));
        assertTrue(result.contains("Unknown error"));
    }
}