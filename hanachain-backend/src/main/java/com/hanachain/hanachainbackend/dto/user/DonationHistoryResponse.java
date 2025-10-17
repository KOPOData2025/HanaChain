package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 기부 이력 응답 DTO
 * 프론트엔드의 DonationRecord 인터페이스와 매핑
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationHistoryResponse {
    
    private String id;
    private String campaignId;
    private String campaignTitle;
    private String campaignImage;
    private BigDecimal amount;
    private String status; // 'completed', 'pending', 'failed', 'cancelled'
    private LocalDateTime donatedAt;
    private String message;
    private String paymentMethod; // 'card', 'bank', 'naverpay', 'kakaopay'
    private String receiptNumber;
    private String donationTransactionHash; // 블록체인 트랜잭션 해시
    
    /**
     * Donation 엔티티로부터 DonationHistoryResponse 생성
     */
    public static DonationHistoryResponse from(com.hanachain.hanachainbackend.entity.Donation donation) {
        return DonationHistoryResponse.builder()
                .id(donation.getId().toString())
                .campaignId(donation.getCampaign().getId().toString())
                .campaignTitle(donation.getCampaign().getTitle())
                .campaignImage(donation.getCampaign().getImageUrl())
                .amount(donation.getAmount())
                .status(mapPaymentStatus(donation.getPaymentStatus()))
                .donatedAt(donation.getPaidAt() != null ? donation.getPaidAt() : donation.getCreatedAt())
                .message(donation.getMessage())
                .paymentMethod(mapPaymentMethod(donation.getPaymentMethod()))
                .receiptNumber(donation.getPaymentId())
                .donationTransactionHash(donation.getDonationTransactionHash())
                .build();
    }
    
    /**
     * PaymentStatus를 프론트엔드 형식으로 변환
     */
    private static String mapPaymentStatus(com.hanachain.hanachainbackend.entity.Donation.PaymentStatus status) {
        if (status == null) return "pending";
        
        return switch (status) {
            case COMPLETED -> "completed";
            case PENDING, PROCESSING -> "pending";
            case FAILED -> "failed";
            case CANCELLED, REFUNDED -> "cancelled";
        };
    }
    
    /**
     * PaymentMethod를 프론트엔드 형식으로 변환
     */
    private static String mapPaymentMethod(com.hanachain.hanachainbackend.entity.Donation.PaymentMethod method) {
        if (method == null) return null;
        
        return switch (method) {
            case CREDIT_CARD -> "card";
            case BANK_TRANSFER -> "bank";
            case VIRTUAL_ACCOUNT -> "bank";
            case MOBILE_PAYMENT -> "naverpay"; // 모바일 결제를 naverpay로 매핑
            case PAYPAL -> "paypal";
            case OTHER -> "other";
        };
    }
}