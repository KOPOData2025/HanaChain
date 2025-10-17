package com.hanachain.hanachainbackend.exception;

/**
 * 프로필 업데이트 실패 예외
 */
public class ProfileUpdateException extends MyPageException {
    
    public ProfileUpdateException(String message) {
        super(message);
    }
    
    public ProfileUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}