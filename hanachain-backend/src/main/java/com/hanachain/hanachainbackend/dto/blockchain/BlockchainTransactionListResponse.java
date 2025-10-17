package com.hanachain.hanachainbackend.dto.blockchain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 블록체인 트랜잭션 목록 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockchainTransactionListResponse {

    /**
     * 트랜잭션 목록
     */
    private List<BlockchainTransaction> transactions;

    /**
     * 총 트랜잭션 수
     */
    private Integer totalCount;

    /**
     * 마지막 업데이트 시간
     */
    private LocalDateTime lastUpdated;
}
