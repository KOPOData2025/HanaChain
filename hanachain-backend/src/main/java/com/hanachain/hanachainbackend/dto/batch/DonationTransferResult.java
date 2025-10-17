package com.hanachain.hanachainbackend.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 기부 토큰 전송 결과 DTO
 *
 * Batch ItemProcessor에서 ItemWriter로 전달되는 결과 객체입니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationTransferResult {

    /**
     * 기부 ID
     */
    private Long donationId;

    /**
     * 전송 성공 여부
     */
    private Boolean success;

    /**
     * 트랜잭션 해시
     */
    private String transactionHash;

    /**
     * 기부자 지갑 주소
     */
    private String donorWalletAddress;

    /**
     * 수혜자 지갑 주소
     */
    private String beneficiaryAddress;

    /**
     * 전송 토큰 수량
     */
    private BigDecimal tokenAmount;

    /**
     * 가스비
     */
    private BigDecimal gasFee;

    /**
     * 에러 메시지 (실패 시)
     */
    private String errorMessage;

    /**
     * 에러 타입 (실패 시)
     */
    private String errorType;

    /**
     * 처리 시간 (밀리초)
     */
    private Long processingTimeMs;

    /**
     * 성공 결과 생성
     */
    public static DonationTransferResult success(
            Long donationId,
            String transactionHash,
            String donorWalletAddress,
            String beneficiaryAddress,
            BigDecimal tokenAmount,
            BigDecimal gasFee,
            Long processingTimeMs
    ) {
        return DonationTransferResult.builder()
                .donationId(donationId)
                .success(true)
                .transactionHash(transactionHash)
                .donorWalletAddress(donorWalletAddress)
                .beneficiaryAddress(beneficiaryAddress)
                .tokenAmount(tokenAmount)
                .gasFee(gasFee)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    /**
     * 실패 결과 생성
     */
    public static DonationTransferResult failure(
            Long donationId,
            String errorMessage,
            String errorType,
            Long processingTimeMs
    ) {
        return DonationTransferResult.builder()
                .donationId(donationId)
                .success(false)
                .errorMessage(errorMessage)
                .errorType(errorType)
                .processingTimeMs(processingTimeMs)
                .build();
    }
}
