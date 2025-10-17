package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.organization.*;
import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import com.hanachain.hanachainbackend.entity.enums.Permission;
import com.hanachain.hanachainbackend.security.RequirePermission;
import com.hanachain.hanachainbackend.security.RequireOrganizationAccess;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import com.hanachain.hanachainbackend.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for organization management
 * Provides both public and admin endpoints for organization operations
 */
@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organizations", description = "Organization management endpoints")
public class OrganizationController {
    
    private final OrganizationService organizationService;
    
    // ======================== PUBLIC ENDPOINTS ========================
    
    @GetMapping
    @Operation(
        summary = "Get all organizations",
        description = "Retrieve paginated list of all organizations"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organizations retrieved successfully")
    public ResponseEntity<Page<OrganizationResponse>> getAllOrganizations(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Getting all organizations with pagination: {}", pageable);
        Page<OrganizationResponse> organizations = organizationService.getAllOrganizations(pageable);
        return ResponseEntity.ok(organizations);
    }
    
    @GetMapping("/search")
    @Operation(
        summary = "Search organizations",
        description = "Search organizations by name and/or status"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed successfully")
    public ResponseEntity<Page<OrganizationResponse>> searchOrganizations(
            @Parameter(description = "Name to search for (partial match)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Organization status filter")
            @RequestParam(required = false) OrganizationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Searching organizations with name: '{}', status: {}, pagination: {}", name, status, pageable);
        Page<OrganizationResponse> organizations = organizationService.searchOrganizations(name, status, pageable);
        return ResponseEntity.ok(organizations);
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get organization by ID",
        description = "Retrieve organization details by ID"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<OrganizationResponse> getOrganizationById(
            @Parameter(description = "Organization ID")
            @PathVariable Long id) {
        
        log.info("Getting organization by ID: {}", id);
        OrganizationResponse organization = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(organization);
    }
    
    @GetMapping("/{id}/details")
    @Operation(
        summary = "Get organization with full details",
        description = "Retrieve organization with members and campaigns details"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization details found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<OrganizationResponse> getOrganizationWithDetails(
            @Parameter(description = "Organization ID")
            @PathVariable Long id) {
        
        log.info("Getting organization details by ID: {}", id);
        OrganizationResponse organization = organizationService.getOrganizationByIdWithDetails(id);
        return ResponseEntity.ok(organization);
    }
    
    @GetMapping("/{id}/wallet")
    @Operation(
        summary = "Get organization wallet address",
        description = "Retrieve the blockchain wallet address for an organization"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Wallet address retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found or no wallet exists")
    public ResponseEntity<Map<String, String>> getOrganizationWallet(
            @Parameter(description = "Organization ID")
            @PathVariable Long id) {

        log.info("Getting wallet address for organization ID: {}", id);
        OrganizationResponse organization = organizationService.getOrganizationById(id);

        if (organization.getWalletAddress() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Organization does not have a wallet address"));
        }

        return ResponseEntity.ok(Map.of(
            "organizationId", id.toString(),
            "organizationName", organization.getName(),
            "walletAddress", organization.getWalletAddress()
        ));
    }

    @GetMapping("/{id}/members")
    @RequirePermission(value = {Permission.VIEW_OWN_ORGANIZATION_MEMBERS, Permission.VIEW_ANY_ORGANIZATION_MEMBERS},
                       organizationParam = "id")
    @Operation(
        summary = "Get organization members",
        description = "Retrieve paginated list of organization members (requires member access)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Members retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<Page<OrganizationMemberResponse>> getOrganizationMembers(
            @Parameter(description = "Organization ID")
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Getting members for organization ID: {} with pagination: {}", id, pageable);
        Page<OrganizationMemberResponse> members = organizationService.getOrganizationMembers(id, pageable);
        return ResponseEntity.ok(members);
    }
    
    // ======================== USER ENDPOINTS (Authenticated) ========================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Create new organization",
        description = "Create a new organization (requires ADMIN or SUPER_ADMIN role)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Organization created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody OrganizationCreateRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Creating organization '{}' by user ID: {}", request.getName(), userId);

        OrganizationResponse organization = organizationService.createOrganization(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(organization);
    }
    
    @GetMapping("/my-organizations")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get user's organizations",
        description = "Get all organizations where the authenticated user is a member"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organizations retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    public ResponseEntity<List<OrganizationResponse>> getMyOrganizations() {

        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Getting organizations for user ID: {}", userId);
        
        List<OrganizationResponse> organizations = organizationService.getOrganizationsForUser(userId);
        return ResponseEntity.ok(organizations);
    }
    
    @GetMapping("/my-admin-organizations")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get organizations where user is admin",
        description = "Get all organizations where the authenticated user has admin role"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organizations retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    public ResponseEntity<List<OrganizationResponse>> getMyAdminOrganizations() {

        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Getting admin organizations for user ID: {}", userId);
        
        List<OrganizationResponse> organizations = organizationService.getOrganizationsWhereUserIsAdmin(userId);
        return ResponseEntity.ok(organizations);
    }
    
    // ======================== ORGANIZATION ADMIN ENDPOINTS ========================
    
    @PutMapping("/{id}")
    @RequirePermission(value = {Permission.UPDATE_OWN_ORGANIZATION, Permission.UPDATE_ANY_ORGANIZATION},
                       organizationParam = "id")
    @Operation(
        summary = "단체 정보 수정",
        description = "단체 정보를 수정합니다 (단체 수정 권한 필요)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "단체 정보가 성공적으로 수정되었습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 부족")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "단체를 찾을 수 없음")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @Parameter(description = "단체 ID")
            @PathVariable Long id,
            @Valid @RequestBody OrganizationUpdateRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();
        log.info("단체 정보 수정 - 단체 ID: {}, 사용자 ID: {}", id, userId);

        OrganizationResponse organization = organizationService.updateOrganization(id, request, userId);
        return ResponseEntity.ok(organization);
    }
    
    @DeleteMapping("/{id}")
    @RequirePermission(value = {Permission.DELETE_OWN_ORGANIZATION, Permission.DELETE_ANY_ORGANIZATION}, 
                       organizationParam = "id")
    @Operation(
        summary = "Delete organization",
        description = "Delete organization (requires DELETE_ORGANIZATION permission for the specific organization)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Organization deleted successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Organization cannot be deleted")
    public ResponseEntity<Void> deleteOrganization(
            @Parameter(description = "Organization ID")
            @PathVariable Long id) {

        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Deleting organization ID: {} by user ID: {}", id, userId);
        
        organizationService.deleteOrganization(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/members")
    @RequirePermission(value = {Permission.MANAGE_OWN_ORGANIZATION_MEMBERS, Permission.MANAGE_ANY_ORGANIZATION_MEMBERS}, 
                       organizationParam = "id")
    @Operation(
        summary = "Add member to organization",
        description = "Add a new member to the organization (requires MANAGE_ORGANIZATION_MEMBERS permission)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Member added successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data or user already member")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization or user not found")
    public ResponseEntity<OrganizationMemberResponse> addMember(
            @Parameter(description = "Organization ID")
            @PathVariable Long id,
            @Valid @RequestBody OrganizationMemberAddRequest request) {

        Long adminUserId = SecurityUtils.getCurrentUserId();
        log.info("Adding member {} to organization ID: {} by admin ID: {}",
                request.getUserId(), id, adminUserId);
        
        OrganizationMemberResponse member = organizationService.addMemberToOrganization(id, request, adminUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }
    
    @PutMapping("/{id}/members/{userId}/role")
    @RequirePermission(value = {Permission.MANAGE_OWN_ORGANIZATION_MEMBERS, Permission.MANAGE_ANY_ORGANIZATION_MEMBERS},
                       organizationParam = "id")
    @Operation(
        summary = "멤버 역할 변경",
        description = "단체 멤버의 역할을 변경합니다 (멤버 관리 권한 필요)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "멤버 역할이 성공적으로 변경되었습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 마지막 관리자는 제거할 수 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 부족")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "단체 또는 멤버를 찾을 수 없음")
    public ResponseEntity<OrganizationMemberResponse> updateMemberRole(
            @Parameter(description = "단체 ID")
            @PathVariable Long id,
            @Parameter(description = "역할을 변경할 사용자 ID")
            @PathVariable Long userId,
            @Valid @RequestBody OrganizationMemberRoleUpdateRequest request) {

        Long adminUserId = SecurityUtils.getCurrentUserId();
        log.info("멤버 역할 변경 - 단체 ID: {}, 대상 사용자 ID: {}, 새 역할: {}, 요청자 ID: {}",
                id, userId, request.getRole(), adminUserId);

        OrganizationMemberResponse member = organizationService.updateMemberRole(id, userId, request, adminUserId);
        return ResponseEntity.ok(member);
    }
    
    @DeleteMapping("/{id}/members/{userId}")
    @RequirePermission(value = {Permission.MANAGE_OWN_ORGANIZATION_MEMBERS, Permission.MANAGE_ANY_ORGANIZATION_MEMBERS},
                       organizationParam = "id")
    @Operation(
        summary = "멤버 제거",
        description = "단체에서 멤버를 제거합니다 (멤버 관리 권한 필요)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "멤버가 성공적으로 제거되었습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "마지막 관리자는 제거할 수 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 부족")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "단체 또는 멤버를 찾을 수 없음")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "단체 ID")
            @PathVariable Long id,
            @Parameter(description = "제거할 사용자 ID")
            @PathVariable Long userId) {

        Long adminUserId = SecurityUtils.getCurrentUserId();
        log.info("멤버 제거 - 단체 ID: {}, 대상 사용자 ID: {}, 요청자 ID: {}", id, userId, adminUserId);

        organizationService.removeMemberFromOrganization(id, userId, adminUserId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/admins")
    @RequirePermission(value = {Permission.VIEW_OWN_ORGANIZATION_MEMBERS, Permission.VIEW_ANY_ORGANIZATION_MEMBERS},
                       organizationParam = "id")
    @Operation(
        summary = "단체 관리자 조회",
        description = "단체의 관리자 목록을 조회합니다 (단체 멤버만 가능)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관리자 목록이 성공적으로 조회되었습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 부족")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "단체를 찾을 수 없음")
    public ResponseEntity<List<OrganizationMemberResponse>> getOrganizationAdmins(
            @Parameter(description = "단체 ID")
            @PathVariable Long id) {

        Long userId = SecurityUtils.getCurrentUserId();
        log.info("단체 관리자 조회 - 단체 ID: {}, 요청자 ID: {}", id, userId);

        List<OrganizationMemberResponse> admins = organizationService.getOrganizationAdmins(id);
        return ResponseEntity.ok(admins);
    }
    
    // ======================== SYSTEM ADMIN ENDPOINTS ========================
    
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Get organization statistics",
        description = "Get system-wide organization statistics (system admin only)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin permission required")
    public ResponseEntity<Object> getOrganizationStatistics() {
        
        log.info("Getting organization statistics");
        
        return ResponseEntity.ok(Map.of(
            "totalOrganizations", organizationService.getOrganizationCount(),
            "activeOrganizations", organizationService.getActiveOrganizationCount(),
            "organizationsWithActiveCampaigns", organizationService.getOrganizationsWithActiveCampaigns().size()
        ));
    }
    
    @GetMapping("/admin/deletable")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(
        summary = "Get deletable organizations",
        description = "Get organizations that can be safely deleted (system admin only)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deletable organizations retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin permission required")
    public ResponseEntity<List<OrganizationResponse>> getDeletableOrganizations() {
        
        log.info("Getting deletable organizations");
        List<OrganizationResponse> organizations = organizationService.getDeletableOrganizations();
        return ResponseEntity.ok(organizations);
    }
    
}