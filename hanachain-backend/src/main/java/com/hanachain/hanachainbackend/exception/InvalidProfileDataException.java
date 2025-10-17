package com.hanachain.hanachainbackend.exception;

/**
 * 유효하지 않은 프로필 데이터로 인해 발생하는 예외
 */
public class InvalidProfileDataException extends MyPageException {
    
    public InvalidProfileDataException(String message) {
        super(message);
    }
    
    public InvalidProfileDataException(String field, String value) {
        super("유효하지 않은 " + field + " 값입니다: " + value);
    }
}