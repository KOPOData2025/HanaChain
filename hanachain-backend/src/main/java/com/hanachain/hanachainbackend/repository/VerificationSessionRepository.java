package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.VerificationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationSessionRepository extends JpaRepository<VerificationSession, Long> {
    
    Optional<VerificationSession> findByEmailAndVerificationCodeAndVerifiedFalse(
            String email, String verificationCode);
    
    @Query("SELECT vs FROM VerificationSession vs WHERE vs.email = :email " +
           "AND vs.type = :type AND vs.verified = false AND vs.expiresAt > :now " +
           "ORDER BY vs.createdAt DESC")
    Optional<VerificationSession> findLatestUnverifiedSession(
            @Param("email") String email, 
            @Param("type") VerificationSession.VerificationType type,
            @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM VerificationSession vs WHERE vs.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM VerificationSession vs WHERE vs.email = :email AND vs.type = :type")
    void deleteByEmailAndType(
            @Param("email") String email, 
            @Param("type") VerificationSession.VerificationType type);
    
    @Query("SELECT vs FROM VerificationSession vs WHERE vs.email = :email " +
           "AND vs.type = :type ORDER BY vs.createdAt DESC")
    Optional<VerificationSession> findLatestSessionByEmail(
            @Param("email") String email, 
            @Param("type") VerificationSession.VerificationType type);
}
