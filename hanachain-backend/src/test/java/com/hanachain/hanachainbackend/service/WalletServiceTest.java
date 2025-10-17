package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.UserWallet;
import com.hanachain.hanachainbackend.exception.*;
import com.hanachain.hanachainbackend.repository.UserWalletRepository;
import com.hanachain.hanachainbackend.service.blockchain.BlockchainService;
import com.hanachain.hanachainbackend.service.impl.WalletServiceImpl;
import com.hanachain.hanachainbackend.util.WalletEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetBalance;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService 테스트")
class WalletServiceTest {
    
    @Mock
    private UserWalletRepository userWalletRepository;
    
    @Mock
    private BlockchainService blockchainService;
    
    @Mock
    private Web3j web3j;
    
    @InjectMocks
    private WalletServiceImpl walletService;
    
    private User testUser;
    private UserWallet testWallet;
    private UserWallet externalWallet;
    
    @BeforeEach
    void setUp() {
        // 테스트 설정
        ReflectionTestUtils.setField(walletService, "chainId", 11155111L);
        ReflectionTestUtils.setField(walletService, "networkName", "Sepolia");
        
        // 테스트 사용자 생성
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("testuser")
                .build();
        
        // 내부 생성 지갑
        testWallet = UserWallet.builder()
                .id(1L)
                .user(testUser)
                .walletAddress("0x1234567890123456789012345678901234567890")
                .walletType(UserWallet.WalletType.INTERNAL)
                .isPrimary(true)
                .isVerified(true)
                .creationMethod(UserWallet.CreationMethod.INTERNAL)
                .encryptionMethod("AES-256-GCM")
                .build();
        
        // 외부 지갑
        externalWallet = UserWallet.builder()
                .id(2L)
                .user(testUser)
                .walletAddress("0x9876543210987654321098765432109876543210")
                .walletType(UserWallet.WalletType.METAMASK)
                .isPrimary(false)
                .isVerified(false)
                .creationMethod(UserWallet.CreationMethod.EXTERNAL)
                .build();
    }
    
    // =========================== 지갑 생성 테스트 ===========================
    
    @Test
    @DisplayName("새 지갑 생성 성공")
    void createWallet_Success() {
        // Given
        String password = "StrongPassword123!";
        
        when(userWalletRepository.existsByWalletAddress(anyString())).thenReturn(false);
        when(userWalletRepository.save(any(UserWallet.class))).thenReturn(testWallet);
        
        // When
        UserWallet result = walletService.createWallet(testUser, password, true);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getWalletAddress()).isNotNull();
        assertThat(result.getCreationMethod()).isEqualTo(UserWallet.CreationMethod.INTERNAL);
        assertThat(result.getWalletType()).isEqualTo(UserWallet.WalletType.INTERNAL);
        
        verify(userWalletRepository).unsetAllPrimaryWallets(testUser);
        verify(userWalletRepository).save(any(UserWallet.class));
    }
    
    @Test
    @DisplayName("약한 비밀번호로 지갑 생성 실패")
    void createWallet_WeakPassword_ThrowsException() {
        // Given
        String weakPassword = "123";
        
        // When & Then
        assertThatThrownBy(() -> walletService.createWallet(testUser, weakPassword, false))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("비밀번호는 최소 8자 이상이어야 합니다");
    }
    
    @Test
    @DisplayName("빈 비밀번호로 지갑 생성 실패")
    void createWallet_EmptyPassword_ThrowsException() {
        // Given
        String emptyPassword = "";
        
        // When & Then
        assertThatThrownBy(() -> walletService.createWallet(testUser, emptyPassword, false))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("비밀번호는 필수입니다");
    }
    
    // =========================== 외부 지갑 연결 테스트 ===========================
    
    @Test
    @DisplayName("외부 지갑 연결 성공")
    void connectExternalWallet_Success() {
        // Given
        String walletAddress = "0x1234567890123456789012345678901234567890";
        UserWallet.WalletType walletType = UserWallet.WalletType.METAMASK;
        
        when(userWalletRepository.existsByWalletAddress(anyString())).thenReturn(false);
        when(userWalletRepository.existsByUserAndWalletAddress(any(), anyString())).thenReturn(false);
        when(userWalletRepository.save(any(UserWallet.class))).thenReturn(externalWallet);
        
        // When
        UserWallet result = walletService.connectExternalWallet(testUser, walletAddress, walletType, false);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCreationMethod()).isEqualTo(UserWallet.CreationMethod.EXTERNAL);
        assertThat(result.getWalletType()).isEqualTo(UserWallet.WalletType.METAMASK);
        assertThat(result.getIsVerified()).isFalse();
        
        verify(userWalletRepository).save(any(UserWallet.class));
    }
    
    @Test
    @DisplayName("유효하지 않은 주소로 외부 지갑 연결 실패")
    void connectExternalWallet_InvalidAddress_ThrowsException() {
        // Given
        String invalidAddress = "invalid_address";
        
        // When & Then
        assertThatThrownBy(() -> 
                walletService.connectExternalWallet(testUser, invalidAddress, UserWallet.WalletType.METAMASK, false))
                .isInstanceOf(WalletValidationException.class);
    }
    
    @Test
    @DisplayName("이미 등록된 주소로 외부 지갑 연결 실패")
    void connectExternalWallet_DuplicateAddress_ThrowsException() {
        // Given
        String walletAddress = "0x1234567890123456789012345678901234567890";
        
        when(userWalletRepository.existsByWalletAddress(anyString())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> 
                walletService.connectExternalWallet(testUser, walletAddress, UserWallet.WalletType.METAMASK, false))
                .isInstanceOf(DuplicateWalletException.class);
    }
    
    // =========================== 지갑 조회 테스트 ===========================
    
    @Test
    @DisplayName("사용자 지갑 목록 조회 성공")
    void getUserWallets_Success() {
        // Given
        List<UserWallet> wallets = Arrays.asList(testWallet, externalWallet);
        when(userWalletRepository.findByUserOrderByIsPrimaryDescCreatedAtDesc(testUser))
                .thenReturn(wallets);
        
        // When
        List<UserWallet> result = walletService.getUserWallets(testUser);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(testWallet);
        assertThat(result.get(1)).isEqualTo(externalWallet);
    }
    
    @Test
    @DisplayName("주 지갑 조회 성공")
    void getPrimaryWallet_Success() {
        // Given
        when(userWalletRepository.findByUserAndIsPrimaryTrue(testUser))
                .thenReturn(Optional.of(testWallet));
        
        // When
        Optional<UserWallet> result = walletService.getPrimaryWallet(testUser);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testWallet);
        assertThat(result.get().getIsPrimary()).isTrue();
    }
    
    @Test
    @DisplayName("주 지갑이 없는 경우 빈 Optional 반환")
    void getPrimaryWallet_NotFound_ReturnsEmpty() {
        // Given
        when(userWalletRepository.findByUserAndIsPrimaryTrue(testUser))
                .thenReturn(Optional.empty());
        
        // When
        Optional<UserWallet> result = walletService.getPrimaryWallet(testUser);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    // =========================== 지갑 검증 테스트 ===========================
    
    @Test
    @DisplayName("유효한 지갑 주소 검증 성공")
    void isValidWalletAddress_ValidAddress_ReturnsTrue() {
        // Given
        String validAddress = "0x1234567890123456789012345678901234567890";
        
        // When
        boolean result = walletService.isValidWalletAddress(validAddress);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("유효하지 않은 지갑 주소 검증 실패")
    void isValidWalletAddress_InvalidAddress_ReturnsFalse() {
        // Given
        String invalidAddress = "invalid_address";
        
        // When
        boolean result = walletService.isValidWalletAddress(invalidAddress);
        
        // Then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("빈 주소 검증 실패")
    void isValidWalletAddress_EmptyAddress_ReturnsFalse() {
        // When
        boolean result1 = walletService.isValidWalletAddress("");
        boolean result2 = walletService.isValidWalletAddress(null);
        
        // Then
        assertThat(result1).isFalse();
        assertThat(result2).isFalse();
    }
    
    // =========================== 지갑 상태 관리 테스트 ===========================
    
    @Test
    @DisplayName("지갑 검증 완료 처리 성공")
    void verifyWallet_Success() {
        // Given
        when(userWalletRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(externalWallet));
        when(userWalletRepository.save(any(UserWallet.class)))
                .thenReturn(externalWallet);
        
        // When
        walletService.verifyWallet(1L, testUser);
        
        // Then
        verify(userWalletRepository).save(any(UserWallet.class));
    }
    
    @Test
    @DisplayName("존재하지 않는 지갑 검증 실패")
    void verifyWallet_WalletNotFound_ThrowsException() {
        // Given
        when(userWalletRepository.findByIdAndUser(999L, testUser))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> walletService.verifyWallet(999L, testUser))
                .isInstanceOf(NotFoundException.class);
    }
    
    @Test
    @DisplayName("주 지갑 설정 성공")
    void setPrimaryWallet_Success() {
        // Given
        when(userWalletRepository.findByIdAndUser(2L, testUser))
                .thenReturn(Optional.of(externalWallet));
        when(userWalletRepository.save(any(UserWallet.class)))
                .thenReturn(externalWallet);
        
        // When
        walletService.setPrimaryWallet(2L, testUser);
        
        // Then
        verify(userWalletRepository).unsetAllPrimaryWallets(testUser);
        verify(userWalletRepository).save(any(UserWallet.class));
    }
    
    // =========================== 잔액 조회 테스트 ===========================
    
    @Test
    @DisplayName("USDC 잔액 조회 성공")
    void getUSDCBalance_Success() {
        // Given
        BigInteger expectedBalance = new BigInteger("1000000"); // 1 USDC
        when(blockchainService.getUSDCBalance(testWallet.getWalletAddress()))
                .thenReturn(expectedBalance);
        
        // When
        BigInteger result = walletService.getUSDCBalance(testWallet);
        
        // Then
        assertThat(result).isEqualTo(expectedBalance);
    }
    
    @Test
    @DisplayName("ETH 잔액 조회 성공")
    void getETHBalance_Success() throws Exception {
        // Given
        BigInteger expectedBalance = new BigInteger("1000000000000000000"); // 1 ETH
        EthGetBalance ethGetBalance = mock(EthGetBalance.class);
        Request<?, EthGetBalance> request = mock(Request.class);
        
        when(web3j.ethGetBalance(anyString(), any())).thenReturn(request);
        when(request.send()).thenReturn(ethGetBalance);
        when(ethGetBalance.getBalance()).thenReturn(expectedBalance);
        
        // When
        BigInteger result = walletService.getETHBalance(testWallet);
        
        // Then
        assertThat(result).isEqualTo(expectedBalance);
    }
    
    // =========================== 암호화/복호화 테스트 ===========================
    
    @Test
    @DisplayName("지갑 Credentials 생성 성공")
    void getCredentials_InternalWallet_Success() {
        // Given
        String password = "TestPassword123!";
        String privateKey = "0x1234567890123456789012345678901234567890123456789012345678901234";
        String encryptedPrivateKey = WalletEncryptionUtil.encrypt(privateKey, password);
        
        testWallet.setPrivateKeyInfo(encryptedPrivateKey, "AES-256-GCM");
        
        // When
        Credentials result = walletService.getCredentials(testWallet, password);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAddress()).isNotNull();
    }
    
    @Test
    @DisplayName("외부 지갑에서 Credentials 생성 실패")
    void getCredentials_ExternalWallet_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> walletService.getCredentials(externalWallet, "password"))
                .isInstanceOf(WalletDecryptionException.class)
                .hasMessageContaining("개인키 정보가 없습니다");
    }
    
    @Test
    @DisplayName("잘못된 비밀번호로 Credentials 생성 실패")
    void getCredentials_WrongPassword_ThrowsException() {
        // Given
        String correctPassword = "TestPassword123!";
        String wrongPassword = "WrongPassword";
        String privateKey = "0x1234567890123456789012345678901234567890123456789012345678901234";
        String encryptedPrivateKey = WalletEncryptionUtil.encrypt(privateKey, correctPassword);
        
        testWallet.setPrivateKeyInfo(encryptedPrivateKey, "AES-256-GCM");
        
        // When & Then
        assertThatThrownBy(() -> walletService.getCredentials(testWallet, wrongPassword))
                .isInstanceOf(WalletDecryptionException.class);
    }
    
    // =========================== 지갑 소유권 테스트 ===========================
    
    @Test
    @DisplayName("지갑 소유권 확인 성공")
    void ownsWallet_UserOwnsWallet_ReturnsTrue() {
        // Given
        String walletAddress = "0x1234567890123456789012345678901234567890";
        when(userWalletRepository.existsByUserAndWalletAddress(testUser, walletAddress.toLowerCase()))
                .thenReturn(true);
        
        // When
        boolean result = walletService.ownsWallet(testUser, walletAddress);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("지갑 소유권 확인 실패")
    void ownsWallet_UserDoesNotOwnWallet_ReturnsFalse() {
        // Given
        String walletAddress = "0x9876543210987654321098765432109876543210";
        when(userWalletRepository.existsByUserAndWalletAddress(testUser, walletAddress.toLowerCase()))
                .thenReturn(false);
        
        // When
        boolean result = walletService.ownsWallet(testUser, walletAddress);
        
        // Then
        assertThat(result).isFalse();
    }
}