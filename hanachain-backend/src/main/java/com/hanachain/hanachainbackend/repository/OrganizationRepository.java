package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.Organization;
import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Organization entity operations
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    /**
     * Find organizations by status with pagination
     */
    Page<Organization> findByStatus(OrganizationStatus status, Pageable pageable);
    
    /**
     * Find organizations by name containing (case insensitive)
     */
    @Query("SELECT o FROM Organization o WHERE LOWER(o.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Organization> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
    
    /**
     * Find organizations by status and name containing
     */
    @Query("SELECT o FROM Organization o WHERE o.status = :status AND LOWER(o.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Organization> findByStatusAndNameContainingIgnoreCase(
        @Param("status") OrganizationStatus status, 
        @Param("name") String name, 
        Pageable pageable
    );
    
    /**
     * Find all active organizations
     */
    @Query("SELECT o FROM Organization o WHERE o.status = 'ACTIVE' AND o.deletedAt IS NULL")
    List<Organization> findAllActive();
    
    /**
     * Find organizations with active campaigns
     * Note: Since campaigns are user-based, this method finds organizations 
     * where at least one member has active campaigns
     */
    @Query("SELECT DISTINCT o FROM Organization o " +
           "JOIN o.members m " +
           "JOIN Campaign c ON c.user = m.user " +
           "WHERE o.status = 'ACTIVE' AND c.status = 'ACTIVE'")
    List<Organization> findOrganizationsWithActiveCampaigns();
    
    /**
     * Count organizations by status
     */
    long countByStatus(OrganizationStatus status);
    
    /**
     * Check if organization name exists (case insensitive)
     */
    @Query("SELECT COUNT(o) > 0 FROM Organization o WHERE LOWER(o.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);
    
    /**
     * Check if organization name exists excluding specific ID (for updates)
     */
    @Query("SELECT COUNT(o) > 0 FROM Organization o WHERE LOWER(o.name) = LOWER(:name) AND o.id != :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);
    
    /**
     * Find organizations that can be safely deleted (no active campaigns by members)
     */
    @Query("SELECT o FROM Organization o WHERE o.id NOT IN (" +
           "SELECT DISTINCT o2.id FROM Organization o2 " +
           "JOIN o2.members m " +
           "JOIN Campaign c ON c.user = m.user " +
           "WHERE c.status = 'ACTIVE')")
    List<Organization> findDeletableOrganizations();
    
    /**
     * Get organization statistics
     */
    @Query("SELECT new map(" +
           "o.status as status, " +
           "COUNT(o) as count, " +
           "COALESCE(AVG(SIZE(o.members)), 0) as avgMembers" +
           ") FROM Organization o GROUP BY o.status")
    List<Object> getOrganizationStatistics();
}