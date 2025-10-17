package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.entity.VerificationSession;
import com.hanachain.hanachainbackend.repository.VerificationSessionRepository;
import com.hanachain.hanachainbackend.service.EmailService;
import com.hanachain.hanachainbackend.service.VerificationService;
import com.hanachain.hanachainbackend.util.VerificationCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VerificationServiceImpl implements VerificationService {
    
    private final VerificationSessionRepository verificationSessionRepository;
    private final EmailService emailService;
    private final VerificationCodeGenerator codeGenerator;
    
    @Value("${app.mail.verification.expiration}")
    private long verificationExpirationMs;
    
    @Override
    public void createAndSendVerificationCode(String email, VerificationSession.VerificationType type) {
        log.info("Creating and sending verification code - Email: {}, Type: {}", email, type);
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        
        if (type == null) {
            throw new IllegalArgumentException("Verification type cannot be null");
        }
        
        // 최근 발송 기록 확인하여 중복 발송 방지 (30초 제한)
        Optional<VerificationSession> recentSession = verificationSessionRepository
                .findLatestSessionByEmail(email, type);
        
        if (recentSession.isPresent()) {
            VerificationSession session = recentSession.get();
            // 30초 이내 재발송 방지
            if (session.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(30))) {
                log.warn("Too frequent verification code request for email: {} - Last sent: {}", 
                        email, session.getCreatedAt());
                throw new RuntimeException("Please wait 30 seconds before requesting a new verification code");
            }
        }
        
        // 기존 미완료 세션들 삭제
        log.debug("Deleting existing verification sessions for email: {} and type: {}", email, type);
        deleteVerificationSessions(email, type);
        
        // 새 인증 코드 생성
        String verificationCode = codeGenerator.generateCode();
        log.debug("Generated verification code for email: {} (code length: {})", email, verificationCode.length());
        
        // 인증 세션 생성
        VerificationSession session = VerificationSession.builder()
                .email(email)
                .verificationCode(verificationCode)
                .type(type)
                .expiresAt(LocalDateTime.now().plusSeconds(verificationExpirationMs / 1000))
                .attemptCount(0)
                .verified(false)
                .build();
        
        log.debug("Saving verification session - Email: {}, ExpiresAt: {}", email, session.getExpiresAt());
        verificationSessionRepository.save(session);
        
        // 이메일 발송
        try {
            log.debug("Attempting to send verification email via EmailService...");
            emailService.sendVerificationEmail(email, verificationCode, type);
            log.info("Verification code sent successfully to email: {} for type: {}", email, type);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {} for type: {} - Error: {}", 
                    email, type, e.getMessage(), e);
            // 실패한 세션 삭제
            try {
                verificationSessionRepository.delete(session);
                log.debug("Deleted failed verification session for email: {}", email);
            } catch (Exception deleteEx) {
                log.warn("Failed to delete verification session after email send failure", deleteEx);
            }
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean verifyCode(String email, String code, VerificationSession.VerificationType type) {
        Optional<VerificationSession> sessionOpt = verificationSessionRepository
                .findLatestUnverifiedSession(email, type, LocalDateTime.now());
        
        if (sessionOpt.isEmpty()) {
            log.warn("No valid verification session found for email: {} and type: {}", email, type);
            return false;
        }
        
        VerificationSession session = sessionOpt.get();
        
        // 최대 시도 횟수 확인
        if (session.isMaxAttemptsReached()) {
            log.warn("Maximum verification attempts reached for email: {}", email);
            return false;
        }
        
        // 시도 횟수 증가
        session.incrementAttempt();
        verificationSessionRepository.save(session);
        
        // 코드 검증
        if (!session.getVerificationCode().equals(code)) {
            log.warn("Invalid verification code for email: {}", email);
            return false;
        }
        
        // 만료 시간 확인
        if (session.isExpired()) {
            log.warn("Verification code expired for email: {}", email);
            return false;
        }
        
        // 인증 성공
        session.setVerified(true);
        verificationSessionRepository.save(session);
        
        log.info("Verification successful for email: {} and type: {}", email, type);
        return true;
    }
    
    @Override
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void cleanupExpiredSessions() {
        try {
            verificationSessionRepository.deleteExpiredSessions(LocalDateTime.now());
            log.debug("Expired verification sessions cleaned up");
        } catch (Exception e) {
            log.error("Failed to cleanup expired verification sessions", e);
        }
    }
    
    @Override
    public void deleteVerificationSessions(String email, VerificationSession.VerificationType type) {
        verificationSessionRepository.deleteByEmailAndType(email, type);
        log.debug("Deleted verification sessions for email: {} and type: {}", email, type);
    }
    
    @Override
    public void resendVerificationCode(String email, VerificationSession.VerificationType type) {
        // 기존 세션 확인
        Optional<VerificationSession> existingSession = verificationSessionRepository
                .findLatestUnverifiedSession(email, type, LocalDateTime.now());
        
        if (existingSession.isPresent()) {
            VerificationSession session = existingSession.get();
            
            // 너무 빈번한 재발송 방지 (30초 이내 재발송 불가)
            if (session.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(30))) {
                throw new RuntimeException("Please wait 30 seconds before requesting a new verification code");
            }
        }
        
        // 새 인증 코드 생성 및 발송
        createAndSendVerificationCode(email, type);
        log.info("Verification code resent to email: {} for type: {}", email, type);
    }
}
