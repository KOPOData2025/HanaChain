package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.entity.SignupSession;

public interface SignupService {
    
    /**
     * 약관 동의 단계 - 세션 생성
     */
    String createSignupSession(boolean termsAccepted, boolean privacyAccepted, boolean marketingAccepted);
    
    /**
     * 계정 정보 입력 단계 - 이메일/비밀번호 저장
     */
    void saveAccountInfo(String sessionId, String email, String password);
    
    /**
     * 이메일 인증 완료 단계 - 인증 상태 업데이트
     */
    void markEmailVerified(String sessionId, String email);
    
    /**
     * 닉네임 설정 및 회원가입 완료
     * 지갑은 시스템 마스터 비밀번호로 자동 생성됨
     */
    Long completeSignup(String sessionId, String nickname, String phoneNumber);
    
    /**
     * 세션 조회
     */
    SignupSession getSession(String sessionId);
    
    /**
     * 세션 유효성 검사
     */
    boolean isSessionValid(String sessionId);
    
    /**
     * 만료된 세션 정리
     */
    void cleanupExpiredSessions();
}