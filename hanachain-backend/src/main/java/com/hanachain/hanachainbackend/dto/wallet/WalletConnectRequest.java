package com.hanachain.hanachainbackend.dto.wallet;

import com.hanachain.hanachainbackend.entity.UserWallet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 외부 지갑 연결 요청 DTO
 */
@Data
public class WalletConnectRequest {
    
    /**
     * 연결할 지갑 주소
     */
    @NotBlank(message = "지갑 주소는 필수입니다")
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "유효하지 않은 지갑 주소 형식입니다")
    private String walletAddress;
    
    /**
     * 지갑 타입
     */
    @NotNull(message = "지갑 타입은 필수입니다")
    private UserWallet.WalletType walletType;
    
    /**
     * 주 지갑으로 설정할지 여부
     */
    private Boolean isPrimary = false;
}