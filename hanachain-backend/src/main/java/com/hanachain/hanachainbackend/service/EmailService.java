package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.entity.VerificationSession;

public interface EmailService {
    
    /**
     * 인증 코드가 포함된 이메일을 발송합니다.
     */
    void sendVerificationEmail(String to, String verificationCode, VerificationSession.VerificationType type);
    
    /**
     * 비밀번호 재설정 이메일을 발송합니다.
     */
    void sendPasswordResetEmail(String to, String verificationCode);
    
    /**
     * 이메일 변경 확인 이메일을 발송합니다.
     */
    void sendEmailChangeConfirmation(String to, String verificationCode);
    
    /**
     * 일반 텍스트 이메일을 발송합니다.
     */
    void sendSimpleEmail(String to, String subject, String content);
    
    /**
     * HTML 이메일을 발송합니다.
     */
    void sendHtmlEmail(String to, String subject, String htmlContent);
}
