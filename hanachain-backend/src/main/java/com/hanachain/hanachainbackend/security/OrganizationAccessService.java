package com.hanachain.hanachainbackend.security;

import com.hanachain.hanachainbackend.entity.Organization;
import com.hanachain.hanachainbackend.entity.OrganizationUser;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import com.hanachain.hanachainbackend.entity.enums.Permission;
import com.hanachain.hanachainbackend.repository.OrganizationRepository;
import com.hanachain.hanachainbackend.repository.OrganizationUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 조직 접근 제어 및 소유권 검증을 위한 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationAccessService {
    
    private final SecurityUtils securityUtils;
    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    
    /**
     * Verify that current user has access to the organization
     * Throws AccessDeniedException if access is denied
     */
    public void verifyOrganizationAccess(Long organizationId) {
        if (!securityUtils.canAccessOrganization(organizationId)) {
            log.warn("Access denied to organization {} for user", organizationId);
            throw new AccessDeniedException("해당 단체에 대한 접근 권한이 없습니다.");
        }
    }
    
    /**
     * Verify that current user has admin access to the organization
     * Throws AccessDeniedException if access is denied
     */
    public void verifyOrganizationAdminAccess(Long organizationId) {
        if (!securityUtils.isOrganizationAdmin(organizationId) && 
            !securityUtils.isSuperAdmin() && 
            !securityUtils.isCampaignAdmin()) {
            log.warn("Admin access denied to organization {} for user", organizationId);
            throw new AccessDeniedException("해당 단체에 대한 관리자 권한이 없습니다.");
        }
    }
    
    /**
     * Verify that current user can manage members of the organization
     */
    public void verifyMemberManagementAccess(Long organizationId) {
        if (!securityUtils.canManageOrganizationMembers(organizationId)) {
            log.warn("Member management access denied to organization {} for user", organizationId);
            throw new AccessDeniedException("해당 단체의 멤버 관리 권한이 없습니다.");
        }
    }
    
    /**
     * Verify that current user can modify the organization
     */
    public void verifyOrganizationModifyAccess(Long organizationId) {
        if (!securityUtils.canModifyOrganization(organizationId)) {
            log.warn("Modify access denied to organization {} for user", organizationId);
            throw new AccessDeniedException("해당 단체를 수정할 권한이 없습니다.");
        }
    }
    
    /**
     * Verify that current user can delete the organization
     */
    public void verifyOrganizationDeleteAccess(Long organizationId) {
        if (!securityUtils.canDeleteOrganization(organizationId)) {
            log.warn("Delete access denied to organization {} for user", organizationId);
            throw new AccessDeniedException("해당 단체를 삭제할 권한이 없습니다.");
        }
    }
    
    /**
     * Verify that organization exists and return it
     */
    public Organization verifyOrganizationExists(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> {
                    log.warn("Organization not found: {}", organizationId);
                    return new AccessDeniedException("존재하지 않는 단체입니다.");
                });
    }
    
    /**
     * Get current user's role in the organization
     */
    public Optional<OrganizationRole> getCurrentUserRoleInOrganization(Long organizationId) {
        return securityUtils.getOrganizationRole(organizationId);
    }
    
    /**
     * Check if current user is the only admin of the organization
     * This is used to prevent removing the last admin
     */
    public boolean isCurrentUserOnlyAdmin(Long organizationId) {
        Optional<User> currentUser = securityUtils.getCurrentUser();
        if (currentUser.isEmpty()) {
            return false;
        }
        
        // 조직의 총 관리자 수 계산
        long adminCount = organizationUserRepository.countByOrganizationIdAndRole(
                organizationId, OrganizationRole.ORG_ADMIN);

        // 현재 사용자가 관리자인지 확인
        boolean isCurrentUserAdmin = securityUtils.isOrganizationAdmin(organizationId);
        
        return isCurrentUserAdmin && adminCount <= 1;
    }
    
    /**
     * 관리자 제약 조건 검증 - 조직에서 마지막 관리자를 제거하지 않도록 보장합니다
     */
    public void verifyAdminConstraints(Long organizationId, Long targetUserId, OrganizationRole newRole) {
        // 대상 사용자의 현재 역할 가져오기
        Optional<OrganizationUser> targetOrgUser = organizationUserRepository
                .findByOrganizationIdAndUserId(organizationId, targetUserId);

        if (targetOrgUser.isEmpty()) {
            log.warn("대상 사용자 {}를 조직 {}에서 찾을 수 없습니다", targetUserId, organizationId);
            throw new AccessDeniedException("해당 사용자가 단체에 속해있지 않습니다.");
        }

        OrganizationRole currentRole = targetOrgUser.get().getRole();

        // 관리자 역할을 제거하거나 관리자 사용자를 제거하는 경우
        if ((currentRole == OrganizationRole.ORG_ADMIN && newRole != OrganizationRole.ORG_ADMIN) ||
            (currentRole == OrganizationRole.ORG_ADMIN && newRole == null)) {

            long adminCount = organizationUserRepository.countByOrganizationIdAndRole(
                    organizationId, OrganizationRole.ORG_ADMIN);

            if (adminCount <= 1) {
                log.warn("조직 {}에서 마지막 관리자를 제거할 수 없습니다", organizationId);
                throw new AccessDeniedException("단체에는 최소한 한 명의 관리자가 있어야 합니다.");
            }
        }
    }
    
    /**
     * 현재 사용자가 지정된 역할을 할당할 수 있는지 검증합니다
     */
    public void verifyRoleAssignmentPermission(Long organizationId, OrganizationRole targetRole) {
        if (!securityUtils.canAssignOrganizationRole(organizationId, targetRole)) {
            log.warn("조직 {}에서 역할 {}의 할당이 거부되었습니다",
                    targetRole, organizationId);
            throw new AccessDeniedException("해당 역할을 할당할 권한이 없습니다.");
        }
    }

    /**
     * 현재 사용자가 관리자인 모든 조직을 가져옵니다
     */
    public List<Organization> getOrganizationsWhereCurrentUserIsAdmin() {
        Optional<User> currentUser = securityUtils.getCurrentUser();
        if (currentUser.isEmpty()) {
            return List.of();
        }

        return organizationUserRepository.findOrganizationsWhereUserIsAdmin(currentUser.get().getId());
    }

    /**
     * 사용자가 조직에 대한 특정 권한을 가지고 있는지 검증합니다
     */
    public void verifyPermissionForOrganization(Long organizationId, Permission permission) {
        if (!securityUtils.hasPermissionInOrganization(organizationId, permission)) {
            log.warn("조직 {}에 대한 권한 {}이 거부되었습니다", permission, organizationId);
            throw new AccessDeniedException("해당 단체에 대한 권한이 없습니다: " + permission.getDisplayName());
        }
    }

    /**
     * 조직이 수정 가능한 상태인지 확인합니다
     */
    public void verifyOrganizationModifiable(Organization organization) {
        if (!organization.isActive()) {
            log.warn("비활성 조직 수정 시도: {}", organization.getId());
            throw new AccessDeniedException("비활성화된 단체는 수정할 수 없습니다.");
        }
    }
}