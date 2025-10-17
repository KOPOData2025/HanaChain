package com.hanachain.hanachainbackend.dto.wallet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 지갑 생성 요청 DTO
 */
@Data
public class WalletCreateRequest {
    
    /**
     * 지갑 암호화에 사용할 비밀번호
     */
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    private String password;
    
    /**
     * 주 지갑으로 설정할지 여부
     */
    private Boolean isPrimary = false;
}