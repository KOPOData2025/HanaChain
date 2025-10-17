package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.dto.organization.*;
import com.hanachain.hanachainbackend.entity.Organization;
import com.hanachain.hanachainbackend.entity.OrganizationUser;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import com.hanachain.hanachainbackend.exception.BusinessException;
import com.hanachain.hanachainbackend.exception.NotFoundException;
import com.hanachain.hanachainbackend.exception.ValidationException;
import com.hanachain.hanachainbackend.repository.OrganizationRepository;
import com.hanachain.hanachainbackend.repository.OrganizationUserRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.security.OrganizationAccessService;
import com.hanachain.hanachainbackend.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OrganizationService 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final UserRepository userRepository;
    private final OrganizationAccessService organizationAccessService;
    private final com.hanachain.hanachainbackend.repository.OrganizationWalletRepository organizationWalletRepository;
    private final com.hanachain.hanachainbackend.service.WalletService walletService;

    @Override
    @Transactional
    public OrganizationResponse createOrganization(OrganizationCreateRequest request, Long creatorUserId) {
        log.info("Creating organization with name: {} by user: {}", request.getName(), creatorUserId);

        // 조직명 고유성 검증
        if (existsByName(request.getName())) {
            throw new ValidationException("Organization name already exists: " + request.getName());
        }

        // 생성자 사용자 가져오기
        User creator = userRepository.findById(creatorUserId)
            .orElseThrow(() -> new NotFoundException("User not found: " + creatorUserId));

        // 조직 생성
        Organization organization = Organization.builder()
            .name(request.getName())
            .description(request.getDescription())
            .imageUrl(request.getImageUrl())
            .status(OrganizationStatus.ACTIVE)
            .build();

        organization = organizationRepository.save(organization);

        // 생성자를 관리자로 추가
        organization.addMember(creator, OrganizationRole.ORG_ADMIN);
        organization = organizationRepository.save(organization);

        // 조직을 위한 블록체인 지갑 자동 생성
        try {
            com.hanachain.hanachainbackend.entity.OrganizationWallet wallet =
                    walletService.generateWalletForOrganization(organization.getId());
            wallet.setOrganization(organization);
            organizationWalletRepository.save(wallet);
            log.info("Created blockchain wallet for organization: {}, address: {}",
                    organization.getId(), wallet.getWalletAddress());
        } catch (Exception e) {
            log.error("Failed to create wallet for organization: {}", organization.getId(), e);
            // 지갑 생성 실패해도 조직 생성은 유지 (지갑은 나중에 생성 가능)
            // 운영 환경에서는 트랜잭션 롤백을 고려할 수 있음
        }

        log.info("Created organization with ID: {} and added creator as admin", organization.getId());
        return convertToDTO(organization);
    }
    
    @Override
    public OrganizationResponse getOrganizationById(Long id) {
        Organization organization = organizationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Organization not found: " + id));
        return convertToDTO(organization);
    }
    
    @Override
    public OrganizationResponse getOrganizationByIdWithDetails(Long id) {
        Organization organization = organizationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Organization not found: " + id));
        return convertToDTOWithDetails(organization);
    }
    
    @Override
    public Page<OrganizationResponse> getAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    public Page<OrganizationResponse> getOrganizationsByStatus(OrganizationStatus status, Pageable pageable) {
        return organizationRepository.findByStatus(status, pageable)
            .map(this::convertToDTO);
    }
    
    @Override
    public Page<OrganizationResponse> searchOrganizations(String name, OrganizationStatus status, Pageable pageable) {
        if (name != null && status != null) {
            return organizationRepository.findByStatusAndNameContainingIgnoreCase(status, name, pageable)
                .map(this::convertToDTO);
        } else if (name != null) {
            return organizationRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(this::convertToDTO);
        } else if (status != null) {
            return organizationRepository.findByStatus(status, pageable)
                .map(this::convertToDTO);
        } else {
            return organizationRepository.findAll(pageable)
                .map(this::convertToDTO);
        }
    }
    
    @Override
    @Transactional
    public OrganizationResponse updateOrganization(Long id, OrganizationUpdateRequest request, Long updaterUserId) {
        log.info("Updating organization ID: {} by user: {}", id, updaterUserId);

        // 조직 존재 및 사용자의 수정 권한 검증
        Organization organization = organizationAccessService.verifyOrganizationExists(id);
        organizationAccessService.verifyOrganizationModifyAccess(id);
        organizationAccessService.verifyOrganizationModifiable(organization);

        // 이름이 변경되는 경우 고유성 검증
        if (!organization.getName().equals(request.getName()) &&
            existsByNameExcludingId(request.getName(), id)) {
            throw new ValidationException("Organization name already exists: " + request.getName());
        }

        // 조직 업데이트
        organization.setName(request.getName());
        organization.setDescription(request.getDescription());
        organization.setImageUrl(request.getImageUrl());
        organization.setStatus(request.getStatus());
        
        organization = organizationRepository.save(organization);
        
        log.info("Updated organization ID: {}", id);
        return convertToDTO(organization);
    }
    
    @Override
    @Transactional
    public void deleteOrganization(Long id, Long deleterUserId) {
        log.info("Deleting organization ID: {} by user: {}", id, deleterUserId);

        // 조직 존재 및 사용자의 삭제 권한 검증
        Organization organization = organizationAccessService.verifyOrganizationExists(id);
        organizationAccessService.verifyOrganizationDeleteAccess(id);

        // 조직을 안전하게 삭제할 수 있는지 확인
        if (!canBeDeleted(id)) {
            throw new BusinessException("Organization cannot be deleted as it has active campaigns");
        }

        // 소프트 삭제 (@SQLDelete 어노테이션으로 처리됨)
        organizationRepository.delete(organization);
        
        log.info("Deleted organization ID: {}", id);
    }
    
    @Override
    public boolean existsByName(String name) {
        return organizationRepository.existsByNameIgnoreCase(name);
    }
    
    @Override
    public boolean existsByNameExcludingId(String name, Long excludeId) {
        return organizationRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
    }
    
    @Override
    public boolean canBeDeleted(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found: " + organizationId));
        return organization.canBeDeleted();
    }
    
    @Override
    public Page<OrganizationMemberResponse> getOrganizationMembers(Long organizationId, Pageable pageable) {
        return organizationUserRepository.findByOrganizationId(organizationId, pageable)
            .map(this::convertToMemberDTO);
    }
    
    @Override
    public List<OrganizationMemberResponse> getOrganizationAdmins(Long organizationId) {
        return organizationUserRepository.findAdminsByOrganizationId(organizationId)
            .stream()
            .map(this::convertToMemberDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public OrganizationMemberResponse addMemberToOrganization(Long organizationId, OrganizationMemberAddRequest request, Long adminUserId) {
        log.info("Adding user {} to organization {} with role {} by admin {}", 
                request.getUserId(), organizationId, request.getRole(), adminUserId);
        
        // 접근 권한 검증
        Organization organization = organizationAccessService.verifyOrganizationExists(organizationId);
        organizationAccessService.verifyMemberManagementAccess(organizationId);
        organizationAccessService.verifyRoleAssignmentPermission(organizationId, request.getRole());

        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new NotFoundException("User not found: " + request.getUserId()));

        // 사용자가 이미 멤버인지 확인
        if (organizationUserRepository.existsByOrganizationIdAndUserId(organizationId, request.getUserId())) {
            throw new BusinessException("User is already a member of this organization");
        }

        // 멤버십 생성
        OrganizationUser orgUser = OrganizationUser.builder()
            .organization(organization)
            .user(user)
            .role(request.getRole())
            .build();
        
        orgUser = organizationUserRepository.save(orgUser);
        
        log.info("Added user {} to organization {} with role {}", request.getUserId(), organizationId, request.getRole());
        return convertToMemberDTO(orgUser);
    }
    
    @Override
    @Transactional
    public OrganizationMemberResponse updateMemberRole(Long organizationId, Long userId, 
                                                 OrganizationMemberRoleUpdateRequest request, Long adminUserId) {
        log.info("Updating role of user {} in organization {} to {} by admin {}",
                userId, organizationId, request.getRole(), adminUserId);

        // 접근 권한 검증
        organizationAccessService.verifyOrganizationExists(organizationId);
        organizationAccessService.verifyMemberManagementAccess(organizationId);
        organizationAccessService.verifyRoleAssignmentPermission(organizationId, request.getRole());

        // 변경하기 전에 관리자 제약 조건 검증
        organizationAccessService.verifyAdminConstraints(organizationId, userId, request.getRole());

        // 조직 멤버십 가져오기
        OrganizationUser orgUser = organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new NotFoundException("User is not a member of this organization"));

        // 역할 업데이트
        orgUser.setRole(request.getRole());
        orgUser = organizationUserRepository.save(orgUser);
        
        log.info("Updated role of user {} in organization {} to {}", userId, organizationId, request.getRole());
        return convertToMemberDTO(orgUser);
    }
    
    @Override
    @Transactional
    public void removeMemberFromOrganization(Long organizationId, Long userId, Long adminUserId) {
        log.info("Removing user {} from organization {} by admin {}", userId, organizationId, adminUserId);

        // 접근 권한 검증
        organizationAccessService.verifyOrganizationExists(organizationId);
        organizationAccessService.verifyMemberManagementAccess(organizationId);

        // 제거하기 전에 관리자 제약 조건 검증 (제거를 나타내기 위해 null 역할 전달)
        organizationAccessService.verifyAdminConstraints(organizationId, userId, null);

        // 조직 멤버십 가져오기
        OrganizationUser orgUser = organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new NotFoundException("User is not a member of this organization"));

        // 멤버십 제거 (소프트 삭제)
        organizationUserRepository.delete(orgUser);
        
        log.info("Removed user {} from organization {}", userId, organizationId);
    }
    
    @Override
    public boolean isUserMemberOfOrganization(Long userId, Long organizationId) {
        return organizationUserRepository.existsByOrganizationIdAndUserId(organizationId, userId);
    }
    
    @Override
    public boolean isUserAdminOfOrganization(Long userId, Long organizationId) {
        return organizationUserRepository.isUserAdminOfOrganization(organizationId, userId);
    }
    
    @Override
    public List<OrganizationResponse> getOrganizationsForUser(Long userId) {
        return organizationUserRepository.findByUserId(userId)
            .stream()
            .map(orgUser -> convertToDTO(orgUser.getOrganization()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationResponse> getOrganizationsWhereUserIsAdmin(Long userId) {
        return organizationUserRepository.findOrganizationsWhereUserIsAdmin(userId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public long getOrganizationCount() {
        return organizationRepository.count();
    }
    
    @Override
    public long getActiveOrganizationCount() {
        return organizationRepository.countByStatus(OrganizationStatus.ACTIVE);
    }
    
    @Override
    public long getOrganizationMemberCount(Long organizationId) {
        return organizationUserRepository.countActiveMembers(organizationId);
    }
    
    @Override
    public long getOrganizationAdminCount(Long organizationId) {
        return organizationUserRepository.countByOrganizationIdAndRole(organizationId, OrganizationRole.ORG_ADMIN);
    }
    
    @Override
    public List<OrganizationResponse> getOrganizationsWithActiveCampaigns() {
        return organizationRepository.findOrganizationsWithActiveCampaigns()
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<OrganizationResponse> getDeletableOrganizations() {
        return organizationRepository.findDeletableOrganizations()
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    // DTO 변환을 위한 헬퍼 메서드

    private OrganizationResponse convertToDTO(Organization organization) {
        OrganizationResponse dto = OrganizationResponse.builder()
            .id(organization.getId())
            .name(organization.getName())
            .description(organization.getDescription())
            .imageUrl(organization.getImageUrl())
            .status(organization.getStatus())
            .createdAt(organization.getCreatedAt())
            .updatedAt(organization.getUpdatedAt())
            .memberCount((long) organization.getMembers().size())
            .adminCount(organization.getAdminCount())
            .activeCampaignCount(organization.getActiveCampaignCount())
            .walletAddress(organization.getWalletAddress())  // 블록체인 지갑 주소 추가
            .build();

        return dto;
    }
    
    private OrganizationResponse convertToDTOWithDetails(Organization organization) {
        OrganizationResponse dto = convertToDTO(organization);

        // 멤버 상세 정보 추가
        List<OrganizationMemberResponse> members = organization.getMembers()
            .stream()
            .map(this::convertToMemberDTO)
            .collect(Collectors.toList());
        dto.setMembers(members);

        // 캠페인 요약 추가 (캠페인은 별도로 로드되어야 함)
        // 현재는 빈 목록 설정 - Campaign 레포지토리를 통해 구현되어야 함
        dto.setCampaigns(new ArrayList<>());

        return dto;
    }
    
    private OrganizationMemberResponse convertToMemberDTO(OrganizationUser orgUser) {
        User user = orgUser.getUser();
        return OrganizationMemberResponse.builder()
            .id(orgUser.getId())
            .userId(user.getId())
            .username(user.getName())
            .email(user.getEmail())
            .fullName(user.getName())
            .role(orgUser.getRole())
            .joinedAt(orgUser.getCreatedAt())
            .active(orgUser.isActive())
            .build();
    }
    
    private OrganizationCampaignSummary convertToCampaignSummaryDTO(Object campaign) {
        // TODO: Campaign 엔티티를 사용할 수 있을 때 구현
        // Campaign 엔티티가 구현되면 업데이트되어야 하는 플레이스홀더
        return OrganizationCampaignSummary.builder()
            .id(0L)
            .title("Placeholder")
            .status("PENDING")
            .build();
    }
}