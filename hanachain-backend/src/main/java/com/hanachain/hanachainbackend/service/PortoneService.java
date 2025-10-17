package com.hanachain.hanachainbackend.service;

import java.math.BigDecimal;

/**
 * PortOne 결제 서비스 인터페이스
 */
public interface PortoneService {

    /**
     * 결제 취소 (전액 환불)
     *
     * @param paymentId PortOne 결제 ID
     * @param reason 취소 사유
     * @return 취소 성공 여부
     */
    boolean cancelPayment(String paymentId, String reason);

    /**
     * 결제 부분 취소 (부분 환불)
     *
     * @param paymentId PortOne 결제 ID
     * @param amount 취소 금액
     * @param reason 취소 사유
     * @return 취소 성공 여부
     */
    boolean cancelPaymentPartial(String paymentId, BigDecimal amount, String reason);

    /**
     * 결제 정보 조회
     *
     * @param paymentId PortOne 결제 ID
     * @return 결제 정보 (JSON)
     */
    String getPaymentInfo(String paymentId);
}
