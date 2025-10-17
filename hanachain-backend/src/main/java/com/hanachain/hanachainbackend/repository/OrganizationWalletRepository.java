package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.OrganizationWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * OrganizationWallet 엔티티를 위한 리포지토리
 * 조직의 블록체인 지갑 정보 관리
 */
@Repository
public interface OrganizationWalletRepository extends JpaRepository<OrganizationWallet, Long> {

    /**
     * 조직 ID로 지갑 조회
     * @param organizationId 조직 ID
     * @return 조직의 지갑 정보
     */
    @Query("SELECT w FROM OrganizationWallet w WHERE w.organization.id = :organizationId")
    Optional<OrganizationWallet> findByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * 지갑 주소로 지갑 조회
     * @param walletAddress 지갑 주소
     * @return 지갑 정보
     */
    Optional<OrganizationWallet> findByWalletAddress(String walletAddress);

    /**
     * 조직 ID로 지갑 존재 여부 확인
     * @param organizationId 조직 ID
     * @return 지갑 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM OrganizationWallet w WHERE w.organization.id = :organizationId")
    boolean existsByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * 지갑 주소 존재 여부 확인
     * @param walletAddress 지갑 주소
     * @return 존재 여부
     */
    boolean existsByWalletAddress(String walletAddress);

    /**
     * 조직 ID로 활성 지갑 조회
     * @param organizationId 조직 ID
     * @return 활성 상태의 지갑 정보
     */
    @Query("SELECT w FROM OrganizationWallet w WHERE w.organization.id = :organizationId AND w.isActive = true")
    Optional<OrganizationWallet> findActiveByOrganizationId(@Param("organizationId") Long organizationId);
}
