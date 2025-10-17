package com.hanachain.hanachainbackend.dto.admin;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for permission delegation response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDelegationResponse {
    
    private Long id;
    private Long targetUserId;
    private String targetUserName;
    private String targetUserEmail;
    private User.Role systemRole;
    private OrganizationRole organizationRole;
    private Long organizationId;
    private String organizationName;
    private String reason;
    private LocalDateTime expiresAt;
    private Boolean temporary;
    private String delegatedBy;
    private LocalDateTime delegatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}