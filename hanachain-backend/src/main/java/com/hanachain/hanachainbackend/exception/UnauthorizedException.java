package com.hanachain.hanachainbackend.exception;

/**
 * 권한 없는 접근 시 발생하는 예외
 */
public class UnauthorizedException extends BusinessException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static UnauthorizedException walletAccess() {
        return new UnauthorizedException("해당 지갑에 접근할 권한이 없습니다");
    }
    
    public static UnauthorizedException notWalletOwner() {
        return new UnauthorizedException("지갑 소유자만 접근할 수 있습니다");
    }
    
    public static UnauthorizedException insufficientPermission() {
        return new UnauthorizedException("작업을 수행할 권한이 없습니다");
    }

    public static UnauthorizedException notAuthenticated() {
        return new UnauthorizedException("인증된 사용자를 찾을 수 없습니다");
    }
}