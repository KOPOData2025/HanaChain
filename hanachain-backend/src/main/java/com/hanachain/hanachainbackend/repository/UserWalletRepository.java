package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
    
    /**
     * 사용자의 모든 지갑 조회
     */
    List<UserWallet> findByUserOrderByIsPrimaryDescCreatedAtDesc(User user);
    
    /**
     * 사용자 ID로 모든 지갑 조회
     */
    List<UserWallet> findByUserIdOrderByIsPrimaryDescCreatedAtDesc(Long userId);
    
    /**
     * 사용자의 주 지갑 조회
     */
    Optional<UserWallet> findByUserAndIsPrimaryTrue(User user);
    
    /**
     * 사용자 ID로 주 지갑 조회
     */
    Optional<UserWallet> findByUserIdAndIsPrimaryTrue(Long userId);
    
    /**
     * 지갑 주소로 조회
     */
    Optional<UserWallet> findByWalletAddress(String walletAddress);
    
    /**
     * 지갑 주소 존재 여부 확인
     */
    boolean existsByWalletAddress(String walletAddress);
    
    /**
     * 사용자가 특정 지갑 주소를 소유하고 있는지 확인
     */
    boolean existsByUserAndWalletAddress(User user, String walletAddress);
    
    /**
     * 검증된 지갑만 조회
     */
    List<UserWallet> findByUserAndIsVerifiedTrue(User user);
    
    /**
     * 특정 체인의 지갑만 조회
     */
    List<UserWallet> findByUserAndChainId(User user, Integer chainId);
    
    /**
     * 사용자의 지갑 개수 조회
     */
    long countByUser(User user);
    
    /**
     * 사용자의 검증된 지갑 개수 조회
     */
    long countByUserAndIsVerifiedTrue(User user);
    
    /**
     * 사용자의 모든 지갑을 주 지갑이 아닌 것으로 업데이트
     */
    @Modifying
    @Query("UPDATE UserWallet w SET w.isPrimary = false WHERE w.user = :user")
    void unsetAllPrimaryWallets(@Param("user") User user);
    
    /**
     * 특정 지갑을 주 지갑으로 설정
     */
    @Modifying
    @Query("UPDATE UserWallet w SET w.isPrimary = true WHERE w.id = :walletId AND w.user = :user")
    void setPrimaryWallet(@Param("walletId") Long walletId, @Param("user") User user);
    
    /**
     * 사용자와 지갑 ID로 조회
     */
    Optional<UserWallet> findByIdAndUser(Long id, User user);
    
    /**
     * 주소와 체인 ID로 조회
     */
    Optional<UserWallet> findByWalletAddressAndChainId(String walletAddress, Integer chainId);
}