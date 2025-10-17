package com.hanachain.hanachainbackend.dto.campaign;

import com.hanachain.hanachainbackend.entity.Campaign;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CampaignCreateRequest {
    
    @NotBlank(message = "캠페인 제목은 필수입니다.")
    @Size(max = 200, message = "캠페인 제목은 200자를 초과할 수 없습니다.")
    private String title;
    
    @Size(max = 500, message = "캠페인 부제목은 500자를 초과할 수 없습니다.")
    private String subtitle;
    
    @NotBlank(message = "캠페인 설명은 필수입니다.")
    private String description;
    
    @NotBlank(message = "주최자는 필수입니다.")
    @Size(max = 100, message = "주최자는 100자를 초과할 수 없습니다.")
    private String organizer;
    
    @NotNull(message = "목표 금액은 필수입니다.")
    @DecimalMin(value = "1000", message = "목표 금액은 최소 1,000원 이상이어야 합니다.")
    @DecimalMax(value = "1000000000", message = "목표 금액은 최대 10억원을 초과할 수 없습니다.")
    private BigDecimal targetAmount;
    
    @Size(max = 500, message = "이미지 URL은 500자를 초과할 수 없습니다.")
    private String imageUrl;
    
    @NotNull(message = "캠페인 카테고리는 필수입니다.")
    private Campaign.CampaignCategory category;
    
    @NotNull(message = "시작 날짜는 필수입니다.")
    private LocalDateTime startDate;

    @NotNull(message = "종료 날짜는 필수입니다.")
    private LocalDateTime endDate;
    
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "올바른 이더리움 주소 형식이 아닙니다.")
    private String beneficiaryAddress;

    // 조직 ID (선택 사항 - 제공 시 자동으로 조직 지갑 주소를 beneficiaryAddress로 매핑)
    private Long organizationId;

    // 블록체인 즉시 등록 여부
    private boolean enableBlockchain = false;
    
    @AssertTrue(message = "종료 날짜는 시작 날짜보다 늦어야 합니다.")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true; // @NotNull로 별도 검증
        }
        return endDate.isAfter(startDate);
    }
    
    @AssertTrue(message = "캠페인 기간은 최소 1일 이상이어야 합니다.")
    public boolean isCampaignDurationValid() {
        if (startDate == null || endDate == null) {
            return true; // @NotNull로 별도 검증
        }
        return endDate.isAfter(startDate.plusDays(1));
    }
}
