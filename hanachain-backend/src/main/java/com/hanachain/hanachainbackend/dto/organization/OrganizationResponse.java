package com.hanachain.hanachainbackend.dto.organization;

import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Organization entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {
    
    private Long id;
    
    @NotBlank(message = "Organization name is required")
    @Size(max = 255, message = "Organization name must not exceed 255 characters")
    private String name;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @Size(max = 512, message = "Image URL must not exceed 512 characters")
    private String imageUrl;
    
    @NotNull(message = "Organization status is required")
    private OrganizationStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields
    private Long memberCount;
    private Long adminCount;
    private Long activeCampaignCount;

    // Blockchain wallet address (read-only)
    private String walletAddress;

    // Related data (optional, loaded based on request)
    private List<OrganizationMemberResponse> members;
    private List<OrganizationCampaignSummary> campaigns;
    
    /**
     * Simplified constructor for basic organization info
     */
    public OrganizationResponse(Long id, String name, String description, String imageUrl,
                          OrganizationStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Get truncated description for list views
     */
    public String getTruncatedDescription(int maxLength) {
        if (description == null) return null;
        if (description.length() <= maxLength) return description;
        return description.substring(0, maxLength) + "...";
    }
    
    /**
     * Check if organization is active
     */
    public boolean isActive() {
        return status == OrganizationStatus.ACTIVE;
    }
}