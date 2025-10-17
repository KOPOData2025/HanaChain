package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    
    /**
     * 사용자 ID로 설정 조회
     */
    Optional<UserSettings> findByUserId(Long userId);
    
    /**
     * 이메일 알림을 허용한 사용자 수 조회
     */
    @Query("SELECT COUNT(us) FROM UserSettings us WHERE us.emailNotifications = true")
    long countByEmailNotificationsEnabled();
    
    /**
     * 특정 알림 타입을 허용한 사용자 수 조회
     */
    @Query("SELECT COUNT(us) FROM UserSettings us WHERE " +
           "(:notificationType = 'donation' AND us.donationUpdateNotifications = true) OR " +
           "(:notificationType = 'campaign' AND us.campaignUpdateNotifications = true) OR " +
           "(:notificationType = 'marketing' AND us.marketingNotifications = true)")
    long countByNotificationType(@Param("notificationType") String notificationType);
    
    /**
     * 공개 프로필을 허용한 사용자 수 조회
     */
    @Query("SELECT COUNT(us) FROM UserSettings us WHERE us.showProfilePublicly = true")
    long countByPublicProfilesEnabled();
}