package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 기부 증서 응답 DTO
 * 블록체인 기반 기부 증서 발급을 위한 데이터
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationCertificateResponse {

    /**
     * 기부자명 (익명인 경우 "익명의 기부자"로 표시)
     */
    private String donorName;

    /**
     * 기부 금액
     */
    private BigDecimal amount;

    /**
     * 기부 날짜 (결제 완료 시간)
     */
    private LocalDateTime donatedAt;

    /**
     * 캠페인 제목
     */
    private String campaignTitle;

    /**
     * 캠페인 주최 단체명
     */
    private String campaignOrganization;

    /**
     * 블록체인 트랜잭션 해시 (전체)
     * 블록체인 검증을 위한 필수 정보
     */
    private String donationTransactionHash;

    /**
     * 기부 ID (영수증 번호)
     */
    private String donationId;

    /**
     * 캠페인 대표 이미지 URL
     */
    private String campaignImage;

    /**
     * Donation 엔티티로부터 DonationCertificateResponse 생성
     * @param donation 기부 엔티티
     * @param isAnonymous 익명 기부 여부
     * @return 기부 증서 응답 DTO
     */
    public static DonationCertificateResponse from(
            com.hanachain.hanachainbackend.entity.Donation donation,
            boolean isAnonymous) {

        // 기부자명 처리 (익명인 경우 "익명의 기부자"로 표시)
        String donorName = isAnonymous ? "익명의 기부자" : donation.getUser().getName();

        // 캠페인 주최 단체명 (organizer 필드가 없으면 사용자명 사용)
        String organization = donation.getCampaign().getUser().getName();

        // 캠페인 이미지 URL (Campaign 엔티티에서 가져오기)
        String campaignImage = donation.getCampaign().getImageUrl();

        return DonationCertificateResponse.builder()
                .donorName(donorName)
                .amount(donation.getAmount())
                .donatedAt(donation.getPaidAt() != null ? donation.getPaidAt() : donation.getCreatedAt())
                .campaignTitle(donation.getCampaign().getTitle())
                .campaignOrganization(organization)
                .donationTransactionHash(donation.getDonationTransactionHash())
                .donationId(donation.getId().toString())
                .campaignImage(campaignImage)
                .build();
    }
}
