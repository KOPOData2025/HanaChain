package com.hanachain.hanachainbackend.dto.organization;

import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for organization update
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationUpdateRequest {
    
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 255, message = "Organization name must be between 2 and 255 characters")
    private String name;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @Size(max = 512, message = "Image URL must not exceed 512 characters")
    private String imageUrl;
    
    @NotNull(message = "Organization status is required")
    private OrganizationStatus status;
}