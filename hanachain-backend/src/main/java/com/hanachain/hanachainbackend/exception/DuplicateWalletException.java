package com.hanachain.hanachainbackend.exception;

/**
 * 중복된 지갑 등록 시 발생하는 예외
 */
public class DuplicateWalletException extends BusinessException {
    
    private final String walletAddress;
    
    public DuplicateWalletException(String walletAddress) {
        super("이미 등록된 지갑 주소입니다: " + walletAddress);
        this.walletAddress = walletAddress;
    }
    
    public DuplicateWalletException(String walletAddress, String message) {
        super(message);
        this.walletAddress = walletAddress;
    }
    
    public String getWalletAddress() {
        return walletAddress;
    }
    
    public static DuplicateWalletException alreadyRegistered(String walletAddress) {
        return new DuplicateWalletException(walletAddress);
    }
    
    public static DuplicateWalletException userAlreadyOwns(String walletAddress) {
        return new DuplicateWalletException(walletAddress, "이미 소유하고 있는 지갑 주소입니다: " + walletAddress);
    }
}