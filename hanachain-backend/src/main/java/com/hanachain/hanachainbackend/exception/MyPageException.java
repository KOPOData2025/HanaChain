package com.hanachain.hanachainbackend.exception;

/**
 * 마이페이지 관련 기본 예외 클래스
 */
public class MyPageException extends RuntimeException {
    
    public MyPageException(String message) {
        super(message);
    }
    
    public MyPageException(String message, Throwable cause) {
        super(message, cause);
    }
}