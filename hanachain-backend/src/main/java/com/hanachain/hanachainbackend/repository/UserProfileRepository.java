package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    
    /**
     * 사용자 ID로 프로필 조회
     */
    Optional<UserProfile> findByUserId(Long userId);
    
    /**
     * 특정 가시성 설정을 가진 프로필 조회
     */
    @Query("SELECT up FROM UserProfile up WHERE up.visibility = :visibility")
    java.util.List<UserProfile> findByVisibility(@Param("visibility") UserProfile.ProfileVisibility visibility);
    
    /**
     * 프로필이 완성된 사용자 수 조회
     */
    @Query("SELECT COUNT(up) FROM UserProfile up WHERE up.user.profileCompleted = true")
    long countCompletedProfiles();
}