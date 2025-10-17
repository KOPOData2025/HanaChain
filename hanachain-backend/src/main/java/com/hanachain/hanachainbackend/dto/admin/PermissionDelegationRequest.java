package com.hanachain.hanachainbackend.dto.admin;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for admin permission delegation requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDelegationRequest {
    
    @NotNull(message = "대상 사용자 ID는 필수입니다")
    private Long targetUserId;
    
    private User.Role systemRole;
    
    private OrganizationRole organizationRole;
    
    private Long organizationId;
    
    private String reason;
    
    private LocalDateTime expiresAt;
    
    @Builder.Default
    private Boolean temporary = false;
}