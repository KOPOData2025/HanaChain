package com.hanachain.hanachainbackend.service.blockchain;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

/**
 * 블록체인과의 상호작용을 담당하는 서비스 인터페이스
 * HanaChain 플랫폼의 스마트 컨트랙트와의 통합을 제공합니다.
 */
public interface BlockchainService {
    
    // =========================== 캠페인 관리 ===========================
    
    /**
     * 블록체인에 새로운 캠페인을 생성합니다 (비동기)
     * 
     * @param campaignId 데이터베이스의 캠페인 ID
     * @param beneficiaryAddress 수혜자 지갑 주소
     * @param goalAmount 목표 금액 (USDC, 6 decimals)
     * @param duration 캠페인 기간 (초)
     * @param title 캠페인 제목
     * @param description 캠페인 설명
     * @return 트랜잭션 해시를 포함한 CompletableFuture
     */
    CompletableFuture<String> createCampaignAsync(
        Long campaignId,
        String beneficiaryAddress, 
        BigInteger goalAmount,
        BigInteger duration,
        String title, 
        String description
    );
    
    /**
     * 블록체인에서 캠페인 정보를 조회합니다
     *
     * @param contractAddress 블록체인 캠페인 컨트랙트 주소
     * @return 캠페인 정보 객체
     */
    CampaignInfo getCampaignFromBlockchain(String contractAddress);
    
    /**
     * 블록체인에서 캠페인을 완료 처리합니다
     *
     * @param contractAddress 블록체인 캠페인 컨트랙트 주소
     * @return 트랜잭션 해시를 포함한 CompletableFuture
     */
    CompletableFuture<String> finalizeCampaignAsync(String contractAddress);
    
    // =========================== 기부 관리 ===========================
    
    /**
     * USDC 기부를 처리합니다 (approve + donate)
     * 
     * @param campaignId 블록체인 상의 캠페인 ID
     * @param donorAddress 기부자 지갑 주소
     * @param amount 기부 금액 (USDC, 6 decimals)
     * @return 트랜잭션 해시를 포함한 CompletableFuture
     */
    CompletableFuture<String> processDonationAsync(
        BigInteger campaignId,
        String donorAddress,
        BigInteger amount
    );
    
    /**
     * 특정 주소의 USDC 잔액을 조회합니다
     * 
     * @param address 조회할 지갑 주소
     * @return USDC 잔액 (6 decimals)
     */
    BigInteger getUSDCBalance(String address);
    
    /**
     * 특정 캠페인에 대한 기부 내역을 조회합니다
     *
     * @param campaignId 캠페인 ID
     * @param donorAddress 기부자 주소 (null이면 전체 조회)
     * @return 기부 내역 리스트
     */
    CompletableFuture<java.util.List<DonationInfo>> getDonationHistoryAsync(
        BigInteger campaignId,
        String donorAddress
    );

    /**
     * 캠페인의 블록체인 트랜잭션 목록을 조회합니다
     *
     * @param campaignId 데이터베이스 캠페인 ID
     * @param limit 조회할 트랜잭션 수
     * @return 트랜잭션 목록
     */
    com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransactionListResponse getCampaignTransactions(
        Long campaignId,
        int limit
    );

    // =========================== 유틸리티 ===========================
    
    /**
     * 트랜잭션 상태를 조회합니다
     * 
     * @param transactionHash 트랜잭션 해시
     * @return 트랜잭션 상태 정보
     */
    TransactionStatus getTransactionStatus(String transactionHash);
    
    /**
     * 현재 가스 가격을 추정합니다
     * 
     * @return 추정된 가스 가격 (wei)
     */
    BigInteger estimateGasPrice();
    
    /**
     * 트랜잭션 확인을 대기합니다
     * 
     * @param transactionHash 대기할 트랜잭션 해시
     * @param timeoutSeconds 타임아웃 시간 (초)
     * @return 트랜잭션 수신 정보
     */
    CompletableFuture<org.web3j.protocol.core.methods.response.TransactionReceipt> waitForTransactionAsync(
        String transactionHash, 
        int timeoutSeconds
    );
    
    // =========================== 내부 클래스 ===========================
    
    /**
     * 캠페인 정보 데이터 클래스
     */
    class CampaignInfo {
        private BigInteger id;
        private String beneficiary;
        private BigInteger goalAmount;
        private BigInteger totalRaised;
        private BigInteger deadline;
        private boolean finalized;
        private boolean exists;
        private String title;
        private String description;
        
        // Constructors, Getters, Setters
        public CampaignInfo() {}
        
        public CampaignInfo(BigInteger id, String beneficiary, BigInteger goalAmount, 
                          BigInteger totalRaised, BigInteger deadline, boolean finalized, 
                          boolean exists, String title, String description) {
            this.id = id;
            this.beneficiary = beneficiary;
            this.goalAmount = goalAmount;
            this.totalRaised = totalRaised;
            this.deadline = deadline;
            this.finalized = finalized;
            this.exists = exists;
            this.title = title;
            this.description = description;
        }

        // Getter 및 Setter
        public BigInteger getId() { return id; }
        public void setId(BigInteger id) { this.id = id; }
        
        public String getBeneficiary() { return beneficiary; }
        public void setBeneficiary(String beneficiary) { this.beneficiary = beneficiary; }
        
        public BigInteger getGoalAmount() { return goalAmount; }
        public void setGoalAmount(BigInteger goalAmount) { this.goalAmount = goalAmount; }
        
        public BigInteger getTotalRaised() { return totalRaised; }
        public void setTotalRaised(BigInteger totalRaised) { this.totalRaised = totalRaised; }
        
        public BigInteger getDeadline() { return deadline; }
        public void setDeadline(BigInteger deadline) { this.deadline = deadline; }
        
        public boolean isFinalized() { return finalized; }
        public void setFinalized(boolean finalized) { this.finalized = finalized; }
        
        public boolean isExists() { return exists; }
        public void setExists(boolean exists) { this.exists = exists; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * 기부 정보 데이터 클래스
     */
    class DonationInfo {
        private BigInteger campaignId;
        private String donorAddress;
        private BigInteger amount;
        private BigInteger blockNumber;
        private String transactionHash;
        
        public DonationInfo() {}
        
        public DonationInfo(BigInteger campaignId, String donorAddress, BigInteger amount,
                          BigInteger blockNumber, String transactionHash) {
            this.campaignId = campaignId;
            this.donorAddress = donorAddress;
            this.amount = amount;
            this.blockNumber = blockNumber;
            this.transactionHash = transactionHash;
        }

        // Getter 및 Setter
        public BigInteger getCampaignId() { return campaignId; }
        public void setCampaignId(BigInteger campaignId) { this.campaignId = campaignId; }
        
        public String getDonorAddress() { return donorAddress; }
        public void setDonorAddress(String donorAddress) { this.donorAddress = donorAddress; }
        
        public BigInteger getAmount() { return amount; }
        public void setAmount(BigInteger amount) { this.amount = amount; }
        
        public BigInteger getBlockNumber() { return blockNumber; }
        public void setBlockNumber(BigInteger blockNumber) { this.blockNumber = blockNumber; }
        
        public String getTransactionHash() { return transactionHash; }
        public void setTransactionHash(String transactionHash) { this.transactionHash = transactionHash; }
    }
    
    /**
     * 트랜잭션 상태 데이터 클래스
     */
    class TransactionStatus {
        private String hash;
        private boolean confirmed;
        private boolean successful;
        private BigInteger blockNumber;
        private BigInteger gasUsed;
        private String errorMessage;
        
        public TransactionStatus() {}
        
        public TransactionStatus(String hash, boolean confirmed, boolean successful,
                               BigInteger blockNumber, BigInteger gasUsed, String errorMessage) {
            this.hash = hash;
            this.confirmed = confirmed;
            this.successful = successful;
            this.blockNumber = blockNumber;
            this.gasUsed = gasUsed;
            this.errorMessage = errorMessage;
        }

        // Getter 및 Setter
        public String getHash() { return hash; }
        public void setHash(String hash) { this.hash = hash; }
        
        public boolean isConfirmed() { return confirmed; }
        public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
        
        public boolean isSuccessful() { return successful; }
        public void setSuccessful(boolean successful) { this.successful = successful; }
        
        public BigInteger getBlockNumber() { return blockNumber; }
        public void setBlockNumber(BigInteger blockNumber) { this.blockNumber = blockNumber; }
        
        public BigInteger getGasUsed() { return gasUsed; }
        public void setGasUsed(BigInteger gasUsed) { this.gasUsed = gasUsed; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}