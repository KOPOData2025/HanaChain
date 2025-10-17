package com.hanachain.hanachainbackend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanachain.hanachainbackend.dto.wallet.WalletConnectRequest;
import com.hanachain.hanachainbackend.dto.wallet.WalletCreateRequest;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.UserWallet;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.repository.UserWalletRepository;
import com.hanachain.hanachainbackend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("WalletService 통합 테스트")
class WalletIntegrationTest {
    
    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserWalletRepository userWalletRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    private User testUser;
    private String authToken;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        // 테스트 사용자 생성
        testUser = User.builder()
                .email("test@example.com")
                .nickname("testuser")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .build();
        
        testUser = userRepository.save(testUser);
        
        // JWT 토큰 생성
        authToken = jwtTokenProvider.createToken(testUser.getEmail());
    }
    
    @Test
    @DisplayName("새 지갑 생성 API 테스트")
    @Transactional
    void createWallet_Success() throws Exception {
        // Given
        WalletCreateRequest request = new WalletCreateRequest();
        request.setPassword("StrongPassword123!");
        request.setIsPrimary(true);
        
        // When & Then
        mockMvc.perform(post("/api/wallets/create")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.walletType").value("INTERNAL"))
                .andExpect(jsonPath("$.data.creationMethod").value("INTERNAL"))
                .andExpect(jsonPath("$.data.isPrimary").value(true))
                .andExpect(jsonPath("$.data.canSign").value(true));
        
        // 데이터베이스 검증
        var wallets = userWalletRepository.findByUserOrderByIsPrimaryDescCreatedAtDesc(testUser);
        assertThat(wallets).hasSize(1);
        assertThat(wallets.get(0).getCreationMethod()).isEqualTo(UserWallet.CreationMethod.INTERNAL);
        assertThat(wallets.get(0).getEncryptedPrivateKey()).isNotNull();
    }
    
    @Test
    @DisplayName("외부 지갑 연결 API 테스트")
    @Transactional
    void connectWallet_Success() throws Exception {
        // Given
        WalletConnectRequest request = new WalletConnectRequest();
        request.setWalletAddress("0x1234567890123456789012345678901234567890");
        request.setWalletType(UserWallet.WalletType.METAMASK);
        request.setIsPrimary(false);
        
        // When & Then
        mockMvc.perform(post("/api/wallets/connect")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.walletType").value("METAMASK"))
                .andExpect(jsonPath("$.data.creationMethod").value("EXTERNAL"))
                .andExpect(jsonPath("$.data.isPrimary").value(false))
                .andExpect(jsonPath("$.data.canSign").value(false));
        
        // 데이터베이스 검증
        var wallets = userWalletRepository.findByUserOrderByIsPrimaryDescCreatedAtDesc(testUser);
        assertThat(wallets).hasSize(1);
        assertThat(wallets.get(0).getCreationMethod()).isEqualTo(UserWallet.CreationMethod.EXTERNAL);
        assertThat(wallets.get(0).getEncryptedPrivateKey()).isNull();
    }
    
    @Test
    @DisplayName("지갑 목록 조회 API 테스트")
    @Transactional
    void getUserWallets_Success() throws Exception {
        // Given - 테스트 지갑 생성
        UserWallet wallet1 = UserWallet.builder()
                .user(testUser)
                .walletAddress("0x1234567890123456789012345678901234567890")
                .walletType(UserWallet.WalletType.INTERNAL)
                .isPrimary(true)
                .isVerified(true)
                .creationMethod(UserWallet.CreationMethod.INTERNAL)
                .build();
        
        UserWallet wallet2 = UserWallet.builder()
                .user(testUser)
                .walletAddress("0x9876543210987654321098765432109876543210")
                .walletType(UserWallet.WalletType.METAMASK)
                .isPrimary(false)
                .isVerified(false)
                .creationMethod(UserWallet.CreationMethod.EXTERNAL)
                .build();
        
        userWalletRepository.save(wallet1);
        userWalletRepository.save(wallet2);
        
        // When & Then
        mockMvc.perform(get("/api/wallets")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].isPrimary").value(true)) // 주 지갑이 먼저
                .andExpect(jsonPath("$.data[1].isPrimary").value(false));
    }
    
    @Test
    @DisplayName("주 지갑 조회 API 테스트")
    @Transactional
    void getPrimaryWallet_Success() throws Exception {
        // Given
        UserWallet primaryWallet = UserWallet.builder()
                .user(testUser)
                .walletAddress("0x1234567890123456789012345678901234567890")
                .walletType(UserWallet.WalletType.INTERNAL)
                .isPrimary(true)
                .isVerified(true)
                .creationMethod(UserWallet.CreationMethod.INTERNAL)
                .build();
        
        userWalletRepository.save(primaryWallet);
        
        // When & Then
        mockMvc.perform(get("/api/wallets/primary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isPrimary").value(true))
                .andExpect(jsonPath("$.data.walletAddress").value("0x1234567890123456789012345678901234567890"));
    }
    
    @Test
    @DisplayName("주 지갑이 없는 경우 API 테스트")
    void getPrimaryWallet_NotFound_ReturnsNull() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/wallets/primary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }
    
    @Test
    @DisplayName("지갑 주소 유효성 검증 API 테스트")
    void validateWalletAddress_ValidAddress_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/wallets/validate/0x1234567890123456789012345678901234567890")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
    
    @Test
    @DisplayName("유효하지 않은 지갑 주소 검증 API 테스트")
    void validateWalletAddress_InvalidAddress_ReturnsFalse() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/wallets/validate/invalid_address")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));
    }
    
    @Test
    @DisplayName("약한 비밀번호로 지갑 생성 실패 테스트")
    void createWallet_WeakPassword_Fails() throws Exception {
        // Given
        WalletCreateRequest request = new WalletCreateRequest();
        request.setPassword("123"); // 약한 비밀번호
        request.setIsPrimary(true);
        
        // When & Then
        mockMvc.perform(post("/api/wallets/create")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
    
    @Test
    @DisplayName("중복 지갑 연결 실패 테스트")
    @Transactional
    void connectWallet_DuplicateAddress_Fails() throws Exception {
        // Given - 이미 등록된 지갑
        String existingAddress = "0x1234567890123456789012345678901234567890";
        UserWallet existingWallet = UserWallet.builder()
                .user(testUser)
                .walletAddress(existingAddress)
                .walletType(UserWallet.WalletType.METAMASK)
                .creationMethod(UserWallet.CreationMethod.EXTERNAL)
                .build();
        
        userWalletRepository.save(existingWallet);
        
        // 같은 주소로 연결 시도
        WalletConnectRequest request = new WalletConnectRequest();
        request.setWalletAddress(existingAddress);
        request.setWalletType(UserWallet.WalletType.TRUST_WALLET);
        request.setIsPrimary(false);
        
        // When & Then
        mockMvc.perform(post("/api/wallets/connect")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict()) // 409 Conflict
                .andExpect(jsonPath("$.success").value(false));
    }
}