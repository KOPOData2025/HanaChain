package com.hanachain.hanachainbackend.dto.campaign;

import com.hanachain.hanachainbackend.entity.Campaign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignListResponse {
    
    private Long id;
    private String title;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private Integer donorCount;
    private String imageUrl;
    private Campaign.CampaignStatus status;
    private Campaign.CampaignCategory category;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private String creatorName;
    private String organizer;
    private BigDecimal progressPercentage;
    private boolean isActive;
    
    public static CampaignListResponse fromEntity(Campaign campaign) {
        return CampaignListResponse.builder()
                .id(campaign.getId())
                .title(campaign.getTitle())
                .description(truncateDescription(campaign.getDescription()))
                .targetAmount(campaign.getTargetAmount())
                .currentAmount(campaign.getCurrentAmount())
                .donorCount(campaign.getDonorCount())
                .imageUrl(campaign.getImageUrl())
                .status(campaign.getStatus())
                .category(campaign.getCategory())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .createdAt(campaign.getCreatedAt())
                .creatorName(campaign.getUser().getName())
                .organizer(campaign.getOrganizer())
                .progressPercentage(campaign.getProgressPercentage())
                .isActive(campaign.isActive())
                .build();
    }
    
    private static String truncateDescription(String description) {
        if (description == null) {
            return null;
        }
        return description.length() > 100 ? description.substring(0, 100) + "..." : description;
    }
}
