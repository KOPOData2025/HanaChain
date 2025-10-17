package com.hanachain.hanachainbackend.dto.organization;

import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating member role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberRoleUpdateRequest {
    
    @NotNull(message = "Role is required")
    private OrganizationRole role;
}