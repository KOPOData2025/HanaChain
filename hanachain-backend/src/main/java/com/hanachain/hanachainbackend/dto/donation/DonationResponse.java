package com.hanachain.hanachainbackend.dto.donation;

import com.hanachain.hanachainbackend.entity.Donation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for donation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationResponse {
    
    private Long id;
    private BigDecimal amount;
    private String message;
    private String paymentId;
    private Donation.PaymentStatus paymentStatus;
    private Donation.PaymentMethod paymentMethod;
    private Boolean anonymous;
    private String donorName;
    private Long campaignId;
    private String campaignTitle;
    private String creatorName; // 캠페인 생성자명
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private String failureReason;

    // FDS (사기 탐지 시스템) 관련 필드
    private String fdsAction;
    private BigDecimal fdsRiskScore;
    private BigDecimal fdsConfidence;
    private LocalDateTime fdsCheckedAt;
    private String fdsExplanation;
    private String fdsStatus;
    
    /**
     * Entity에서 DTO로 변환하는 정적 메서드
     */
    public static DonationResponse fromEntity(Donation donation) {
        return DonationResponse.builder()
                .id(donation.getId())
                .amount(donation.getAmount())
                .message(donation.getMessage())
                .paymentId(donation.getPaymentId())
                .paymentStatus(donation.getPaymentStatus())
                .paymentMethod(donation.getPaymentMethod())
                .anonymous(donation.getAnonymous())
                .donorName(donation.getAnonymous() ? "익명" : donation.getDonorName())
                .campaignId(donation.getCampaign().getId())
                .campaignTitle(donation.getCampaign().getTitle())
                .creatorName(donation.getCampaign().getUser().getNickname())
                .createdAt(donation.getCreatedAt())
                .paidAt(donation.getPaidAt())
                .cancelledAt(donation.getCancelledAt())
                .failureReason(donation.getFailureReason())
                // FDS 관련 필드
                .fdsAction(donation.getFdsAction())
                .fdsRiskScore(donation.getFdsRiskScore())
                .fdsConfidence(donation.getFdsConfidence())
                .fdsCheckedAt(donation.getFdsCheckedAt())
                .fdsExplanation(donation.getFdsExplanation())
                .fdsStatus(donation.getFdsStatus())
                .build();
    }
    
    /**
     * 간단한 형태의 기부 정보만 포함하는 DTO 생성
     */
    public static DonationResponse fromEntitySimple(Donation donation) {
        return DonationResponse.builder()
                .id(donation.getId())
                .amount(donation.getAmount())
                .paymentStatus(donation.getPaymentStatus())
                .anonymous(donation.getAnonymous())
                .donorName(donation.getAnonymous() ? "익명" : donation.getDonorName())
                .campaignId(donation.getCampaign().getId())
                .campaignTitle(donation.getCampaign().getTitle())
                .createdAt(donation.getCreatedAt())
                .paidAt(donation.getPaidAt())
                .build();
    }
}