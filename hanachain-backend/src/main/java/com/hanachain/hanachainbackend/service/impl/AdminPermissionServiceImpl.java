package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.dto.admin.PermissionDelegationRequest;
import com.hanachain.hanachainbackend.dto.admin.PermissionDelegationResponse;
import com.hanachain.hanachainbackend.entity.Organization;
import com.hanachain.hanachainbackend.entity.OrganizationUser;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import com.hanachain.hanachainbackend.repository.OrganizationRepository;
import com.hanachain.hanachainbackend.repository.OrganizationUserRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.security.exceptions.InsufficientPermissionException;
import com.hanachain.hanachainbackend.security.exceptions.OrganizationAccessDeniedException;
import com.hanachain.hanachainbackend.security.exceptions.RoleElevationDeniedException;
import com.hanachain.hanachainbackend.service.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AdminPermissionService 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminPermissionServiceImpl implements AdminPermissionService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;

    @Override
    public PermissionDelegationResponse delegateSystemRole(PermissionDelegationRequest request, User adminUser) {
        log.info("Delegating system role {} to user {} by admin {}",
                request.getSystemRole(), request.getTargetUserId(), adminUser.getId());

        // 관리자 권한 검증
        if (!canDelegateSystemRole(adminUser, request.getSystemRole())) {
            throw new RoleElevationDeniedException(adminUser.getRole(), request.getSystemRole());
        }

        // 대상 사용자 가져오기
        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다"));

        // 역할 상승 규칙 검증
        validateSystemRoleElevation(adminUser, targetUser, request.getSystemRole());

        // 사용자 역할 업데이트
        User.Role previousRole = targetUser.getRole();
        targetUser.setRole(request.getSystemRole());
        userRepository.save(targetUser);

        log.info("System role updated: User {} role changed from {} to {} by admin {}",
                targetUser.getId(), previousRole, request.getSystemRole(), adminUser.getId());

        // 응답 생성
        return PermissionDelegationResponse.builder()
                .targetUserId(targetUser.getId())
                .targetUserName(targetUser.getName())
                .targetUserEmail(targetUser.getEmail())
                .systemRole(request.getSystemRole())
                .reason(request.getReason())
                .expiresAt(request.getExpiresAt())
                .temporary(request.getTemporary())
                .delegatedBy(adminUser.getName())
                .delegatedAt(LocalDateTime.now())
                .build();
    }
    
    @Override
    public PermissionDelegationResponse delegateOrganizationRole(PermissionDelegationRequest request, User adminUser) {
        log.info("Delegating organization role {} to user {} in organization {} by admin {}",
                request.getOrganizationRole(), request.getTargetUserId(), request.getOrganizationId(), adminUser.getId());

        // 관리자 권한 검증
        if (!canDelegateOrganizationRole(adminUser, request.getOrganizationRole(), request.getOrganizationId())) {
            throw new OrganizationAccessDeniedException(request.getOrganizationId(), "역할 할당");
        }

        // 대상 사용자 및 조직 가져오기
        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다"));

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("단체를 찾을 수 없습니다"));

        // 조직 사용자 관계 찾기 또는 생성
        Optional<OrganizationUser> existingRelation = organizationUserRepository
                .findByUserIdAndOrganizationId(targetUser.getId(), organization.getId());
        
        OrganizationUser organizationUser;
        if (existingRelation.isPresent()) {
            organizationUser = existingRelation.get();
            OrganizationRole previousRole = organizationUser.getRole();
            organizationUser.setRole(request.getOrganizationRole());
            log.info("Organization role updated: User {} role changed from {} to {} in organization {} by admin {}", 
                    targetUser.getId(), previousRole, request.getOrganizationRole(), organization.getId(), adminUser.getId());
        } else {
            organizationUser = OrganizationUser.builder()
                    .user(targetUser)
                    .organization(organization)
                    .role(request.getOrganizationRole())
                    .build();
            log.info("New organization membership created: User {} assigned role {} in organization {} by admin {}", 
                    targetUser.getId(), request.getOrganizationRole(), organization.getId(), adminUser.getId());
        }
        
        organizationUserRepository.save(organizationUser);

        // 응답 생성
        return PermissionDelegationResponse.builder()
                .targetUserId(targetUser.getId())
                .targetUserName(targetUser.getName())
                .targetUserEmail(targetUser.getEmail())
                .organizationRole(request.getOrganizationRole())
                .organizationId(organization.getId())
                .organizationName(organization.getName())
                .reason(request.getReason())
                .expiresAt(request.getExpiresAt())
                .temporary(request.getTemporary())
                .delegatedBy(adminUser.getName())
                .delegatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public void revokeSystemRole(Long targetUserId, User adminUser) {
        log.info("Revoking system role from user {} by admin {}", targetUserId, adminUser.getId());

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다"));

        // 관리자가 이 역할을 취소할 수 있는지 검증
        if (!canDelegateSystemRole(adminUser, targetUser.getRole())) {
            throw new RoleElevationDeniedException(adminUser.getRole(), targetUser.getRole());
        }

        User.Role previousRole = targetUser.getRole();
        targetUser.setRole(User.Role.USER); // 기본 사용자 역할로 재설정
        userRepository.save(targetUser);

        log.info("System role revoked: User {} role changed from {} to USER by admin {}",
                targetUser.getId(), previousRole, adminUser.getId());
    }

    @Override
    public void revokeOrganizationRole(Long targetUserId, Long organizationId, User adminUser) {
        log.info("Revoking organization role from user {} in organization {} by admin {}",
                targetUserId, organizationId, adminUser.getId());

        // 관리자 권한 검증
        if (!canDelegateOrganizationRole(adminUser, OrganizationRole.OWNER, organizationId)) {
            throw new OrganizationAccessDeniedException(organizationId, "역할 취소");
        }
        
        OrganizationUser organizationUser = organizationUserRepository
                .findByUserIdAndOrganizationId(targetUserId, organizationId)
                .orElseThrow(() -> new IllegalArgumentException("단체 멤버십을 찾을 수 없습니다"));
        
        OrganizationRole previousRole = organizationUser.getRole();
        organizationUserRepository.delete(organizationUser);
        
        log.info("Organization membership revoked: User {} with role {} removed from organization {} by admin {}", 
                targetUserId, previousRole, organizationId, adminUser.getId());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PermissionDelegationResponse> getUserDelegatedPermissions(Long userId) {
        // 이 구현에서는 현재 사용자 역할과 조직 멤버십을 반환합니다
        // 더 복잡한 시스템에서는 위임 기록을 별도 테이블에 저장할 수 있습니다

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        List<PermissionDelegationResponse> permissions = new ArrayList<>();

        // 기본 USER가 아닌 경우 시스템 역할 추가
        if (user.getRole() != User.Role.USER) {
            permissions.add(PermissionDelegationResponse.builder()
                    .targetUserId(user.getId())
                    .targetUserName(user.getName())
                    .targetUserEmail(user.getEmail())
                    .systemRole(user.getRole())
                    .build());
        }

        // 조직 역할 추가
        List<OrganizationUser> organizationMemberships = organizationUserRepository.findByUserId(userId);
        for (OrganizationUser membership : organizationMemberships) {
            permissions.add(PermissionDelegationResponse.builder()
                    .targetUserId(user.getId())
                    .targetUserName(user.getName())
                    .targetUserEmail(user.getEmail())
                    .organizationRole(membership.getRole())
                    .organizationId(membership.getOrganization().getId())
                    .organizationName(membership.getOrganization().getName())
                    .build());
        }

        return permissions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDelegationResponse> getActiveDelegations() {
        // 상승된 역할 및 조직 멤버십을 가진 모든 사용자 반환
        List<PermissionDelegationResponse> delegations = new ArrayList<>();

        // 상승된 시스템 역할을 가진 사용자들
        List<User> elevatedUsers = userRepository.findByRoleNot(User.Role.USER);
        for (User user : elevatedUsers) {
            delegations.add(PermissionDelegationResponse.builder()
                    .targetUserId(user.getId())
                    .targetUserName(user.getName())
                    .targetUserEmail(user.getEmail())
                    .systemRole(user.getRole())
                    .build());
        }

        // 조직 역할을 가진 사용자들
        List<OrganizationUser> allMemberships = organizationUserRepository.findAll();
        for (OrganizationUser membership : allMemberships) {
            delegations.add(PermissionDelegationResponse.builder()
                    .targetUserId(membership.getUser().getId())
                    .targetUserName(membership.getUser().getName())
                    .targetUserEmail(membership.getUser().getEmail())
                    .organizationRole(membership.getRole())
                    .organizationId(membership.getOrganization().getId())
                    .organizationName(membership.getOrganization().getName())
                    .build());
        }

        return delegations;
    }
    
    @Override
    public boolean canDelegateSystemRole(User adminUser, User.Role targetRole) {
        // 슈퍼 관리자는 모든 역할을 위임할 수 있음
        if (adminUser.getRole().isSuperAdmin()) {
            return true;
        }

        // 관리자는 USER와 CAMPAIGN_ADMIN 역할을 위임할 수 있지만, 다른 ADMIN 역할은 불가
        if (adminUser.getRole() == User.Role.ADMIN) {
            return targetRole == User.Role.USER || targetRole == User.Role.CAMPAIGN_ADMIN;
        }

        // 캠페인 관리자는 USER 역할만 위임할 수 있음
        if (adminUser.getRole().isCampaignAdmin()) {
            return targetRole == User.Role.USER;
        }

        return false;
    }

    @Override
    public boolean canDelegateOrganizationRole(User adminUser, OrganizationRole targetRole, Long organizationId) {
        // 슈퍼 관리자와 캠페인 관리자는 모든 조직을 관리할 수 있음
        if (adminUser.getRole().canManageAnyOrganization()) {
            return true;
        }

        // 특정 조직에서 사용자가 충분한 역할을 가지고 있는지 확인
        Optional<OrganizationUser> adminMembership = organizationUserRepository
                .findByUserIdAndOrganizationId(adminUser.getId(), organizationId);

        if (adminMembership.isPresent()) {
            OrganizationRole adminRole = adminMembership.get().getRole();

            // 소유자는 OWNER를 제외한 모든 역할을 위임할 수 있음
            if (adminRole == OrganizationRole.OWNER) {
                return targetRole != OrganizationRole.OWNER;
            }

            // 관리자는 MEMBER와 VOLUNTEER 역할을 위임할 수 있음
            if (adminRole == OrganizationRole.ADMIN) {
                return targetRole == OrganizationRole.MEMBER || targetRole == OrganizationRole.VOLUNTEER;
            }
        }

        return false;
    }

    @Override
    public void processExpiredDelegations() {
        // 더 복잡한 구현에서는 만료 날짜를 가진 위임 기록 테이블이 있어야 하며
        // 여기에서 만료된 위임을 처리합니다
        log.info("Processing expired delegations - not implemented yet");
    }

    private void validateSystemRoleElevation(User adminUser, User targetUser, User.Role newRole) {
        // 자신의 권한을 더 높은 역할로 상승시키는 것을 방지
        if (adminUser.getId().equals(targetUser.getId()) &&
            newRole.ordinal() > adminUser.getRole().ordinal()) {
            throw new RoleElevationDeniedException("자신의 권한을 상승시킬 수 없습니다",
                    adminUser.getRole(), newRole);
        }

        // 관리자의 레벨 이상으로 상승시키는 것을 방지 (SUPER_ADMIN 제외)
        if (!adminUser.getRole().isSuperAdmin() &&
            newRole.ordinal() >= adminUser.getRole().ordinal()) {
            throw new RoleElevationDeniedException(adminUser.getRole(), newRole);
        }
    }
}