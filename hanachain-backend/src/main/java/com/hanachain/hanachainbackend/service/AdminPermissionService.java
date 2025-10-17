package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.admin.PermissionDelegationRequest;
import com.hanachain.hanachainbackend.dto.admin.PermissionDelegationResponse;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;

import java.util.List;

/**
 * Service for admin permission delegation and role management
 */
public interface AdminPermissionService {
    
    /**
     * Delegate system role to a user
     * @param request Permission delegation request
     * @param adminUser Admin user performing the delegation
     * @return Response with delegation details
     */
    PermissionDelegationResponse delegateSystemRole(PermissionDelegationRequest request, User adminUser);
    
    /**
     * Delegate organization role to a user
     * @param request Permission delegation request
     * @param adminUser Admin user performing the delegation
     * @return Response with delegation details
     */
    PermissionDelegationResponse delegateOrganizationRole(PermissionDelegationRequest request, User adminUser);
    
    /**
     * Revoke system role from a user
     * @param targetUserId User ID to revoke role from
     * @param adminUser Admin user performing the revocation
     */
    void revokeSystemRole(Long targetUserId, User adminUser);
    
    /**
     * Revoke organization role from a user
     * @param targetUserId User ID to revoke role from
     * @param organizationId Organization ID
     * @param adminUser Admin user performing the revocation
     */
    void revokeOrganizationRole(Long targetUserId, Long organizationId, User adminUser);
    
    /**
     * Get all delegated permissions for a user
     * @param userId User ID
     * @return List of delegated permissions
     */
    List<PermissionDelegationResponse> getUserDelegatedPermissions(Long userId);
    
    /**
     * Get all active permission delegations
     * @return List of active delegations
     */
    List<PermissionDelegationResponse> getActiveDelegations();
    
    /**
     * Check if admin can delegate a specific system role
     * @param adminUser Admin user
     * @param targetRole Role to delegate
     * @return true if delegation is allowed
     */
    boolean canDelegateSystemRole(User adminUser, User.Role targetRole);
    
    /**
     * Check if admin can delegate a specific organization role
     * @param adminUser Admin user
     * @param targetRole Organization role to delegate
     * @param organizationId Organization ID
     * @return true if delegation is allowed
     */
    boolean canDelegateOrganizationRole(User adminUser, OrganizationRole targetRole, Long organizationId);
    
    /**
     * Process expired delegations
     */
    void processExpiredDelegations();
}