package com.hanachain.hanachainbackend.security;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import com.hanachain.hanachainbackend.entity.enums.Permission;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 역할 권한 매퍼
 * 사용자 역할과 조직 역할을 해당 권한에 매핑합니다
 */
@Component
public class RolePermissionMapper {

    /**
     * 시스템 레벨 사용자 역할에 대한 모든 권한을 가져옵니다
     */
    public Set<Permission> getPermissionsForRole(User.Role role) {
        return switch (role) {
            case SUPER_ADMIN -> getSuperAdminPermissions();
            case ADMIN -> getAdminPermissions();
            case CAMPAIGN_ADMIN -> getCampaignAdminPermissions();
            case USER -> getUserPermissions();
        };
    }

    /**
     * 조직 역할에 대한 모든 권한을 가져옵니다
     */
    public Set<Permission> getPermissionsForOrganizationRole(OrganizationRole role) {
        return switch (role) {
            case OWNER -> getOwnerPermissions();
            case ADMIN -> getOrgAdminPermissions();
            case MEMBER -> getOrgMemberPermissions();
            case VOLUNTEER -> getVolunteerPermissions();
            case ORG_ADMIN -> getOrgAdminPermissions();
            case ORG_MEMBER -> getOrgMemberPermissions();
        };
    }

    /**
     * 시스템 역할과 조직 역할을 모두 고려한 사용자의 결합 권한을 가져옵니다
     */
    public Set<Permission> getCombinedPermissions(User.Role systemRole, OrganizationRole orgRole) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        permissions.addAll(getPermissionsForRole(systemRole));
        if (orgRole != null) {
            permissions.addAll(getPermissionsForOrganizationRole(orgRole));
        }
        return permissions;
    }

    /**
     * 시스템 역할이 특정 권한을 가지고 있는지 확인합니다
     */
    public boolean hasPermission(User.Role role, Permission permission) {
        return getPermissionsForRole(role).contains(permission);
    }

    /**
     * 조직 역할이 특정 권한을 가지고 있는지 확인합니다
     */
    public boolean hasPermission(OrganizationRole role, Permission permission) {
        return getPermissionsForOrganizationRole(role).contains(permission);
    }

    /**
     * 결합된 역할이 특정 권한을 가지고 있는지 확인합니다
     */
    public boolean hasPermission(User.Role systemRole, OrganizationRole orgRole, Permission permission) {
        return getCombinedPermissions(systemRole, orgRole).contains(permission);
    }

    // 권한 세트 정의
    
    private Set<Permission> getSuperAdminPermissions() {
        return EnumSet.allOf(Permission.class); // 슈퍼 관리자는 모든 권한을 가짐
    }

    private Set<Permission> getAdminPermissions() {
        return EnumSet.of(
            // 조직 권한
            Permission.VIEW_ANY_ORGANIZATION,
            Permission.VIEW_ANY_ORGANIZATION_MEMBERS,

            // 캠페인 권한
            Permission.VIEW_ANY_CAMPAIGN,
            Permission.UPDATE_ANY_CAMPAIGN,
            Permission.DELETE_ANY_CAMPAIGN,

            // 사용자 관리
            Permission.MANAGE_USERS,
            Permission.VIEW_USER_DETAILS,

            // 시스템 권한
            Permission.SYSTEM_ADMIN,
            Permission.AUDIT_LOG_ACCESS,

            // 기본 권한
            Permission.VIEW_PUBLIC_CAMPAIGN,
            Permission.CREATE_CAMPAIGN,
            Permission.UPDATE_OWN_CAMPAIGN,
            Permission.DELETE_OWN_CAMPAIGN
        );
    }

    private Set<Permission> getCampaignAdminPermissions() {
        return EnumSet.of(
            // 조직 관리
            Permission.CREATE_ORGANIZATION,
            Permission.UPDATE_ANY_ORGANIZATION,
            Permission.DELETE_ANY_ORGANIZATION,
            Permission.VIEW_ANY_ORGANIZATION,

            // 멤버 관리
            Permission.MANAGE_ANY_ORGANIZATION_MEMBERS,
            Permission.VIEW_ANY_ORGANIZATION_MEMBERS,

            // 캠페인 권한
            Permission.CREATE_CAMPAIGN,
            Permission.UPDATE_ANY_CAMPAIGN,
            Permission.DELETE_ANY_CAMPAIGN,
            Permission.VIEW_ANY_CAMPAIGN,
            Permission.VIEW_PUBLIC_CAMPAIGN,

            // 사용자 관리
            Permission.VIEW_USER_DETAILS,

            // 역할 관리
            Permission.ASSIGN_ORGANIZATION_ROLES,

            // 기본 권한
            Permission.UPDATE_OWN_CAMPAIGN,
            Permission.DELETE_OWN_CAMPAIGN
        );
    }

    private Set<Permission> getUserPermissions() {
        return EnumSet.of(
            // 기본 캠페인 권한
            Permission.CREATE_CAMPAIGN,
            Permission.UPDATE_OWN_CAMPAIGN,
            Permission.DELETE_OWN_CAMPAIGN,
            Permission.VIEW_PUBLIC_CAMPAIGN
        );
    }
    
    private Set<Permission> getOrgAdminPermissions() {
        return EnumSet.of(
            // 자신의 조직 관리
            Permission.UPDATE_OWN_ORGANIZATION,
            Permission.DELETE_OWN_ORGANIZATION,
            Permission.VIEW_OWN_ORGANIZATION,

            // 자신의 조직 멤버 관리
            Permission.MANAGE_OWN_ORGANIZATION_MEMBERS,
            Permission.VIEW_OWN_ORGANIZATION_MEMBERS,

            // 캠페인 권한
            Permission.CREATE_CAMPAIGN,
            Permission.UPDATE_OWN_CAMPAIGN,
            Permission.DELETE_OWN_CAMPAIGN,
            Permission.VIEW_PUBLIC_CAMPAIGN,

            // 조직 내 역할 관리
            Permission.ASSIGN_ORGANIZATION_ROLES
        );
    }

    private Set<Permission> getOrgMemberPermissions() {
        return EnumSet.of(
            // 기본 조직 접근
            Permission.VIEW_OWN_ORGANIZATION,
            Permission.VIEW_OWN_ORGANIZATION_MEMBERS,

            // 캠페인 권한
            Permission.CREATE_CAMPAIGN,
            Permission.UPDATE_OWN_CAMPAIGN,
            Permission.DELETE_OWN_CAMPAIGN,
            Permission.VIEW_PUBLIC_CAMPAIGN
        );
    }

    private Set<Permission> getOwnerPermissions() {
        return EnumSet.of(
            // 전체 조직 관리
            Permission.UPDATE_OWN_ORGANIZATION,
            Permission.DELETE_OWN_ORGANIZATION,
            Permission.VIEW_OWN_ORGANIZATION,

            // 전체 멤버 관리
            Permission.MANAGE_OWN_ORGANIZATION_MEMBERS,
            Permission.VIEW_OWN_ORGANIZATION_MEMBERS,

            // 캠페인 권한
            Permission.CREATE_CAMPAIGN,
            Permission.UPDATE_OWN_CAMPAIGN,
            Permission.DELETE_OWN_CAMPAIGN,
            Permission.VIEW_PUBLIC_CAMPAIGN,

            // 조직 내 역할 관리
            Permission.ASSIGN_ORGANIZATION_ROLES
        );
    }

    private Set<Permission> getVolunteerPermissions() {
        return EnumSet.of(
            // 기본 조직 접근
            Permission.VIEW_OWN_ORGANIZATION,
            Permission.VIEW_OWN_ORGANIZATION_MEMBERS,

            // 제한된 캠페인 권한
            Permission.VIEW_PUBLIC_CAMPAIGN
        );
    }
}