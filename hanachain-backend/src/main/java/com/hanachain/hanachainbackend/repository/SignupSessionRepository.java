package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.SignupSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SignupSessionRepository extends JpaRepository<SignupSession, Long> {
    
    Optional<SignupSession> findBySessionId(String sessionId);
    
    Optional<SignupSession> findByEmail(String email);
    
    @Query("SELECT ss FROM SignupSession ss WHERE ss.sessionId = :sessionId AND ss.expiresAt > :now")
    Optional<SignupSession> findBySessionIdAndNotExpired(
            @Param("sessionId") String sessionId,
            @Param("now") LocalDateTime now);
    
    @Query("SELECT ss FROM SignupSession ss WHERE ss.email = :email AND ss.expiresAt > :now")
    Optional<SignupSession> findByEmailAndNotExpired(
            @Param("email") String email,
            @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM SignupSession ss WHERE ss.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM SignupSession ss WHERE ss.email = :email")
    void deleteByEmail(@Param("email") String email);
    
    boolean existsByEmail(String email);
}