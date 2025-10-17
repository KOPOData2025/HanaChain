package com.hanachain.hanachainbackend.exception;

/**
 * 사용자 프로필을 찾을 수 없을 때 발생하는 예외
 */
public class ProfileNotFoundException extends MyPageException {
    
    public ProfileNotFoundException(Long userId) {
        super("사용자 ID " + userId + "에 해당하는 프로필을 찾을 수 없습니다.");
    }
    
    public ProfileNotFoundException(String message) {
        super(message);
    }
}