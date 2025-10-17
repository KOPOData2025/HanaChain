package com.hanachain.hanachainbackend.dto.donation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PortOne 결제 웹훅
 * PortOne에서 전송하는 웹훅 데이터 구조
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhook {
    
    /**
     * 웹훅 기본 정보
     */
    @JsonProperty("imp_uid")
    private String impUid; // 포트원 거래 고유번호
    
    @JsonProperty("merchant_uid")
    private String merchantUid; // 가맹점 주문번호 (우리의 paymentId)
    
    @JsonProperty("status")
    private String status; // 결제상태 (paid, failed, cancelled 등)
    
    /**
     * 결제 정보
     */
    @JsonProperty("amount")
    private BigDecimal amount; // 결제금액
    
    @JsonProperty("currency")
    private String currency; // 통화코드 (KRW)
    
    @JsonProperty("pay_method")
    private String payMethod; // 결제수단
    
    @JsonProperty("pg_provider")
    private String pgProvider; // PG사 구분코드
    
    @JsonProperty("pg_tid")
    private String pgTid; // PG사 거래번호
    
    /**
     * 구매자 정보
     */
    @JsonProperty("buyer_name")
    private String buyerName; // 구매자명
    
    @JsonProperty("buyer_email")
    private String buyerEmail; // 구매자 이메일
    
    @JsonProperty("buyer_tel")
    private String buyerTel; // 구매자 전화번호
    
    /**
     * 시간 정보
     */
    @JsonProperty("paid_at")
    private Long paidAt; // 결제완료시점 UNIX timestamp
    
    @JsonProperty("failed_at")
    private Long failedAt; // 결제실패시점 UNIX timestamp
    
    @JsonProperty("cancelled_at")
    private Long cancelledAt; // 결제취소시점 UNIX timestamp
    
    /**
     * 실패/취소 정보
     */
    @JsonProperty("fail_reason")
    private String failReason; // 결제실패 사유
    
    @JsonProperty("cancel_reason")
    private String cancelReason; // 결제취소 사유
    
    /**
     * 사용자 정의 데이터
     */
    @JsonProperty("custom_data")
    private String customData; // 결제시 전달한 사용자 정의 데이터
    
    /**
     * 결제 상태 확인 메서드들
     */
    public boolean isPaid() {
        return "paid".equals(status);
    }
    
    public boolean isFailed() {
        return "failed".equals(status);
    }
    
    public boolean isCancelled() {
        return "cancelled".equals(status);
    }
    
    /**
     * 결제 완료 시점을 LocalDateTime으로 변환
     */
    public LocalDateTime getPaidAtAsLocalDateTime() {
        return paidAt != null ? 
            LocalDateTime.ofEpochSecond(paidAt, 0, java.time.ZoneOffset.of("+09:00")) : 
            null;
    }
    
    /**
     * 결제 실패 시점을 LocalDateTime으로 변환
     */
    public LocalDateTime getFailedAtAsLocalDateTime() {
        return failedAt != null ? 
            LocalDateTime.ofEpochSecond(failedAt, 0, java.time.ZoneOffset.of("+09:00")) : 
            null;
    }
    
    /**
     * 결제 취소 시점을 LocalDateTime으로 변환
     */
    public LocalDateTime getCancelledAtAsLocalDateTime() {
        return cancelledAt != null ? 
            LocalDateTime.ofEpochSecond(cancelledAt, 0, java.time.ZoneOffset.of("+09:00")) : 
            null;
    }
}