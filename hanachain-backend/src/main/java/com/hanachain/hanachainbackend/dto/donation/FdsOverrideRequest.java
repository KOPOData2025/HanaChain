package com.hanachain.hanachainbackend.dto.donation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FDS 검증 결과 오버라이드 요청 DTO
 * 관리자가 FDS 검증 결과를 수동으로 변경할 때 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FdsOverrideRequest {

    /**
     * 액션 (approve 또는 block)
     */
    @NotBlank(message = "액션은 필수입니다")
    @Pattern(regexp = "^(approve|block)$", message = "액션은 'approve' 또는 'block'만 가능합니다")
    private String action;

    /**
     * 오버라이드 사유
     */
    @NotBlank(message = "사유는 필수입니다")
    @Size(min = 10, max = 500, message = "사유는 10자 이상 500자 이하로 입력해주세요")
    private String reason;
}
