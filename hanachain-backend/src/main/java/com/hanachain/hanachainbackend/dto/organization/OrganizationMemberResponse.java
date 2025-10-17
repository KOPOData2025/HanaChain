package com.hanachain.hanachainbackend.dto.organization;

import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Internal DTO for organization member information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberResponse {
    
    private Long id; // OrganizationUser ID
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private OrganizationRole role;
    private LocalDateTime joinedAt; // createdAt from OrganizationUser
    private boolean active; // user.enabled && deletedAt == null
    
    /**
     * Check if member is admin
     */
    public boolean isAdmin() {
        return role == OrganizationRole.ORG_ADMIN;
    }
    
    /**
     * Get role display name
     */
    public String getRoleDisplayName() {
        return role != null ? role.getDisplayName() : null;
    }
}