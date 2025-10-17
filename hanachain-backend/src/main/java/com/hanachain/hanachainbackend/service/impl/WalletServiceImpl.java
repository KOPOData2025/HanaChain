package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.UserWallet;
import com.hanachain.hanachainbackend.exception.*;
import com.hanachain.hanachainbackend.repository.UserWalletRepository;
import com.hanachain.hanachainbackend.service.WalletService;
import com.hanachain.hanachainbackend.service.blockchain.BlockchainService;
import com.hanachain.hanachainbackend.util.WalletEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 지갑 관리 서비스 구현체
 * Ethereum 지갑 생성, 암호화, 관리 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WalletServiceImpl implements WalletService {
    
    private final UserWalletRepository userWalletRepository;
    private final BlockchainService blockchainService;
    private final Web3j web3j;
    
    @Value("${blockchain.network.chain-id}")
    private Long chainId;
    
    @Value("${blockchain.network.name}")
    private String networkName;

    @Value("${wallet.master.password}")
    private String masterPassword;

    // =========================== 지갑 생성 및 관리 ===========================
    
    @Override
    public UserWallet createWallet(User user, String password, boolean isPrimary) {
        log.info("Creating new wallet for user: {}", user.getId());
        
        // 비밀번호 강도 검증
        validatePassword(password);
        
        try {
            // 새로운 키 쌍 생성
            BigInteger privateKey = Keys.createEcKeyPair().getPrivateKey();
            String privateKeyHex = Numeric.toHexStringWithPrefix(privateKey);
            
            // 지갑 주소 생성
            Credentials credentials = Credentials.create(privateKeyHex);
            String walletAddress = credentials.getAddress();
            
            // 중복 주소 확인
            if (isWalletAddressRegistered(walletAddress)) {
                log.warn("Generated wallet address already exists: {}", walletAddress);
                throw WalletCreationException.addressGenerationFailed();
            }
            
            // 개인키 암호화 (시스템 마스터 비밀번호 사용)
            String encryptedPrivateKey = WalletEncryptionUtil.encrypt(privateKeyHex, masterPassword);
            
            // 기존 주 지갑 해제 (새 지갑이 주 지갑인 경우)
            if (isPrimary) {
                userWalletRepository.unsetAllPrimaryWallets(user);
            }
            
            // 지갑 엔티티 생성 및 저장
            UserWallet wallet = UserWallet.builder()
                    .user(user)
                    .walletAddress(walletAddress)
                    .walletType(UserWallet.WalletType.INTERNAL)
                    .isPrimary(isPrimary)
                    .isVerified(true) // 내부 생성 지갑은 자동 검증
                    .chainId(chainId.intValue())
                    .chainName(networkName)
                    .creationMethod(UserWallet.CreationMethod.INTERNAL)
                    .encryptionMethod("AES-256-GCM")
                    .build();
            
            wallet.setPrivateKeyInfo(encryptedPrivateKey, "AES-256-GCM");
            wallet.verify(); // 검증 완료 처리
            
            UserWallet savedWallet = userWalletRepository.save(wallet);
            
            log.info("Successfully created wallet for user: {}, address: {}", 
                    user.getId(), savedWallet.getShortAddress());
            
            return savedWallet;
            
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            log.error("Failed to generate wallet keys for user: {}", user.getId(), e);
            throw WalletCreationException.keyGenerationFailed();
        } catch (Exception e) {
            log.error("Failed to create wallet for user: {}", user.getId(), e);
            throw WalletCreationException.databaseSaveFailed();
        }
    }
    
    @Override
    public UserWallet connectExternalWallet(User user, String walletAddress, 
                                          UserWallet.WalletType walletType, boolean isPrimary) {
        log.info("Connecting external wallet for user: {}, address: {}", user.getId(), walletAddress);
        
        // 지갑 주소 유효성 검증
        if (!isValidWalletAddress(walletAddress)) {
            throw WalletValidationException.invalidAddress();
        }
        
        // 중복 검증
        if (isWalletAddressRegistered(walletAddress)) {
            throw DuplicateWalletException.alreadyRegistered(walletAddress);
        }
        
        if (ownsWallet(user, walletAddress)) {
            throw DuplicateWalletException.userAlreadyOwns(walletAddress);
        }
        
        // 기존 주 지갑 해제 (새 지갑이 주 지갑인 경우)
        if (isPrimary) {
            userWalletRepository.unsetAllPrimaryWallets(user);
        }
        
        // 지갑 엔티티 생성 및 저장
        UserWallet wallet = UserWallet.builder()
                .user(user)
                .walletAddress(walletAddress.toLowerCase()) // 소문자로 저장
                .walletType(walletType)
                .isPrimary(isPrimary)
                .isVerified(false) // 외부 지갑은 별도 검증 필요
                .chainId(chainId.intValue())
                .chainName(networkName)
                .creationMethod(UserWallet.CreationMethod.EXTERNAL)
                .build();
        
        UserWallet savedWallet = userWalletRepository.save(wallet);
        
        log.info("Successfully connected external wallet for user: {}, address: {}", 
                user.getId(), savedWallet.getShortAddress());
        
        return savedWallet;
    }
    
    @Override
    public Credentials getCredentials(UserWallet userWallet, String password) {
        if (userWallet.isExternalWallet()) {
            throw WalletDecryptionException.noPrivateKey();
        }

        if (!StringUtils.hasText(userWallet.getEncryptedPrivateKey())) {
            throw WalletDecryptionException.noPrivateKey();
        }

        try {
            // 시스템 마스터 비밀번호로 복호화 (사용자 password 파라미터는 무시됨)
            String privateKeyHex = WalletEncryptionUtil.decrypt(
                    userWallet.getEncryptedPrivateKey(), masterPassword);
            return Credentials.create(privateKeyHex);
        } catch (WalletDecryptionException e) {
            log.warn("Failed to decrypt wallet credentials for wallet: {}",
                    userWallet.getShortAddress());
            throw e;
        }
    }

    @Override
    public Credentials getCredentials(UserWallet userWallet) {
        if (userWallet.isExternalWallet()) {
            throw WalletDecryptionException.noPrivateKey();
        }

        if (!StringUtils.hasText(userWallet.getEncryptedPrivateKey())) {
            throw WalletDecryptionException.noPrivateKey();
        }

        try {
            // 배치 작업용: 시스템 마스터 비밀번호로 복호화
            String privateKeyHex = WalletEncryptionUtil.decrypt(
                    userWallet.getEncryptedPrivateKey(), masterPassword);
            return Credentials.create(privateKeyHex);
        } catch (WalletDecryptionException e) {
            log.error("Failed to decrypt wallet for batch operation: {}",
                    userWallet.getShortAddress(), e);
            throw new BlockchainException(
                "배치 작업용 지갑 복호화 실패: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.WALLET_ERROR,
                e
            );
        } catch (Exception e) {
            log.error("Failed to get credentials for wallet: {}",
                    userWallet.getShortAddress(), e);
            throw new BlockchainException(
                "지갑 인증 정보 조회 실패: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.WALLET_ERROR,
                e
            );
        }
    }

    // =========================== 지갑 조회 및 검증 ===========================
    
    @Override
    @Transactional(readOnly = true)
    public List<UserWallet> getUserWallets(User user) {
        return userWalletRepository.findByUserOrderByIsPrimaryDescCreatedAtDesc(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<UserWallet> getPrimaryWallet(User user) {
        return userWalletRepository.findByUserAndIsPrimaryTrue(user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<UserWallet> getWalletByAddress(String walletAddress) {
        if (!StringUtils.hasText(walletAddress)) {
            return Optional.empty();
        }
        return userWalletRepository.findByWalletAddress(walletAddress.toLowerCase());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean ownsWallet(User user, String walletAddress) {
        if (!StringUtils.hasText(walletAddress)) {
            return false;
        }
        return userWalletRepository.existsByUserAndWalletAddress(user, walletAddress.toLowerCase());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isValidWalletAddress(String walletAddress) {
        if (!StringUtils.hasText(walletAddress)) {
            return false;
        }
        
        try {
            // 기본 형식 검증 (0x + 40자리 16진수)
            if (!walletAddress.matches("^0x[a-fA-F0-9]{40}$")) {
                return false;
            }
            
            // Web3j를 사용한 체크섬 검증
            return WalletUtils.isValidAddress(walletAddress);
        } catch (Exception e) {
            log.debug("Invalid wallet address format: {}", walletAddress);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isWalletAddressRegistered(String walletAddress) {
        if (!StringUtils.hasText(walletAddress)) {
            return false;
        }
        return userWalletRepository.existsByWalletAddress(walletAddress.toLowerCase());
    }
    
    // =========================== 지갑 상태 관리 ===========================
    
    @Override
    public void verifyWallet(Long walletId, User user) {
        UserWallet wallet = getUserWalletOrThrow(walletId, user);
        
        if (!wallet.getIsVerified()) {
            wallet.verify();
            userWalletRepository.save(wallet);
            log.info("Wallet verified: {} for user: {}", wallet.getShortAddress(), user.getId());
        }
    }
    
    @Override
    public void setPrimaryWallet(Long walletId, User user) {
        UserWallet wallet = getUserWalletOrThrow(walletId, user);
        
        // 기존 주 지갑 해제
        userWalletRepository.unsetAllPrimaryWallets(user);
        
        // 새 주 지갑 설정
        wallet.setAsPrimary();
        userWalletRepository.save(wallet);
        
        log.info("Primary wallet set: {} for user: {}", wallet.getShortAddress(), user.getId());
    }
    
    @Override
    public void deleteWallet(Long walletId, User user) {
        UserWallet wallet = getUserWalletOrThrow(walletId, user);
        
        // 주 지갑 삭제 방지
        if (wallet.getIsPrimary()) {
            long walletCount = userWalletRepository.countByUser(user);
            if (walletCount > 1) {
                throw new BusinessException("주 지갑을 삭제하기 전에 다른 지갑을 주 지갑으로 설정해주세요");
            }
        }
        
        // 보안을 위해 개인키 정보 제거 후 삭제
        if (wallet.isInternalWallet()) {
            wallet.clearPrivateKeyInfo();
            userWalletRepository.save(wallet);
        }
        
        userWalletRepository.delete(wallet);
        log.info("Wallet deleted: {} for user: {}", wallet.getShortAddress(), user.getId());
    }
    
    // =========================== 블록체인 연동 ===========================
    
    @Override
    @Transactional(readOnly = true)
    public BigInteger getUSDCBalance(UserWallet userWallet) {
        try {
            return blockchainService.getUSDCBalance(userWallet.getWalletAddress());
        } catch (Exception e) {
            log.error("Failed to get USDC balance for wallet: {}", 
                    userWallet.getShortAddress(), e);
            return BigInteger.ZERO;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigInteger getETHBalance(UserWallet userWallet) {
        try {
            EthGetBalance ethGetBalance = web3j.ethGetBalance(
                    userWallet.getWalletAddress(), 
                    DefaultBlockParameterName.LATEST
            ).send();
            
            return ethGetBalance.getBalance();
        } catch (Exception e) {
            log.error("Failed to get ETH balance for wallet: {}", 
                    userWallet.getShortAddress(), e);
            return BigInteger.ZERO;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public WalletBalanceInfo updateWalletBalance(UserWallet userWallet) {
        BigInteger usdcBalance = getUSDCBalance(userWallet);
        BigInteger ethBalance = getETHBalance(userWallet);
        
        return new WalletBalanceInfo(usdcBalance, ethBalance, userWallet.getWalletAddress());
    }
    
    @Override
    @Transactional(readOnly = true)
    public String signTransaction(UserWallet userWallet, String password, String transactionData) {
        if (userWallet.isExternalWallet()) {
            throw new BusinessException("외부 지갑은 플랫폼에서 직접 서명할 수 없습니다");
        }
        
        try {
            Credentials credentials = getCredentials(userWallet, password);
            
            // TODO: 실제 트랜잭션 서명 로직 구현
            // 현재는 예시로 트랜잭션 데이터만 반환
            return "signed_" + transactionData + "_with_" + credentials.getAddress();
            
        } catch (Exception e) {
            log.error("Failed to sign transaction for wallet: {}", 
                    userWallet.getShortAddress(), e);
            throw new BusinessException("트랜잭션 서명에 실패했습니다");
        }
    }
    
    // =========================== 헬퍼 메서드 ===========================
    
    /**
     * 사용자의 지갑을 조회하고 권한을 확인합니다
     */
    private UserWallet getUserWalletOrThrow(Long walletId, User user) {
        Optional<UserWallet> walletOpt = userWalletRepository.findByIdAndUser(walletId, user);
        
        if (walletOpt.isEmpty()) {
            throw new NotFoundException("지갑을 찾을 수 없습니다");
        }
        
        return walletOpt.get();
    }
    
    /**
     * 비밀번호 강도를 검증합니다
     */
    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new ValidationException("비밀번호는 필수입니다");
        }

        if (password.length() < 8) {
            throw new ValidationException("비밀번호는 최소 8자 이상이어야 합니다");
        }

        int strength = WalletEncryptionUtil.getPasswordStrength(password);
        if (strength < 6) {
            throw new ValidationException("비밀번호가 너무 약합니다. 대소문자, 숫자, 특수문자를 조합해주세요");
        }
    }

    // =========================== 조직 지갑 관리 구현 ===========================

    @Override
    public com.hanachain.hanachainbackend.entity.OrganizationWallet generateWalletForOrganization(Long organizationId) {
        log.info("Generating wallet for organization: {}", organizationId);

        // 조직 존재 확인은 OrganizationService에서 처리
        // 이미 지갑이 있는지 확인
        if (hasOrganizationWallet(organizationId)) {
            throw new BusinessException("이 조직은 이미 지갑을 보유하고 있습니다");
        }

        try {
            // 새로운 키 쌍 생성
            BigInteger privateKey = Keys.createEcKeyPair().getPrivateKey();
            String privateKeyHex = Numeric.toHexStringWithPrefix(privateKey);

            // 지갑 주소 생성
            Credentials credentials = Credentials.create(privateKeyHex);
            String walletAddress = credentials.getAddress();

            // 개인키 암호화 (시스템 마스터 비밀번호 사용)
            String encryptedPrivateKey = WalletEncryptionUtil.encrypt(privateKeyHex, masterPassword);

            // Organization 엔티티 조회 (외부에서 주입받아야 하므로 생략)
            // 대신 OrganizationService에서 이 메서드를 호출하고 설정해야 함

            log.info("Successfully generated wallet for organization: {}, address: {}",
                    organizationId, walletAddress);

            // OrganizationWallet 엔티티 생성 (Organization은 외부에서 설정)
            com.hanachain.hanachainbackend.entity.OrganizationWallet wallet =
                    com.hanachain.hanachainbackend.entity.OrganizationWallet.builder()
                    .walletAddress(walletAddress)
                    .privateKeyEncrypted(encryptedPrivateKey)
                    .walletType("ETHEREUM")
                    .isActive(true)
                    .build();

            return wallet;

        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            log.error("Failed to generate wallet keys for organization: {}", organizationId, e);
            throw WalletCreationException.keyGenerationFailed();
        } catch (Exception e) {
            log.error("Failed to create wallet for organization: {}", organizationId, e);
            throw WalletCreationException.databaseSaveFailed();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public com.hanachain.hanachainbackend.entity.OrganizationWallet getOrganizationWallet(Long organizationId) {
        // OrganizationWalletRepository는 의존성 주입이 필요 (추가 필요)
        // 임시로 null 반환 - OrganizationService에서 처리하도록 위임
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public String getOrganizationWalletAddress(Long organizationId) {
        com.hanachain.hanachainbackend.entity.OrganizationWallet wallet = getOrganizationWallet(organizationId);
        return wallet != null ? wallet.getWalletAddress() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOrganizationWallet(Long organizationId) {
        // OrganizationWalletRepository 의존성 필요
        return false;
    }
}