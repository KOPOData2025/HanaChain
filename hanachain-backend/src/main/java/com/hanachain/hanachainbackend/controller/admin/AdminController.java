package com.hanachain.hanachainbackend.controller.admin;

import com.hanachain.hanachainbackend.dto.admin.PermissionDelegationRequest;
import com.hanachain.hanachainbackend.dto.admin.PermissionDelegationResponse;
import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.donation.AdminDonationTrendResponse;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.security.annotation.RequiresPermission;
import com.hanachain.hanachainbackend.security.annotation.RequiresRole;
import com.hanachain.hanachainbackend.service.AdminPermissionService;
import com.hanachain.hanachainbackend.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for admin permission delegation and role management
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Management", description = "관리자 권한 위임 및 역할 관리")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminPermissionService adminPermissionService;
    private final DonationService donationService;
    
    @PostMapping("/permissions/delegate/system-role")
    @Operation(summary = "시스템 역할 위임", description = "사용자에게 시스템 역할을 위임합니다")
    @RequiresRole({User.Role.ADMIN, User.Role.SUPER_ADMIN})
    public ResponseEntity<ApiResponse<PermissionDelegationResponse>> delegateSystemRole(
            @Valid @RequestBody PermissionDelegationRequest request,
            @AuthenticationPrincipal User adminUser) {
        
        log.info("Admin {} delegating system role {} to user {}", 
                adminUser.getId(), request.getSystemRole(), request.getTargetUserId());
        
        PermissionDelegationResponse response = adminPermissionService.delegateSystemRole(request, adminUser);
        
        return ResponseEntity.ok(ApiResponse.success("시스템 역할이 성공적으로 위임되었습니다", response));
    }
    
    @PostMapping("/permissions/delegate/organization-role")
    @Operation(summary = "단체 역할 위임", description = "사용자에게 단체 역할을 위임합니다")
    @RequiresRole({User.Role.ADMIN, User.Role.SUPER_ADMIN, User.Role.CAMPAIGN_ADMIN})
    public ResponseEntity<ApiResponse<PermissionDelegationResponse>> delegateOrganizationRole(
            @Valid @RequestBody PermissionDelegationRequest request,
            @AuthenticationPrincipal User adminUser) {
        
        log.info("Admin {} delegating organization role {} to user {} in organization {}", 
                adminUser.getId(), request.getOrganizationRole(), request.getTargetUserId(), request.getOrganizationId());
        
        PermissionDelegationResponse response = adminPermissionService.delegateOrganizationRole(request, adminUser);
        
        return ResponseEntity.ok(ApiResponse.success("단체 역할이 성공적으로 위임되었습니다", response));
    }
    
    @DeleteMapping("/permissions/revoke/system-role/{targetUserId}")
    @Operation(summary = "시스템 역할 취소", description = "사용자의 시스템 역할을 취소합니다")
    @RequiresRole({User.Role.ADMIN, User.Role.SUPER_ADMIN})
    public ResponseEntity<ApiResponse<Void>> revokeSystemRole(
            @Parameter(description = "대상 사용자 ID") @PathVariable Long targetUserId,
            @AuthenticationPrincipal User adminUser) {
        
        log.info("Admin {} revoking system role from user {}", adminUser.getId(), targetUserId);
        
        adminPermissionService.revokeSystemRole(targetUserId, adminUser);
        
        return ResponseEntity.ok(ApiResponse.success("시스템 역할이 성공적으로 취소되었습니다"));
    }
    
    @DeleteMapping("/permissions/revoke/organization-role/{targetUserId}/organization/{organizationId}")
    @Operation(summary = "단체 역할 취소", description = "사용자의 단체 역할을 취소합니다")
    @RequiresRole({User.Role.ADMIN, User.Role.SUPER_ADMIN, User.Role.CAMPAIGN_ADMIN})
    public ResponseEntity<ApiResponse<Void>> revokeOrganizationRole(
            @Parameter(description = "대상 사용자 ID") @PathVariable Long targetUserId,
            @Parameter(description = "단체 ID") @PathVariable Long organizationId,
            @AuthenticationPrincipal User adminUser) {
        
        log.info("Admin {} revoking organization role from user {} in organization {}", 
                adminUser.getId(), targetUserId, organizationId);
        
        adminPermissionService.revokeOrganizationRole(targetUserId, organizationId, adminUser);
        
        return ResponseEntity.ok(ApiResponse.success("단체 역할이 성공적으로 취소되었습니다"));
    }
    
    @GetMapping("/permissions/user/{userId}")
    @Operation(summary = "사용자 권한 조회", description = "특정 사용자의 위임된 권한을 조회합니다")
    @RequiresRole({User.Role.ADMIN, User.Role.SUPER_ADMIN, User.Role.CAMPAIGN_ADMIN})
    public ResponseEntity<ApiResponse<List<PermissionDelegationResponse>>> getUserDelegatedPermissions(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        
        List<PermissionDelegationResponse> permissions = adminPermissionService.getUserDelegatedPermissions(userId);
        
        return ResponseEntity.ok(ApiResponse.success("사용자 권한을 성공적으로 조회했습니다", permissions));
    }
    
    @GetMapping("/permissions/delegations")
    @Operation(summary = "활성 권한 위임 목록", description = "현재 활성화된 모든 권한 위임을 조회합니다")
    @RequiresRole({User.Role.ADMIN, User.Role.SUPER_ADMIN})
    public ResponseEntity<ApiResponse<List<PermissionDelegationResponse>>> getActiveDelegations() {
        
        List<PermissionDelegationResponse> delegations = adminPermissionService.getActiveDelegations();
        
        return ResponseEntity.ok(ApiResponse.success("활성 권한 위임 목록을 성공적으로 조회했습니다", delegations));
    }
    
    @GetMapping("/permissions/check/system-role/{targetRole}")
    @Operation(summary = "시스템 역할 위임 가능 여부 확인", description = "현재 관리자가 특정 시스템 역할을 위임할 수 있는지 확인합니다")
    @RequiresRole({User.Role.ADMIN, User.Role.SUPER_ADMIN, User.Role.CAMPAIGN_ADMIN})
    public ResponseEntity<ApiResponse<Boolean>> canDelegateSystemRole(
            @Parameter(description = "대상 역할") @PathVariable User.Role targetRole,
            @AuthenticationPrincipal User adminUser) {
        
        boolean canDelegate = adminPermissionService.canDelegateSystemRole(adminUser, targetRole);
        
        return ResponseEntity.ok(ApiResponse.success("권한 확인이 완료되었습니다", canDelegate));
    }
    
    @GetMapping("/permissions/check/organization-role/{organizationId}/{targetRole}")
    @Operation(summary = "단체 역할 위임 가능 여부 확인", description = "현재 관리자가 특정 단체에서 역할을 위임할 수 있는지 확인합니다")
    @RequiresRole({User.Role.ADMIN, User.Role.SUPER_ADMIN, User.Role.CAMPAIGN_ADMIN})
    public ResponseEntity<ApiResponse<Boolean>> canDelegateOrganizationRole(
            @Parameter(description = "단체 ID") @PathVariable Long organizationId,
            @Parameter(description = "대상 역할") @PathVariable String targetRole,
            @AuthenticationPrincipal User adminUser) {
        
        try {
            com.hanachain.hanachainbackend.entity.enums.OrganizationRole orgRole = 
                    com.hanachain.hanachainbackend.entity.enums.OrganizationRole.valueOf(targetRole);
            
            boolean canDelegate = adminPermissionService.canDelegateOrganizationRole(adminUser, orgRole, organizationId);
            
            return ResponseEntity.ok(ApiResponse.success("권한 확인이 완료되었습니다", canDelegate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("유효하지 않은 역할입니다: " + targetRole));
        }
    }

    @GetMapping("/donations/trends")
    @Operation(summary = "기부 금액 추이 조회", description = "관리자용 기부 금액 추이 데이터를 조회합니다")
    @RequiresRole({User.Role.ADMIN, User.Role.SUPER_ADMIN})
    public ResponseEntity<ApiResponse<AdminDonationTrendResponse>> getDonationTrends(
            @Parameter(description = "조회 기간 (7d, 30d, 3m, all)") @RequestParam(defaultValue = "30d") String period) {

        log.info("Fetching donation trends for period: {}", period);

        AdminDonationTrendResponse trends = donationService.getDonationTrends(period);

        return ResponseEntity.ok(ApiResponse.success("기부 금액 추이를 성공적으로 조회했습니다", trends));
    }
}