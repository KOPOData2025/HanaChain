package com.hanachain.hanachainbackend.exception;

/**
 * 지갑 검증 과정에서 발생하는 예외
 */
public class WalletValidationException extends ValidationException {
    
    public WalletValidationException(String message) {
        super(message);
    }
    
    public WalletValidationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static WalletValidationException invalidAddress() {
        return new WalletValidationException("유효하지 않은 지갑 주소입니다");
    }
    
    public static WalletValidationException addressFormatInvalid() {
        return new WalletValidationException("지갑 주소 형식이 올바르지 않습니다. 0x로 시작하는 40자리 16진수여야 합니다");
    }
    
    public static WalletValidationException checksumFailed() {
        return new WalletValidationException("지갑 주소 체크섬 검증에 실패했습니다");
    }
    
    public static WalletValidationException emptyAddress() {
        return new WalletValidationException("지갑 주소는 필수입니다");
    }
    
    public static WalletValidationException invalidChainId() {
        return new WalletValidationException("지원하지 않는 체인 ID입니다");
    }
}