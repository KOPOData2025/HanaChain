package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.entity.VerificationSession;

public interface VerificationService {
    
    /**
     * 이메일 인증 세션을 생성하고 인증 코드를 발송합니다.
     */
    void createAndSendVerificationCode(String email, VerificationSession.VerificationType type);
    
    /**
     * 인증 코드를 검증합니다.
     */
    boolean verifyCode(String email, String code, VerificationSession.VerificationType type);
    
    /**
     * 만료된 인증 세션들을 정리합니다.
     */
    void cleanupExpiredSessions();
    
    /**
     * 특정 이메일과 타입의 모든 인증 세션을 삭제합니다.
     */
    void deleteVerificationSessions(String email, VerificationSession.VerificationType type);
    
    /**
     * 인증 코드 재발송을 처리합니다.
     */
    void resendVerificationCode(String email, VerificationSession.VerificationType type);
}
