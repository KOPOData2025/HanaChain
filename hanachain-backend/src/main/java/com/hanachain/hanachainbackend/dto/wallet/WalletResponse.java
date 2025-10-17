package com.hanachain.hanachainbackend.dto.wallet;

import com.hanachain.hanachainbackend.entity.UserWallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 지갑 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    
    /**
     * 지갑 ID
     */
    private Long id;
    
    /**
     * 지갑 주소
     */
    private String walletAddress;
    
    /**
     * 짧은 형식의 지갑 주소 (0x1234...5678)
     */
    private String shortAddress;
    
    /**
     * 지갑 타입
     */
    private UserWallet.WalletType walletType;
    
    /**
     * 지갑 생성 방식
     */
    private UserWallet.CreationMethod creationMethod;
    
    /**
     * 주 지갑 여부
     */
    private Boolean isPrimary;
    
    /**
     * 검증 완료 여부
     */
    private Boolean isVerified;
    
    /**
     * 검증 완료 시간
     */
    private LocalDateTime verifiedAt;
    
    /**
     * 체인 ID
     */
    private Integer chainId;
    
    /**
     * 체인 이름
     */
    private String chainName;
    
    /**
     * 서명 가능 여부
     */
    private Boolean canSign;
    
    /**
     * 생성일시
     */
    private LocalDateTime createdAt;
    
    /**
     * UserWallet 엔티티로부터 DTO 생성
     */
    public static WalletResponse from(UserWallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .walletAddress(wallet.getWalletAddress())
                .shortAddress(wallet.getShortAddress())
                .walletType(wallet.getWalletType())
                .creationMethod(wallet.getCreationMethod())
                .isPrimary(wallet.getIsPrimary())
                .isVerified(wallet.getIsVerified())
                .verifiedAt(wallet.getVerifiedAt())
                .chainId(wallet.getChainId())
                .chainName(wallet.getChainName())
                .canSign(wallet.canSign())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}