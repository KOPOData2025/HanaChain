package com.hanachain.hanachainbackend.exception;

/**
 * 지갑 복호화 과정에서 발생하는 예외
 */
public class WalletDecryptionException extends BusinessException {
    
    public WalletDecryptionException(String message) {
        super(message);
    }
    
    public WalletDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static WalletDecryptionException invalidPassword() {
        return new WalletDecryptionException("지갑 비밀번호가 올바르지 않습니다");
    }
    
    public static WalletDecryptionException decryptionFailed() {
        return new WalletDecryptionException("개인키 복호화에 실패했습니다");
    }
    
    public static WalletDecryptionException corruptedData() {
        return new WalletDecryptionException("지갑 데이터가 손상되었습니다");
    }
    
    public static WalletDecryptionException noPrivateKey() {
        return new WalletDecryptionException("개인키 정보가 없습니다. 외부 지갑은 서명 기능을 지원하지 않습니다");
    }
}