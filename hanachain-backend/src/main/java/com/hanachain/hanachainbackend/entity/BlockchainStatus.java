package com.hanachain.hanachainbackend.entity;

/**
 * 블록체인 통합 상태를 나타내는 열거형
 * 캠페인 및 기타 블록체인 관련 엔티티의 상태 추적에 사용됩니다.
 */
public enum BlockchainStatus {
    
    /**
     * 블록체인 연동 없음 (초기 상태)
     * - 블록체인 연동이 시작되지 않은 기본 상태
     */
    NONE("블록체인 연동 없음"),
    
    /**
     * 블록체인 등록 대기 중
     * - 데이터베이스에는 저장되었지만 블록체인 트랜잭션이 아직 전송되지 않은 상태
     */
    BLOCKCHAIN_PENDING("블록체인 등록 대기"),
    
    /**
     * 블록체인 처리 중
     * - 트랜잭션이 전송되었지만 아직 확인되지 않은 상태
     * - 트랜잭션 해시가 존재하며 확인 대기 중
     */
    BLOCKCHAIN_PROCESSING("블록체인 처리 중"),
    
    /**
     * 활성 상태
     * - 블록체인 등록이 성공적으로 완료된 상태
     * - 모든 기능이 정상적으로 사용 가능
     */
    ACTIVE("활성"),
    
    /**
     * 블록체인 등록 실패
     * - 트랜잭션 전송 또는 확인 중 실패한 상태
     * - 재시도가 필요하거나 수동 처리가 필요한 상태
     */
    BLOCKCHAIN_FAILED("블록체인 등록 실패");
    
    private final String description;
    
    BlockchainStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 블록체인 관련 작업이 진행 중인지 확인
     * 
     * @return 진행 중이면 true, 완료/실패 상태면 false
     */
    public boolean isProcessing() {
        return this == BLOCKCHAIN_PENDING || this == BLOCKCHAIN_PROCESSING;
    }
    
    /**
     * 블록체인 작업이 완료되었는지 확인
     * 
     * @return 성공적으로 완료되었으면 true
     */
    public boolean isCompleted() {
        return this == ACTIVE;
    }
    
    /**
     * 블록체인 작업이 실패했는지 확인
     * 
     * @return 실패 상태면 true
     */
    public boolean isFailed() {
        return this == BLOCKCHAIN_FAILED;
    }
}