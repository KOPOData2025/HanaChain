package com.hanachain.hanachainbackend.dto.campaign;

import com.hanachain.hanachainbackend.entity.Campaign;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CampaignUpdateRequest {
    
    @Size(max = 200, message = "캠페인 제목은 200자를 초과할 수 없습니다.")
    private String title;
    
    private String description;
    
    @DecimalMin(value = "1000", message = "목표 금액은 최소 1,000원 이상이어야 합니다.")
    @DecimalMax(value = "1000000000", message = "목표 금액은 최대 10억원을 초과할 수 없습니다.")
    private BigDecimal targetAmount;
    
    @Size(max = 500, message = "이미지 URL은 500자를 초과할 수 없습니다.")
    private String imageUrl;
    
    private Campaign.CampaignCategory category;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    private Campaign.CampaignStatus status;
}
