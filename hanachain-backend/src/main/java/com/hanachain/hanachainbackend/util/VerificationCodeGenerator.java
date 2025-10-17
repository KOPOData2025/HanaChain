package com.hanachain.hanachainbackend.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {
    
    private static final String DIGITS = "0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * 6자리 숫자 인증 코드를 생성합니다.
     */
    public String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        return code.toString();
    }
    
    /**
     * 지정된 길이의 숫자 인증 코드를 생성합니다.
     */
    public String generateCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        return code.toString();
    }
    
    /**
     * 인증 코드의 유효성을 검증합니다.
     */
    public boolean isValidCode(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            return false;
        }
        
        for (char c : code.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        
        return true;
    }
}
