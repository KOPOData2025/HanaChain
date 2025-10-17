package com.hanachain.hanachainbackend.exception;

/**
 * 블록체인 관련 작업 중 발생하는 예외를 처리하는 클래스
 */
public class BlockchainException extends RuntimeException {
    
    private final String transactionHash;
    private final BlockchainErrorType errorType;
    
    public BlockchainException(String message) {
        super(message);
        this.transactionHash = null;
        this.errorType = BlockchainErrorType.GENERAL;
    }
    
    public BlockchainException(String message, Throwable cause) {
        super(message, cause);
        this.transactionHash = null;
        this.errorType = BlockchainErrorType.GENERAL;
    }
    
    public BlockchainException(String message, String transactionHash, BlockchainErrorType errorType) {
        super(message);
        this.transactionHash = transactionHash;
        this.errorType = errorType;
    }
    
    public BlockchainException(String message, BlockchainErrorType errorType) {
        super(message);
        this.transactionHash = null;
        this.errorType = errorType;
    }
    
    public BlockchainException(String message, BlockchainErrorType errorType, Throwable cause) {
        super(message, cause);
        this.transactionHash = null;
        this.errorType = errorType;
    }
    
    public BlockchainException(String message, String transactionHash, BlockchainErrorType errorType, Throwable cause) {
        super(message, cause);
        this.transactionHash = transactionHash;
        this.errorType = errorType;
    }
    
    public String getTransactionHash() {
        return transactionHash;
    }
    
    public BlockchainErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * 블록체인 오류 타입
     */
    public enum BlockchainErrorType {
        GENERAL("일반 오류"),
        CONNECTION_FAILED("네트워크 연결 실패"),
        TRANSACTION_FAILED("트랜잭션 실행 실패"),
        INSUFFICIENT_GAS("가스 부족"),
        INSUFFICIENT_BALANCE("잔액 부족"),
        CONTRACT_ERROR("컨트랙트 실행 오류"),
        TIMEOUT("타임아웃"),
        INVALID_ADDRESS("잘못된 주소 형식"),
        INVALID_AMOUNT("잘못된 금액"),
        WALLET_ERROR("지갑 오류");

        private final String description;

        BlockchainErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}