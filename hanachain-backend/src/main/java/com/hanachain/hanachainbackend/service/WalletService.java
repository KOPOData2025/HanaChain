package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.UserWallet;
import org.web3j.crypto.Credentials;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 지갑 관리를 담당하는 서비스 인터페이스
 * 지갑 생성, 암호화/복호화, 검증, 잔액 조회 등의 기능을 제공합니다.
 */
public interface WalletService {
    
    // =========================== 지갑 생성 및 관리 ===========================
    
    /**
     * 새로운 Ethereum 지갑을 생성하고 암호화하여 저장합니다
     * 
     * @param user 지갑을 생성할 사용자
     * @param password 개인키 암호화에 사용할 비밀번호
     * @param isPrimary 주 지갑 여부
     * @return 생성된 지갑 정보
     * @throws WalletCreationException 지갑 생성 실패 시
     */
    UserWallet createWallet(User user, String password, boolean isPrimary);
    
    /**
     * 외부 지갑 주소를 연결합니다 (MetaMask 등)
     * 
     * @param user 지갑을 연결할 사용자
     * @param walletAddress 연결할 지갑 주소
     * @param walletType 지갑 타입
     * @param isPrimary 주 지갑 여부
     * @return 연결된 지갑 정보
     * @throws WalletValidationException 지갑 주소가 유효하지 않은 경우
     * @throws DuplicateWalletException 이미 등록된 지갑인 경우
     */
    UserWallet connectExternalWallet(User user, String walletAddress, 
                                   UserWallet.WalletType walletType, boolean isPrimary);
    
    /**
     * 지갑 개인키를 복호화하여 Credentials 객체를 생성합니다
     *
     * @param userWallet 복호화할 지갑
     * @param password 복호화에 사용할 비밀번호
     * @return Web3j Credentials 객체
     * @throws WalletDecryptionException 복호화 실패 시
     */
    Credentials getCredentials(UserWallet userWallet, String password);

    /**
     * 지갑 개인키를 복호화하여 Credentials 객체를 생성합니다 (배치 작업용)
     * 시스템 마스터 키를 사용하여 암호화된 private key를 복호화합니다
     *
     * @param userWallet 복호화할 지갑
     * @return Web3j Credentials 객체
     * @throws WalletDecryptionException 복호화 실패 시
     */
    Credentials getCredentials(UserWallet userWallet);
    
    // =========================== 지갑 조회 및 검증 ===========================
    
    /**
     * 사용자의 모든 지갑을 조회합니다
     * 
     * @param user 조회할 사용자
     * @return 사용자의 지갑 리스트 (주 지갑 우선 정렬)
     */
    List<UserWallet> getUserWallets(User user);
    
    /**
     * 사용자의 주 지갑을 조회합니다
     * 
     * @param user 조회할 사용자
     * @return 주 지갑 정보 (없으면 Optional.empty())
     */
    Optional<UserWallet> getPrimaryWallet(User user);
    
    /**
     * 지갑 주소로 지갑 정보를 조회합니다
     * 
     * @param walletAddress 조회할 지갑 주소
     * @return 지갑 정보 (없으면 Optional.empty())
     */
    Optional<UserWallet> getWalletByAddress(String walletAddress);
    
    /**
     * 사용자가 특정 지갑을 소유하고 있는지 확인합니다
     * 
     * @param user 확인할 사용자
     * @param walletAddress 확인할 지갑 주소
     * @return 소유 여부
     */
    boolean ownsWallet(User user, String walletAddress);
    
    /**
     * 지갑 주소가 유효한 Ethereum 주소인지 검증합니다
     * 
     * @param walletAddress 검증할 지갑 주소
     * @return 유효성 여부
     */
    boolean isValidWalletAddress(String walletAddress);
    
    /**
     * 지갑 주소가 이미 등록되어 있는지 확인합니다
     * 
     * @param walletAddress 확인할 지갑 주소
     * @return 등록 여부
     */
    boolean isWalletAddressRegistered(String walletAddress);
    
    // =========================== 지갑 상태 관리 ===========================
    
    /**
     * 지갑을 검증 완료 상태로 변경합니다
     * 
     * @param walletId 검증할 지갑 ID
     * @param user 지갑 소유자
     * @throws NotFoundException 지갑을 찾을 수 없는 경우
     * @throws UnauthorizedException 권한이 없는 경우
     */
    void verifyWallet(Long walletId, User user);
    
    /**
     * 지갑을 주 지갑으로 설정합니다
     * 기존 주 지갑은 자동으로 해제됩니다
     * 
     * @param walletId 주 지갑으로 설정할 지갑 ID
     * @param user 지갑 소유자
     * @throws NotFoundException 지갑을 찾을 수 없는 경우
     * @throws UnauthorizedException 권한이 없는 경우
     */
    void setPrimaryWallet(Long walletId, User user);
    
    /**
     * 지갑을 삭제합니다
     * 
     * @param walletId 삭제할 지갑 ID
     * @param user 지갑 소유자
     * @throws NotFoundException 지갑을 찾을 수 없는 경우
     * @throws UnauthorizedException 권한이 없는 경우
     * @throws BusinessException 주 지갑이거나 삭제할 수 없는 경우
     */
    void deleteWallet(Long walletId, User user);
    
    // =========================== 블록체인 연동 ===========================
    
    /**
     * 지갑의 USDC 잔액을 조회합니다
     * 
     * @param userWallet 조회할 지갑
     * @return USDC 잔액 (6 decimals)
     */
    BigInteger getUSDCBalance(UserWallet userWallet);
    
    /**
     * 지갑의 ETH 잔액을 조회합니다
     * 
     * @param userWallet 조회할 지갑
     * @return ETH 잔액 (wei 단위)
     */
    BigInteger getETHBalance(UserWallet userWallet);
    
    /**
     * 지갑의 잔액 정보를 업데이트합니다
     * 
     * @param userWallet 업데이트할 지갑
     * @return 업데이트된 잔액 정보
     */
    WalletBalanceInfo updateWalletBalance(UserWallet userWallet);
    
    /**
     * 지갑으로 트랜잭션에 서명합니다
     *
     * @param userWallet 서명할 지갑
     * @param password 지갑 복호화 비밀번호
     * @param transactionData 서명할 트랜잭션 데이터
     * @return 서명된 트랜잭션
     * @throws WalletDecryptionException 복호화 실패 시
     */
    String signTransaction(UserWallet userWallet, String password, String transactionData);

    // =========================== 조직 지갑 관리 ===========================

    /**
     * 조직을 위한 새 이더리움 지갑 생성
     * @param organizationId 조직 ID
     * @return 생성된 지갑 정보
     */
    com.hanachain.hanachainbackend.entity.OrganizationWallet generateWalletForOrganization(Long organizationId);

    /**
     * 조직의 지갑 정보 조회
     * @param organizationId 조직 ID
     * @return 지갑 정보 (없으면 null)
     */
    com.hanachain.hanachainbackend.entity.OrganizationWallet getOrganizationWallet(Long organizationId);

    /**
     * 조직의 지갑 주소 조회
     * @param organizationId 조직 ID
     * @return 지갑 주소 (없으면 null)
     */
    String getOrganizationWalletAddress(Long organizationId);

    /**
     * 조직이 지갑을 가지고 있는지 확인
     * @param organizationId 조직 ID
     * @return 지갑 보유 여부
     */
    boolean hasOrganizationWallet(Long organizationId);

    // =========================== 내부 클래스 ===========================
    
    /**
     * 지갑 잔액 정보 데이터 클래스
     */
    class WalletBalanceInfo {
        private BigInteger usdcBalance;
        private BigInteger ethBalance;
        private String walletAddress;
        
        public WalletBalanceInfo() {}
        
        public WalletBalanceInfo(BigInteger usdcBalance, BigInteger ethBalance, String walletAddress) {
            this.usdcBalance = usdcBalance;
            this.ethBalance = ethBalance;
            this.walletAddress = walletAddress;
        }

        // Getter 및 Setter
        public BigInteger getUsdcBalance() { return usdcBalance; }
        public void setUsdcBalance(BigInteger usdcBalance) { this.usdcBalance = usdcBalance; }
        
        public BigInteger getEthBalance() { return ethBalance; }
        public void setEthBalance(BigInteger ethBalance) { this.ethBalance = ethBalance; }
        
        public String getWalletAddress() { return walletAddress; }
        public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
    }
}