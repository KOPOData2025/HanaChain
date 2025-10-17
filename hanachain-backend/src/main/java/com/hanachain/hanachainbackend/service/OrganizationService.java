package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.organization.*;
import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 단체 관리를 위한 서비스 인터페이스
 */
public interface OrganizationService {
    
    // 단체 CRUD 작업
    OrganizationResponse createOrganization(OrganizationCreateRequest request, Long creatorUserId);
    OrganizationResponse getOrganizationById(Long id);
    OrganizationResponse getOrganizationByIdWithDetails(Long id);
    Page<OrganizationResponse> getAllOrganizations(Pageable pageable);
    Page<OrganizationResponse> getOrganizationsByStatus(OrganizationStatus status, Pageable pageable);
    Page<OrganizationResponse> searchOrganizations(String name, OrganizationStatus status, Pageable pageable);
    OrganizationResponse updateOrganization(Long id, OrganizationUpdateRequest request, Long updaterUserId);
    void deleteOrganization(Long id, Long deleterUserId);
    
    // 단체 검증
    boolean existsByName(String name);
    boolean existsByNameExcludingId(String name, Long excludeId);
    boolean canBeDeleted(Long organizationId);
    
    // 멤버 관리
    Page<OrganizationMemberResponse> getOrganizationMembers(Long organizationId, Pageable pageable);
    List<OrganizationMemberResponse> getOrganizationAdmins(Long organizationId);
    OrganizationMemberResponse addMemberToOrganization(Long organizationId, OrganizationMemberAddRequest request, Long adminUserId);
    OrganizationMemberResponse updateMemberRole(Long organizationId, Long userId, OrganizationMemberRoleUpdateRequest request, Long adminUserId);
    void removeMemberFromOrganization(Long organizationId, Long userId, Long adminUserId);
    
    // 멤버십 조회
    boolean isUserMemberOfOrganization(Long userId, Long organizationId);
    boolean isUserAdminOfOrganization(Long userId, Long organizationId);
    List<OrganizationResponse> getOrganizationsForUser(Long userId);
    List<OrganizationResponse> getOrganizationsWhereUserIsAdmin(Long userId);
    
    // 통계 및 분석
    long getOrganizationCount();
    long getActiveOrganizationCount();
    long getOrganizationMemberCount(Long organizationId);
    long getOrganizationAdminCount(Long organizationId);
    
    // 비즈니스 작업
    List<OrganizationResponse> getOrganizationsWithActiveCampaigns();
    List<OrganizationResponse> getDeletableOrganizations();
}