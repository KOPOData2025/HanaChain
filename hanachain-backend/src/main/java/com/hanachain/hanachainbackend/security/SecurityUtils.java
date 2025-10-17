package com.hanachain.hanachainbackend.security;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.OrganizationUser;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import com.hanachain.hanachainbackend.entity.enums.Permission;
import com.hanachain.hanachainbackend.exception.UnauthorizedException;
import com.hanachain.hanachainbackend.repository.OrganizationUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class SecurityUtils {
    
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    
    @Autowired
    private OrganizationUserRepository organizationUserRepository;
    
    /**
     * 현재 인증된 사용자의 이메일을 반환합니다.
     */
    public static Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            return Optional.of(((UserDetails) principal).getUsername());
        }
        
        if (principal instanceof String) {
            return Optional.of((String) principal);
        }
        
        return Optional.empty();
    }
    
    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     */
    public static Long getCurrentUserId() {
        return getCurrentUser()
                .map(User::getId)
                .orElseThrow(UnauthorizedException::notAuthenticated);
    }
    
    /**
     * 현재 인증된 사용자 객체를 반환합니다.
     */
    public static Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        
        // User 클래스가 UserDetails를 구현하므로 UserDetails로 캐스팅한 후 User로 변환
        if (principal instanceof User) {
            return Optional.of((User) principal);
        }
        
        if (principal instanceof UserDetails) {
            // UserDetails에서 User로 변환하는 로직이 필요할 수 있음
            return Optional.of((User) principal);
        }
        
        return Optional.empty();
    }
    
    /**
     * 현재 사용자가 특정 역할을 가지고 있는지 확인합니다.
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
    
    /**
     * 현재 사용자가 관리자인지 확인합니다.
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    /**
     * 현재 사용자가 특정 사용자 ID와 일치하는지 확인합니다.
     */
    public static boolean isCurrentUser(Long userId) {
        return getCurrentUser()
                .map(user -> user.getId().equals(userId))
                .orElse(false);
    }
    
    /**
     * 현재 사용자가 특정 사용자 ID와 일치하거나 관리자인지 확인합니다.
     */
    public static boolean isCurrentUserOrAdmin(Long userId) {
        return isCurrentUser(userId) || isAdmin();
    }
    
    // === 새로운 권한 기반 접근 제어 메서드 ===

    /**
     * 현재 사용자가 특정 시스템 권한을 가지고 있는지 확인합니다
     */
    public boolean hasPermission(Permission permission) {
        return getCurrentUser()
                .map(user -> rolePermissionMapper.hasPermission(user.getRole(), permission))
                .orElse(false);
    }
    
    /**
     * 현재 사용자가 특정 조직 내에서 권한을 가지고 있는지 확인합니다
     */
    public boolean hasPermissionInOrganization(Long organizationId, Permission permission) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return false;
        }

        User user = currentUser.get();

        // 시스템 레벨 권한 먼저 확인
        if (rolePermissionMapper.hasPermission(user.getRole(), permission)) {
            return true;
        }

        // 조직 레벨 권한 확인
        Optional<OrganizationUser> orgUser = organizationUserRepository
                .findByUserAndOrganizationId(user, organizationId);
        
        if (orgUser.isPresent() && orgUser.get().isActive()) {
            return rolePermissionMapper.hasPermission(orgUser.get().getRole(), permission);
        }
        
        return false;
    }
    
    /**
     * 현재 사용자가 조직을 소유하거나 관리자 접근 권한을 가지고 있는지 확인합니다
     */
    public boolean canAccessOrganization(Long organizationId) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return false;
        }

        User user = currentUser.get();

        // 슈퍼 관리자와 캠페인 관리자는 모든 조직에 접근 가능
        if (user.getRole().canManageAnyOrganization()) {
            return true;
        }

        // 사용자가 조직의 멤버인지 확인
        return organizationUserRepository
                .findByUserAndOrganizationId(user, organizationId)
                .map(OrganizationUser::isActive)
                .orElse(false);
    }
    
    /**
     * 현재 사용자가 조직의 멤버를 관리할 수 있는지 확인합니다
     */
    public boolean canManageOrganizationMembers(Long organizationId) {
        return hasPermissionInOrganization(organizationId, Permission.MANAGE_OWN_ORGANIZATION_MEMBERS) ||
               hasPermission(Permission.MANAGE_ANY_ORGANIZATION_MEMBERS);
    }

    /**
     * 현재 사용자가 조직을 수정할 수 있는지 확인합니다
     */
    public boolean canModifyOrganization(Long organizationId) {
        return hasPermissionInOrganization(organizationId, Permission.UPDATE_OWN_ORGANIZATION) ||
               hasPermission(Permission.UPDATE_ANY_ORGANIZATION);
    }

    /**
     * 현재 사용자가 조직을 삭제할 수 있는지 확인합니다
     */
    public boolean canDeleteOrganization(Long organizationId) {
        return hasPermissionInOrganization(organizationId, Permission.DELETE_OWN_ORGANIZATION) ||
               hasPermission(Permission.DELETE_ANY_ORGANIZATION);
    }

    /**
     * 특정 조직에서 현재 사용자의 조직 역할을 가져옵니다
     */
    public Optional<OrganizationRole> getOrganizationRole(Long organizationId) {
        return getCurrentUser()
                .flatMap(user -> organizationUserRepository
                        .findByUserAndOrganizationId(user, organizationId))
                .filter(OrganizationUser::isActive)
                .map(OrganizationUser::getRole);
    }
    
    /**
     * 현재 사용자가 특정 조직의 관리자인지 확인합니다
     */
    public boolean isOrganizationAdmin(Long organizationId) {
        return getOrganizationRole(organizationId)
                .map(OrganizationRole::isAdmin)
                .orElse(false);
    }

    /**
     * 현재 사용자가 슈퍼 관리자인지 확인합니다
     */
    public boolean isSuperAdmin() {
        return getCurrentUser()
                .map(user -> user.getRole().isSuperAdmin())
                .orElse(false);
    }

    /**
     * 현재 사용자가 캠페인 관리자인지 확인합니다
     */
    public boolean isCampaignAdmin() {
        return getCurrentUser()
                .map(user -> user.getRole().isCampaignAdmin())
                .orElse(false);
    }

    /**
     * 현재 사용자가 시스템 레벨 관리자 권한을 가지고 있는지 확인합니다
     */
    public boolean isSystemLevelAdmin() {
        return getCurrentUser()
                .map(user -> user.getRole().isSystemLevelAdmin())
                .orElse(false);
    }

    /**
     * 감사 목적으로 접근 시도를 로그에 기록합니다
     */
    public void logAccessAttempt(String resource, String action, boolean granted) {
        getCurrentUser().ifPresent(user -> {
            log.info("Access attempt - User: {}, Role: {}, Resource: {}, Action: {}, Granted: {}", 
                    user.getEmail(), user.getRole(), resource, action, granted);
        });
    }
    
    /**
     * 사용자가 역할을 할당할 수 있는 충분한 권한을 가지고 있는지 검증합니다
     */
    public boolean canAssignRole(User.Role targetRole) {
        Optional<User> currentUser = getCurrentUser();
        if (currentUser.isEmpty()) {
            return false;
        }

        User.Role currentRole = currentUser.get().getRole();

        // 슈퍼 관리자만 슈퍼 관리자 역할을 할당할 수 있음
        if (targetRole == User.Role.SUPER_ADMIN) {
            return currentRole == User.Role.SUPER_ADMIN;
        }

        // 슈퍼 관리자와 관리자는 다른 모든 역할을 할당할 수 있음
        if (currentRole == User.Role.SUPER_ADMIN || currentRole == User.Role.ADMIN) {
            return true;
        }

        // 캠페인 관리자는 사용자 역할만 할당할 수 있음
        if (currentRole == User.Role.CAMPAIGN_ADMIN) {
            return targetRole == User.Role.USER;
        }

        return false;
    }

    /**
     * 사용자가 조직 역할을 할당할 수 있는지 검증합니다
     */
    public boolean canAssignOrganizationRole(Long organizationId, OrganizationRole targetRole) {
        // 시스템 관리자는 모든 조직 역할을 할당할 수 있음
        if (hasPermission(Permission.ASSIGN_ORGANIZATION_ROLES)) {
            return true;
        }

        // 조직 관리자는 자신의 조직 내에서 역할을 할당할 수 있음
        return isOrganizationAdmin(organizationId);
    }
}
