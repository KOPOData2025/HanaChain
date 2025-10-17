package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.Organization;
import com.hanachain.hanachainbackend.entity.OrganizationUser;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OrganizationUser entity operations
 */
@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, Long> {
    
    /**
     * Find organization membership by organization and user
     */
    Optional<OrganizationUser> findByOrganizationAndUser(Organization organization, User user);
    
    /**
     * Find organization membership by organization ID and user ID
     */
    @Query("SELECT ou FROM OrganizationUser ou WHERE ou.organization.id = :orgId AND ou.user.id = :userId")
    Optional<OrganizationUser> findByOrganizationIdAndUserId(@Param("orgId") Long organizationId, @Param("userId") Long userId);
    
    /**
     * Find organization membership by user ID and organization ID
     */
    @Query("SELECT ou FROM OrganizationUser ou WHERE ou.user.id = :userId AND ou.organization.id = :orgId")
    Optional<OrganizationUser> findByUserIdAndOrganizationId(@Param("userId") Long userId, @Param("orgId") Long organizationId);
    
    /**
     * Find organization membership by user and organization ID
     */
    @Query("SELECT ou FROM OrganizationUser ou WHERE ou.user = :user AND ou.organization.id = :orgId")
    Optional<OrganizationUser> findByUserAndOrganizationId(@Param("user") User user, @Param("orgId") Long organizationId);
    
    /**
     * Find all memberships for a user
     */
    List<OrganizationUser> findByUser(User user);
    
    /**
     * Find all memberships for a user by user ID
     */
    @Query("SELECT ou FROM OrganizationUser ou WHERE ou.user.id = :userId")
    List<OrganizationUser> findByUserId(@Param("userId") Long userId);
    
    /**
     * Find all members of an organization with pagination
     */
    Page<OrganizationUser> findByOrganization(Organization organization, Pageable pageable);
    
    /**
     * Find all members of an organization by organization ID
     */
    @Query("SELECT ou FROM OrganizationUser ou WHERE ou.organization.id = :orgId")
    Page<OrganizationUser> findByOrganizationId(@Param("orgId") Long organizationId, Pageable pageable);
    
    /**
     * Find members by organization and role
     */
    List<OrganizationUser> findByOrganizationAndRole(Organization organization, OrganizationRole role);
    
    /**
     * Find admin members of an organization
     */
    @Query("SELECT ou FROM OrganizationUser ou WHERE ou.organization.id = :orgId AND ou.role = 'ORG_ADMIN'")
    List<OrganizationUser> findAdminsByOrganizationId(@Param("orgId") Long organizationId);
    
    /**
     * Find active members of an organization
     */
    @Query("SELECT ou FROM OrganizationUser ou " +
           "WHERE ou.organization.id = :orgId AND ou.user.enabled = true AND ou.deletedAt IS NULL")
    List<OrganizationUser> findActiveMembers(@Param("orgId") Long organizationId);
    
    /**
     * Check if user is member of organization
     */
    @Query("SELECT COUNT(ou) > 0 FROM OrganizationUser ou " +
           "WHERE ou.organization.id = :orgId AND ou.user.id = :userId AND ou.deletedAt IS NULL")
    boolean existsByOrganizationIdAndUserId(@Param("orgId") Long organizationId, @Param("userId") Long userId);
    
    /**
     * Check if user is admin of organization
     */
    @Query("SELECT COUNT(ou) > 0 FROM OrganizationUser ou " +
           "WHERE ou.organization.id = :orgId AND ou.user.id = :userId " +
           "AND ou.role = 'ORG_ADMIN' AND ou.deletedAt IS NULL")
    boolean isUserAdminOfOrganization(@Param("orgId") Long organizationId, @Param("userId") Long userId);
    
    /**
     * Count members by organization and role
     */
    @Query("SELECT COUNT(ou) FROM OrganizationUser ou " +
           "WHERE ou.organization.id = :orgId AND ou.role = :role AND ou.deletedAt IS NULL")
    long countByOrganizationIdAndRole(@Param("orgId") Long organizationId, @Param("role") OrganizationRole role);
    
    /**
     * Count active members of organization
     */
    @Query("SELECT COUNT(ou) FROM OrganizationUser ou " +
           "WHERE ou.organization.id = :orgId AND ou.user.enabled = true AND ou.deletedAt IS NULL")
    long countActiveMembers(@Param("orgId") Long organizationId);
    
    /**
     * Find organizations where user is admin
     */
    @Query("SELECT ou.organization FROM OrganizationUser ou " +
           "WHERE ou.user.id = :userId AND ou.role = 'ORG_ADMIN' AND ou.deletedAt IS NULL")
    List<Organization> findOrganizationsWhereUserIsAdmin(@Param("userId") Long userId);
    
    /**
     * Find users by organization with specific roles
     */
    @Query("SELECT ou.user FROM OrganizationUser ou " +
           "WHERE ou.organization.id = :orgId AND ou.role IN :roles AND ou.deletedAt IS NULL")
    List<User> findUsersByOrganizationIdAndRoles(@Param("orgId") Long organizationId, @Param("roles") List<OrganizationRole> roles);
    
    /**
     * Remove user from organization (soft delete)
     */
    @Query("UPDATE OrganizationUser ou SET ou.deletedAt = CURRENT_TIMESTAMP " +
           "WHERE ou.organization.id = :orgId AND ou.user.id = :userId")
    void removeUserFromOrganization(@Param("orgId") Long organizationId, @Param("userId") Long userId);
}