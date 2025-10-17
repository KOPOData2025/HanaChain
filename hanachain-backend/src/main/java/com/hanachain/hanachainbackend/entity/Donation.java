package com.hanachain.hanachainbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "donation_seq")
    @SequenceGenerator(name = "donation_seq", sequenceName = "donation_sequence", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(length = 500)
    private String message;
    
    @Column(nullable = false, unique = true, length = 100)
    private String paymentId; // 결제 시스템의 고유 ID
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean anonymous = false;
    
    @Column(length = 50)
    private String donorName; // 익명이 아닌 경우 표시할 이름
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // null 가능 (비회원 기부)
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
    
    @Column
    private LocalDateTime paidAt;
    
    @Column
    private LocalDateTime cancelledAt;
    
    @Column(length = 1000)
    private String failureReason;
    
    // ===== 블록체인 관련 필드 =====
    
    /**
     * 블록체인 연동 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "blockchain_status", length = 30)
    @Builder.Default
    private BlockchainStatus blockchainStatus = BlockchainStatus.NONE;
    
    /**
     * 기부 트랜잭션 해시
     */
    @Column(name = "donation_transaction_hash", length = 100)
    private String donationTransactionHash;
    
    /**
     * 기부자 지갑 주소
     */
    @Column(name = "donor_wallet_address", length = 50)
    private String donorWalletAddress;
    
    /**
     * 블록체인 기록 여부
     */
    @Column(name = "blockchain_recorded")
    @Builder.Default
    private Boolean blockchainRecorded = false;
    
    /**
     * 블록체인 기록 시간
     */
    @Column(name = "blockchain_recorded_at")
    private LocalDateTime blockchainRecordedAt;
    
    /**
     * 블록체인 에러 메시지
     */
    @Column(name = "blockchain_error_message", length = 500)
    private String blockchainErrorMessage;
    
    /**
     * 토큰 타입 (USDC, ETH 등)
     */
    @Column(name = "token_type", length = 30)
    private String tokenType;

    /**
     * 토큰 수량 (블록체인상 실제 수량)
     */
    @Column(name = "token_amount", precision = 38, scale = 18)
    private BigDecimal tokenAmount;

    /**
     * 가스 수수료
     */
    @Column(name = "gas_fee", precision = 38, scale = 18)
    private BigDecimal gasFee;

    // ===== FDS (사기 탐지 시스템) 관련 필드 =====

    /**
     * FDS 검증 액션 (APPROVE, MANUAL_REVIEW, BLOCK)
     */
    @Column(name = "fds_action", length = 30)
    private String fdsAction;

    /**
     * FDS 위험 점수 (0.0000 ~ 1.0000)
     */
    @Column(name = "fds_risk_score", precision = 5, scale = 4)
    private BigDecimal fdsRiskScore;

    /**
     * FDS 신뢰도 (0.0000 ~ 1.0000)
     */
    @Column(name = "fds_confidence", precision = 5, scale = 4)
    private BigDecimal fdsConfidence;

    /**
     * FDS 검증 시각
     */
    @Column(name = "fds_checked_at")
    private LocalDateTime fdsCheckedAt;

    /**
     * FDS 검증 설명
     */
    @Column(name = "fds_explanation", length = 500)
    private String fdsExplanation;

    /**
     * FDS 검증 상태 (PENDING, SUCCESS, FAILED, TIMEOUT)
     */
    @Column(name = "fds_status", length = 20)
    @Builder.Default
    private String fdsStatus = "PENDING";

    /**
     * FDS 검증 상세 정보 (JSON)
     * features, Q-values 등을 JSON 형태로 저장
     */
    @Column(name = "fds_detail_json")
    @Lob
    private String fdsDetailJson;

    // 결제 완료 처리
    public void markAsPaid() {
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
    }
    
    // 결제 실패 처리
    public void markAsFailed(String reason) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.failureReason = reason;
    }
    
    // 결제 취소 처리
    public void markAsCancelled() {
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }
    
    public boolean isSuccessful() {
        return paymentStatus == PaymentStatus.COMPLETED;
    }
    
    // ===== 블록체인 관련 헬퍼 메서드 =====
    
    /**
     * 블록체인에 기록되었는지 확인
     */
    public boolean isBlockchainRecorded() {
        return Boolean.TRUE.equals(blockchainRecorded) && 
               donationTransactionHash != null;
    }
    
    /**
     * 블록체인 처리 중인지 확인
     */
    public boolean isBlockchainProcessing() {
        return blockchainStatus != null && blockchainStatus.isProcessing();
    }
    
    /**
     * 블록체인 기록 실패했는지 확인
     */
    public boolean isBlockchainFailed() {
        return blockchainStatus != null && blockchainStatus.isFailed();
    }
    
    /**
     * 블록체인 기록 성공 시 호출
     */
    public void onBlockchainRecorded(String transactionHash, String walletAddress) {
        this.donationTransactionHash = transactionHash;
        this.donorWalletAddress = walletAddress;
        this.blockchainRecorded = true;
        this.blockchainRecordedAt = LocalDateTime.now();
        this.blockchainStatus = BlockchainStatus.ACTIVE;
        this.blockchainErrorMessage = null;
    }
    
    /**
     * 블록체인 상태 업데이트
     */
    public void updateBlockchainStatus(BlockchainStatus status, String errorMessage) {
        this.blockchainStatus = status;
        this.blockchainErrorMessage = errorMessage;
        if (status == BlockchainStatus.ACTIVE) {
            this.blockchainRecorded = true;
            this.blockchainRecordedAt = LocalDateTime.now();
        }
    }

    // ===== FDS 관련 헬퍼 메서드 =====

    /**
     * FDS 검증 결과 업데이트
     */
    public void updateFdsResult(
        String action,
        BigDecimal riskScore,
        BigDecimal confidence,
        String explanation
    ) {
        this.fdsAction = action;
        this.fdsRiskScore = riskScore;
        this.fdsConfidence = confidence;
        this.fdsExplanation = explanation;
        this.fdsCheckedAt = LocalDateTime.now();
        this.fdsStatus = "SUCCESS";
    }

    /**
     * FDS 검증 실패 처리
     */
    public void markFdsFailed(String reason) {
        this.fdsStatus = "FAILED";
        this.fdsExplanation = reason;
        this.fdsCheckedAt = LocalDateTime.now();
    }

    /**
     * FDS 검증 타임아웃 처리
     */
    public void markFdsTimeout() {
        this.fdsStatus = "TIMEOUT";
        this.fdsExplanation = "FDS verification timeout";
        this.fdsCheckedAt = LocalDateTime.now();
    }

    /**
     * FDS 검증 완료 여부 확인
     */
    public boolean isFdsVerified() {
        return "SUCCESS".equals(fdsStatus) && fdsAction != null;
    }

    /**
     * FDS 고위험 거래 여부 확인
     */
    public boolean isFdsHighRisk() {
        return "BLOCK".equals(fdsAction) ||
               (fdsRiskScore != null && fdsRiskScore.compareTo(new BigDecimal("0.7")) > 0);
    }
    
    public enum PaymentStatus {
        PENDING,    // 결제 대기
        PROCESSING, // 결제 처리 중
        COMPLETED,  // 결제 완료
        FAILED,     // 결제 실패
        CANCELLED,  // 결제 취소
        REFUNDED    // 환불 완료
    }
    
    public enum PaymentMethod {
        CREDIT_CARD,
        BANK_TRANSFER,
        VIRTUAL_ACCOUNT,
        MOBILE_PAYMENT,
        PAYPAL,
        OTHER
    }
}
