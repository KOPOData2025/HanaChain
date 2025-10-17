package com.hanachain.hanachainbackend.exception;

/**
 * 지갑 생성 과정에서 발생하는 예외
 */
public class WalletCreationException extends BusinessException {
    
    public WalletCreationException(String message) {
        super(message);
    }
    
    public WalletCreationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static WalletCreationException keyGenerationFailed() {
        return new WalletCreationException("암호화 키 생성에 실패했습니다");
    }
    
    public static WalletCreationException encryptionFailed() {
        return new WalletCreationException("개인키 암호화에 실패했습니다");
    }
    
    public static WalletCreationException addressGenerationFailed() {
        return new WalletCreationException("지갑 주소 생성에 실패했습니다");
    }
    
    public static WalletCreationException databaseSaveFailed() {
        return new WalletCreationException("지갑 정보 저장에 실패했습니다");
    }
}