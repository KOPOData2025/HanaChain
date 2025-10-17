package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWallet extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_wallet_seq")
    @SequenceGenerator(name = "user_wallet_seq", sequenceName = "user_wallet_sequence", allocationSize = 1)
    private Long id;
    
    /**
     * 지갑 소유자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 지갑 주소 (0x로 시작하는 이더리움 주소)
     */
    @Column(name = "wallet_address", nullable = false, unique = true, length = 50)
    private String walletAddress;
    
    /**
     * 지갑 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false, length = 30)
    @Builder.Default
    private WalletType walletType = WalletType.METAMASK;
    
    /**
     * 주 지갑 여부
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;
    
    /**
     * 검증 완료 여부
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
    
    /**
     * 검증 완료 시간
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    /**
     * 체인 ID (예: 1 = Ethereum Mainnet, 11155111 = Sepolia)
     */
    @Column(name = "chain_id")
    private Integer chainId;
    
    /**
     * 체인 이름
     */
    @Column(name = "chain_name", length = 50)
    private String chainName;
    
    /**
     * 암호화된 개인키 (내부 생성 지갑만 해당)
     * 외부 지갑(MetaMask 등)의 경우 null
     */
    @Lob
    @Column(name = "encrypted_private_key")
    private String encryptedPrivateKey;
    
    /**
     * 개인키 암호화 방식 (예: AES-256-GCM)
     */
    @Column(name = "encryption_method", length = 50)
    @Builder.Default
    private String encryptionMethod = "AES-256-GCM";
    
    /**
     * 지갑 생성 방식
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "creation_method", nullable = false, length = 30)
    @Builder.Default
    private CreationMethod creationMethod = CreationMethod.EXTERNAL;
    
    // ===== 헬퍼 메서드 =====
    
    /**
     * 지갑 검증 완료 처리
     */
    public void verify() {
        this.isVerified = true;
        this.verifiedAt = LocalDateTime.now();
    }
    
    /**
     * 주 지갑으로 설정
     */
    public void setAsPrimary() {
        this.isPrimary = true;
    }
    
    /**
     * 주 지갑 해제
     */
    public void unsetAsPrimary() {
        this.isPrimary = false;
    }
    
    /**
     * 체인 정보 업데이트
     */
    public void updateChainInfo(Integer chainId, String chainName) {
        this.chainId = chainId;
        this.chainName = chainName;
    }
    
    /**
     * 지갑 주소 포맷 검증
     */
    public boolean isValidAddress() {
        return walletAddress != null && 
               walletAddress.matches("^0x[a-fA-F0-9]{40}$");
    }
    
    /**
     * 짧은 형식의 지갑 주소 반환 (예: 0x1234...5678)
     */
    public String getShortAddress() {
        if (walletAddress == null || walletAddress.length() < 10) {
            return walletAddress;
        }
        return walletAddress.substring(0, 6) + "..." + 
               walletAddress.substring(walletAddress.length() - 4);
    }
    
    /**
     * 내부 생성 지갑인지 확인
     */
    public boolean isInternalWallet() {
        return creationMethod == CreationMethod.INTERNAL && encryptedPrivateKey != null;
    }
    
    /**
     * 외부 지갑인지 확인
     */
    public boolean isExternalWallet() {
        return creationMethod == CreationMethod.EXTERNAL;
    }
    
    /**
     * 서명 가능한 지갑인지 확인
     */
    public boolean canSign() {
        return isInternalWallet();
    }
    
    /**
     * 개인키 정보 설정 (내부 생성 지갑용)
     */
    public void setPrivateKeyInfo(String encryptedPrivateKey, String encryptionMethod) {
        this.encryptedPrivateKey = encryptedPrivateKey;
        this.encryptionMethod = encryptionMethod;
        this.creationMethod = CreationMethod.INTERNAL;
    }
    
    /**
     * 개인키 정보 제거 (보안상 필요시)
     */
    public void clearPrivateKeyInfo() {
        this.encryptedPrivateKey = null;
        this.encryptionMethod = null;
    }
    
    public enum WalletType {
        METAMASK,
        WALLETCONNECT,
        COINBASE_WALLET,
        TRUST_WALLET,
        RAINBOW,
        ARGENT,
        INTERNAL,  // 내부 생성 지갑
        OTHER
    }
    
    public enum CreationMethod {
        INTERNAL,   // 플랫폼에서 생성한 지갑
        EXTERNAL    // 외부에서 연결한 지갑
    }
}