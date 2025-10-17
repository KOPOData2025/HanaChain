package com.hanachain.hanachainbackend.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 지갑 서명 요청 DTO
 */
@Data
public class WalletSignRequest {
    
    /**
     * 서명할 지갑 ID
     */
    @NotNull(message = "지갑 ID는 필수입니다")
    private Long walletId;
    
    /**
     * 지갑 비밀번호
     */
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
    
    /**
     * 서명할 트랜잭션 데이터
     */
    @NotBlank(message = "트랜잭션 데이터는 필수입니다")
    private String transactionData;
}