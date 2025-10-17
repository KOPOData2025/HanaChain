package com.hanachain.hanachainbackend.dto.blockchain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 블록체인 트랜잭션 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockchainTransaction {

    /**
     * 트랜잭션 해시
     */
    private String transactionHash;

    /**
     * 블록 번호
     */
    private String blockNumber;

    /**
     * 트랜잭션 타임스탬프
     */
    private LocalDateTime timestamp;

    /**
     * 발신자 주소
     */
    private String from;

    /**
     * 수신자 주소 (스마트 컨트랙트 주소)
     */
    private String to;

    /**
     * 전송 금액 (USDC)
     */
    private String value;

    /**
     * 이벤트 타입
     */
    private EventType eventType;

    /**
     * 기부자명 (DonationMade 이벤트인 경우)
     */
    private String donorName;

    /**
     * 익명 여부
     */
    private Boolean anonymous;

    /**
     * 블록체인 이벤트 타입
     */
    public enum EventType {
        DONATION_MADE("DonationMade"),
        CAMPAIGN_CREATED("CampaignCreated"),
        CAMPAIGN_FINALIZED("CampaignFinalized"),
        CAMPAIGN_CANCELLED("CampaignCancelled");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
