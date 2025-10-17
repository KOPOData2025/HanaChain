package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.entity.SignupSession;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.UserWallet;
import com.hanachain.hanachainbackend.repository.SignupSessionRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.service.SignupService;
import com.hanachain.hanachainbackend.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SignupServiceImpl implements SignupService {
    
    private final SignupSessionRepository signupSessionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    
    @Override
    public String createSignupSession(boolean termsAccepted, boolean privacyAccepted, boolean marketingAccepted) {
        // 새 세션 생성
        String sessionId = UUID.randomUUID().toString();
        
        SignupSession session = SignupSession.builder()
                .sessionId(sessionId)
                .email(null) // 이메일은 Account 단계에서 추가
                .termsAccepted(termsAccepted)
                .privacyAccepted(privacyAccepted)
                .marketingAccepted(marketingAccepted)
                .currentStep(SignupSession.SignupStep.TERMS)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        
        signupSessionRepository.save(session);
        log.info("Created signup session with ID: {}", sessionId);
        
        return sessionId;
    }
    
    @Override
    public void saveAccountInfo(String sessionId, String email, String password) {
        SignupSession session = getValidSession(sessionId);
        
        if (!session.canProceedToStep(SignupSession.SignupStep.ACCOUNT)) {
            throw new RuntimeException("Cannot proceed to account step");
        }
        
        // 기존 세션에서 같은 이메일이 있는지 확인하여 중복 세션 삭제
        signupSessionRepository.deleteByEmail(email);
        
        // 세션에 이메일과 비밀번호 정보 추가
        session.setEmail(email);
        session.setPasswordHash(passwordEncoder.encode(password));
        session.setCurrentStep(SignupSession.SignupStep.ACCOUNT);
        
        signupSessionRepository.save(session);
        log.info("Saved account info for session: {} with email: {}", sessionId, email);
    }
    
    @Override
    public void markEmailVerified(String sessionId, String email) {
        SignupSession session = getValidSession(sessionId);
        
        if (!session.getEmail().equals(email)) {
            throw new RuntimeException("Session email mismatch");
        }
        
        if (!session.canProceedToStep(SignupSession.SignupStep.VERIFICATION)) {
            throw new RuntimeException("Cannot proceed to verification step");
        }
        
        session.setEmailVerified(true);
        session.setCurrentStep(SignupSession.SignupStep.VERIFICATION);
        
        signupSessionRepository.save(session);
        log.info("Marked email verified for session: {}", sessionId);
    }
    
    @Override
    public Long completeSignup(String sessionId, String nickname, String phoneNumber) {
        SignupSession session = getValidSession(sessionId);

        if (!session.canProceedToStep(SignupSession.SignupStep.COMPLETED)) {
            throw new RuntimeException("Cannot complete signup - missing required steps");
        }

        // 이메일 중복 체크
        if (userRepository.existsByEmail(session.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // 닉네임 중복 체크
        if (userRepository.existsByName(nickname)) {
            throw new RuntimeException("Nickname already taken");
        }

        // 사용자 생성
        User user = User.builder()
                .email(session.getEmail())
                .password(session.getPasswordHash()) // 이미 해싱됨
                .name(nickname)
                .phoneNumber(phoneNumber)
                .termsAccepted(session.getTermsAccepted())
                .privacyAccepted(session.getPrivacyAccepted())
                .marketingAccepted(session.getMarketingAccepted())
                .emailVerified(true) // 이메일 인증 완료됨
                .role(User.Role.USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // ✨ E-Wallet 자동 생성 (시스템 마스터 비밀번호 사용)
        try {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("🔐 E-Wallet 자동 생성 시작 - userId: {}", savedUser.getId());

            UserWallet primaryWallet = walletService.createWallet(
                savedUser,
                "dummy-password-not-used",  // WalletServiceImpl에서 마스터 비밀번호 사용
                true  // 주 지갑으로 설정
            );

            log.info("✅ E-Wallet 자동 생성 완료!");
            log.info("   userId: {}", savedUser.getId());
            log.info("   walletAddress: {}", primaryWallet.getWalletAddress());
            log.info("   walletType: {}", primaryWallet.getWalletType());
            log.info("   isPrimary: {}", primaryWallet.getIsPrimary());
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            // 지갑 생성 실패 시에도 회원가입은 완료 (추후 수동 생성 가능)
            log.error("❌ E-Wallet 자동 생성 실패 - userId: {}", savedUser.getId(), e);
            log.warn("⚠️  회원가입은 완료되었으나 지갑 생성에 실패했습니다. 사용자는 마이페이지에서 수동으로 지갑을 생성할 수 있습니다.");
        }

        // 세션 완료 표시 후 삭제
        signupSessionRepository.deleteByEmail(session.getEmail());

        log.info("Signup completed for email: {}", session.getEmail());
        return savedUser.getId();
    }
    
    @Override
    @Transactional(readOnly = true)
    public SignupSession getSession(String sessionId) {
        return getValidSession(sessionId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isSessionValid(String sessionId) {
        try {
            getValidSession(sessionId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    @Scheduled(fixedRate = 600000) // 10분마다 실행
    public void cleanupExpiredSessions() {
        try {
            signupSessionRepository.deleteExpiredSessions(LocalDateTime.now());
            log.debug("Expired signup sessions cleaned up");
        } catch (Exception e) {
            log.error("Failed to cleanup expired signup sessions", e);
        }
    }
    
    private SignupSession getValidSession(String sessionId) {
        SignupSession session = signupSessionRepository.findBySessionIdAndNotExpired(sessionId, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired session"));
        
        if (session.isExpired()) {
            throw new RuntimeException("Session has expired");
        }
        
        return session;
    }
}