package com.hanachain.hanachainbackend.dto.wallet;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 지갑 검증 요청 DTO
 */
@Data
public class WalletVerifyRequest {
    
    /**
     * 검증할 지갑 ID
     */
    @NotNull(message = "지갑 ID는 필수입니다")
    private Long walletId;
}