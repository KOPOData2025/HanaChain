package com.hanachain.hanachainbackend.controller.admin;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.organization.OrganizationMemberAddRequest;
import com.hanachain.hanachainbackend.dto.organization.OrganizationMemberResponse;
import com.hanachain.hanachainbackend.dto.organization.OrganizationMemberRoleUpdateRequest;
import com.hanachain.hanachainbackend.dto.user.UserProfileResponse;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import com.hanachain.hanachainbackend.service.OrganizationService;
import com.hanachain.hanachainbackend.service.UserService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Admin Controller for organization user management
 * Provides admin-only endpoints for managing organization users
 */
@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Organizations", description = "Admin endpoints for organization user management")
// @PreAuthorize("hasRole('ADMIN')") // Temporarily disabled for debugging
public class AdminOrganizationController {
    
    private final OrganizationService organizationService;
    private final UserService userService;
    
    // ======================== ORGANIZATION USER MANAGEMENT ========================
    
    @GetMapping("/test")
    @Operation(summary = "Test endpoint", description = "Simple test endpoint")
    public ResponseEntity<ApiResponse<String>> testEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Test successful", "Admin controller is working"));
    }
    
    @GetMapping("/{id}/users")
    @Operation(
        summary = "Get organization users",
        description = "Retrieve paginated list of users in an organization (Admin only)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    public ResponseEntity<ApiResponse<Page<OrganizationMemberResponse>>> getOrganizationUsers(
            @Parameter(description = "Organization ID")
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        try {
            Long adminUserId = getUserIdFromAuthentication(authentication);
            log.info("Admin user {} getting users for organization ID: {}", adminUserId, id);

            // 권한 확인: ORG_ADMIN은 자신의 조직만 접근 가능
            // validateOrganizationAccess(adminUserId, id, authentication); // Temporarily disabled for debugging
            
            Page<OrganizationMemberResponse> users = organizationService.getOrganizationMembers(id, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Organization users retrieved successfully",
                users
            ));
        } catch (SecurityException e) {
            log.warn("Access denied for organization {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving organization users for org {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(
                "Failed to retrieve organization users: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{id}/users")
    @Operation(
        summary = "Add user to organization",
        description = "Add a user to an organization with specified role (Admin only)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User added successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or user already member")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization or user not found")
    public ResponseEntity<ApiResponse<OrganizationMemberResponse>> addUserToOrganization(
            @Parameter(description = "Organization ID")
            @PathVariable Long id,
            @Valid @RequestBody OrganizationMemberAddRequest request,
            Authentication authentication) {
        
        Long adminUserId = getUserIdFromAuthentication(authentication);
        log.info("Admin user {} adding user {} to organization ID: {} with role {}",
                adminUserId, request.getUserId(), id, request.getRole());

        // 권한 확인
        validateOrganizationAccess(adminUserId, id, authentication);

        // 비즈니스 로직 검증
        if (organizationService.isUserMemberOfOrganization(request.getUserId(), id)) {
            return ResponseEntity.badRequest().body(ApiResponse.<OrganizationMemberResponse>error(
                "User is already a member of this organization"
            ));
        }
        
        OrganizationMemberResponse member = organizationService.addMemberToOrganization(id, request, adminUserId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
            "User added to organization successfully",
            member
        ));
    }
    
    @PutMapping("/{id}/users/{userId}")
    @Operation(
        summary = "Update user role in organization",
        description = "Update a user's role in an organization (Admin only)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User role updated successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or cannot remove last admin")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization or user not found")
    public ResponseEntity<ApiResponse<OrganizationMemberResponse>> updateUserRole(
            @Parameter(description = "Organization ID")
            @PathVariable Long id,
            @Parameter(description = "User ID")
            @PathVariable Long userId,
            @Valid @RequestBody OrganizationMemberRoleUpdateRequest request,
            Authentication authentication) {
        
        Long adminUserId = getUserIdFromAuthentication(authentication);
        log.info("Admin user {} updating role of user {} in organization ID: {} to {}",
                adminUserId, userId, id, request.getRole());

        // 권한 확인
        validateOrganizationAccess(adminUserId, id, authentication);
        
        OrganizationMemberResponse member = organizationService.updateMemberRole(id, userId, request, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(
            "User role updated successfully",
            member
        ));
    }
    
    @DeleteMapping("/{id}/users/{userId}")
    @Operation(
        summary = "Remove user from organization",
        description = "Remove a user from an organization (Admin only). Cannot remove last admin."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User removed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot remove last admin")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization or user not found")
    public ResponseEntity<ApiResponse<Void>> removeUserFromOrganization(
            @Parameter(description = "Organization ID")
            @PathVariable Long id,
            @Parameter(description = "User ID")
            @PathVariable Long userId,
            Authentication authentication) {
        
        Long adminUserId = getUserIdFromAuthentication(authentication);
        log.info("Admin user {} removing user {} from organization ID: {}", adminUserId, userId, id);

        // 권한 확인
        validateOrganizationAccess(adminUserId, id, authentication);

        // 마지막 관리자를 제거하는지 확인
        List<OrganizationMemberResponse> admins = organizationService.getOrganizationAdmins(id);
        if (admins.size() == 1 && admins.get(0).getUserId().equals(userId)) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>error(
                "Cannot remove the last administrator from the organization"
            ));
        }
        
        organizationService.removeMemberFromOrganization(id, userId, adminUserId);
        
        return ResponseEntity.ok(ApiResponse.success(
            "User removed from organization successfully",
            null
        ));
    }
    
    // ======================== USER SEARCH FOR ORGANIZATION ASSIGNMENT ========================
    
    @GetMapping("/users/search")
    @Operation(
        summary = "Search users for organization assignment",
        description = "Search users by name or email for adding to organizations (Admin only)"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users found successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> searchUsers(
            @Parameter(description = "Search keyword (name or email)")
            @RequestParam String keyword,
            @Parameter(description = "Maximum number of results", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        Long adminUserId = getUserIdFromAuthentication(authentication);
        log.info("Admin user {} searching users with keyword: '{}', limit: {}", adminUserId, keyword, limit);

        // 키워드 검증
        if (keyword == null || keyword.trim().length() < 2) {
            return ResponseEntity.badRequest().body(ApiResponse.<List<UserProfileResponse>>error(
                "Search keyword must be at least 2 characters long"
            ));
        }
        
        List<UserProfileResponse> users = userService.searchUsers(keyword.trim(), limit);
        
        return ResponseEntity.ok(ApiResponse.success(
            "Users found successfully",
            users
        ));
    }
    
    // ======================== HELPER METHODS ========================
    
    /**
     * Extract user ID from authentication
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        return SecurityUtils.getCurrentUserId();
    }
    
    /**
     * Validate organization access based on user role
     */
    private void validateOrganizationAccess(Long userId, Long organizationId, Authentication authentication) {
        // 시스템 관리자인지 확인 (전체 접근 권한)
        if (SecurityUtils.hasRole("ADMIN")) {
            log.info("System admin user {} granted access to organization {}", userId, organizationId);
            return;
        }

        // 관리자가 아닌 사용자의 경우, 레포지토리를 사용하여 조직 관리자인지 확인
        try {
            boolean isOrgAdmin = organizationService.isUserAdminOfOrganization(userId, organizationId);
            
            if (!isOrgAdmin) {
                log.warn("User {} denied access to organization {} - not an admin", userId, organizationId);
                throw new SecurityException("Access denied: User is not an admin of this organization");
            }
            
            log.info("Organization admin user {} granted access to organization {}", userId, organizationId);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating organization access for user {} and org {}: {}", userId, organizationId, e.getMessage());
            throw new SecurityException("Access denied: Unable to validate organization permissions");
        }
    }
}