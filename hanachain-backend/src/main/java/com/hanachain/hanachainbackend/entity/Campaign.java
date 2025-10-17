package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "campaign_seq")
    @SequenceGenerator(name = "campaign_seq", sequenceName = "campaign_sequence", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(length = 500)
    private String subtitle;
    
    @Column(columnDefinition = "CLOB")
    private String description;
    
    @Column(length = 100)
    private String organizer;
    
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal targetAmount = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer donorCount = 0;
    
    @Column(length = 500)
    private String imageUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignCategory category;
    
    @Column(nullable = false)
    private LocalDateTime startDate;
    
    @Column(nullable = false)
    private LocalDateTime endDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CampaignStory> stories;
    
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Donation> donations;
    
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Expense> expenses;
    
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Comment> comments;
    
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CampaignManager> campaignManagers;
    
    // ===== 블록체인 관련 필드 =====

    /**
     * 블록체인 연동 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Builder.Default
    private com.hanachain.hanachainbackend.entity.BlockchainStatus blockchainStatus = com.hanachain.hanachainbackend.entity.BlockchainStatus.NONE;

    /**
     * 블록체인 컨트랙트 주소 (HanaChainCampaign 컨트랙트 주소)
     * 여러 캠페인이 같은 컨트랙트를 공유할 수 있음
     */
    @Column(name = "blockchain_contract_address", length = 66)
    private String blockchainContractAddress;

    /**
     * 블록체인 캠페인 ID (컨트랙트 내부의 캠페인 ID)
     * LEGACY: 기존 blockchain_campaign_id 컬럼을 재사용
     */
    @Column(name = "blockchain_campaign_id")
    private BigInteger blockchainCampaignId;
    
    /**
     * 캠페인 생성 트랜잭션 해시
     */
    @Column(length = 100)
    private String blockchainTransactionHash;
    
    /**
     * 수혜자 블록체인 주소
     */
    @Column(length = 50)
    private String beneficiaryAddress;
    
    /**
     * 블록체인 연동 실패 사유
     */
    @Column(length = 500)
    private String blockchainErrorMessage;
    
    /**
     * 블록체인 최종 처리 시간
     */
    private LocalDateTime blockchainProcessedAt;

    // ===== 배치 작업 관련 필드 =====

    /**
     * 마지막 실행된 배치 작업 ID
     */
    @Column(name = "batch_job_execution_id")
    private Long batchJobExecutionId;

    /**
     * 배치 작업 상태 (PENDING, RUNNING, COMPLETED, FAILED, STOPPED)
     */
    @Column(name = "batch_job_status", length = 20)
    private String batchJobStatus;

    /**
     * 배치 작업 시작 시간
     */
    @Column(name = "batch_started_at")
    private LocalDateTime batchStartedAt;

    /**
     * 배치 작업 완료 시간
     */
    @Column(name = "batch_completed_at")
    private LocalDateTime batchCompletedAt;

    /**
     * 처리된 총 기부 건수
     */
    @Column(name = "total_donations_processed")
    @Builder.Default
    private Integer totalDonationsProcessed = 0;

    /**
     * 성공한 토큰 전송 건수
     */
    @Column(name = "successful_transfers")
    @Builder.Default
    private Integer successfulTransfers = 0;

    /**
     * 실패한 토큰 전송 건수
     */
    @Column(name = "failed_transfers")
    @Builder.Default
    private Integer failedTransfers = 0;

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == CampaignStatus.ACTIVE && 
               now.isAfter(startDate) && 
               now.isBefore(endDate);
    }
    
    public boolean isCompleted() {
        return currentAmount.compareTo(targetAmount) >= 0;
    }
    
    public BigDecimal getProgressPercentage() {
        if (targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentAmount.divide(targetAmount, 4, BigDecimal.ROUND_HALF_UP)
                           .multiply(BigDecimal.valueOf(100));
    }
    
    /**
     * 기부 추가 - 동시성 처리를 위해 synchronized 추가
     */
    public synchronized void addDonation(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("기부 금액은 0보다 커야 합니다.");
        }
        this.currentAmount = this.currentAmount.add(amount);
        this.donorCount++;
    }
    
    /**
     * 기부 취소 - 금액 차감
     */
    public synchronized void cancelDonation(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("취소 금액은 0보다 커야 합니다.");
        }
        
        // 현재 금액이 취소 금액보다 작은 경우 방어
        if (this.currentAmount.compareTo(amount) < 0) {
            throw new IllegalStateException("취소 금액이 현재 모금액보다 큽니다.");
        }
        
        this.currentAmount = this.currentAmount.subtract(amount);
        if (this.donorCount > 0) {
            this.donorCount--;
        }
    }
    
    // 총 사용 금액 계산
    public BigDecimal getTotalExpenseAmount() {
        return expenses.stream()
                .filter(expense -> expense.getStatus() == Expense.ExpenseStatus.COMPLETED)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    // 남은 금액 계산
    public BigDecimal getRemainingAmount() {
        return currentAmount.subtract(getTotalExpenseAmount());
    }
    
    // 사용률 계산 (사용 금액 / 모금 금액)
    public BigDecimal getUsagePercentage() {
        if (currentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return getTotalExpenseAmount().divide(currentAmount, 4, BigDecimal.ROUND_HALF_UP)
                                     .multiply(BigDecimal.valueOf(100));
    }
    
    // 활성 댓글 수 조회
    public long getActiveCommentCount() {
        return comments.stream()
                .filter(comment -> comment.getStatus() == Comment.CommentStatus.ACTIVE)
                .count();
    }
    
    // ===== 블록체인 관련 헬퍼 메서드 =====
    
    /**
     * 블록체인 연동이 완료되었는지 확인
     */
    public boolean isBlockchainActive() {
        return blockchainStatus == com.hanachain.hanachainbackend.entity.BlockchainStatus.ACTIVE &&
               blockchainCampaignId != null;
    }
    
    /**
     * 블록체인 연동이 진행 중인지 확인
     */
    public boolean isBlockchainProcessing() {
        return blockchainStatus != null && blockchainStatus.isProcessing();
    }
    
    /**
     * 블록체인 연동이 실패했는지 확인
     */
    public boolean isBlockchainFailed() {
        return blockchainStatus != null && blockchainStatus.isFailed();
    }
    
    /**
     * 블록체인 상태 업데이트
     */
    public void updateBlockchainStatus(com.hanachain.hanachainbackend.entity.BlockchainStatus status, 
                                      String errorMessage) {
        this.blockchainStatus = status;
        this.blockchainErrorMessage = errorMessage;
        this.blockchainProcessedAt = LocalDateTime.now();
    }
    
    /**
     * 블록체인 캠페인 생성 성공 시 호출
     */
    public void onBlockchainCampaignCreated(String contractAddress, BigInteger campaignId, String transactionHash) {
        this.blockchainContractAddress = contractAddress;
        this.blockchainCampaignId = campaignId;
        this.blockchainTransactionHash = transactionHash;
        this.blockchainStatus = com.hanachain.hanachainbackend.entity.BlockchainStatus.ACTIVE;
        this.blockchainProcessedAt = LocalDateTime.now();
        this.blockchainErrorMessage = null;
    }
    
    public enum CampaignStatus {
        DRAFT,
        PENDING_APPROVAL,
        ACTIVE,
        PAUSED,
        COMPLETED,
        CANCELLED
    }
    
    public enum CampaignCategory {
        MEDICAL,
        EDUCATION,
        DISASTER_RELIEF,
        ENVIRONMENT,
        ANIMAL_WELFARE,
        COMMUNITY,
        EMERGENCY,
        OTHER
    }
}
