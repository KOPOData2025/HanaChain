package com.hanachain.hanachainbackend.dto.donation;

import com.hanachain.hanachainbackend.entity.Donation;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating donation
 * 결제 전 기부 정보를 사전 등록할 때 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationCreateRequest {
    
    @NotNull(message = "캠페인 ID는 필수입니다")
    private Long campaignId;
    
    @NotNull(message = "기부 금액은 필수입니다")
    @DecimalMin(value = "1000", message = "최소 기부 금액은 1,000원입니다")
    @DecimalMax(value = "100000000", message = "최대 기부 금액은 1억원입니다")
    private BigDecimal amount;
    
    @Size(max = 500, message = "기부 메시지는 500자 이하로 입력해주세요")
    private String message;
    
    @NotBlank(message = "결제 ID는 필수입니다")
    @Size(max = 100, message = "결제 ID는 100자 이하여야 합니다")
    private String paymentId;
    
    @NotNull(message = "결제 수단은 필수입니다")
    private Donation.PaymentMethod paymentMethod;
    
    @Builder.Default
    private Boolean anonymous = false;
    
    @Size(max = 50, message = "기부자 이름은 50자 이하로 입력해주세요")
    private String donorName;
    
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요")
    private String donorEmail;
    
    @Size(max = 20, message = "전화번호는 20자 이하로 입력해주세요")
    private String donorPhone;
    
    // 익명 기부가 아닌 경우 기부자 이름 필수 검증
    public boolean isValidDonorInfo() {
        if (anonymous) {
            return true;
        }
        return donorName != null && !donorName.trim().isEmpty();
    }
}