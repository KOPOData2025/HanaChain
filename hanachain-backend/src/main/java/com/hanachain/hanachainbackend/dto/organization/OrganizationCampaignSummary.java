package com.hanachain.hanachainbackend.dto.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Organization campaign summary information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCampaignSummary {
    
    private Long id;
    private String title;
    private String status;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    
    /**
     * Calculate funding percentage
     */
    public BigDecimal getFundingPercentage() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (currentAmount == null) {
            return BigDecimal.ZERO;
        }
        return currentAmount.divide(targetAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * Check if campaign is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}