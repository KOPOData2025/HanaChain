package com.hanachain.hanachainbackend.exception.batch;

/**
 * 블록체인 네트워크 예외
 *
 * 블록체인 네트워크 통신 오류 시 발생합니다.
 * Spring Batch에서 Retry 처리되어 최대 3회까지 재시도합니다.
 */
public class BlockchainNetworkException extends RuntimeException {

    private final Long donationId;
    private final String operation;

    public BlockchainNetworkException(Long donationId, String operation, String message) {
        super(String.format("Blockchain network error for donation %d during %s: %s", donationId, operation, message));
        this.donationId = donationId;
        this.operation = operation;
    }

    public BlockchainNetworkException(Long donationId, String operation, Throwable cause) {
        super(String.format("Blockchain network error for donation %d during %s", donationId, operation), cause);
        this.donationId = donationId;
        this.operation = operation;
    }

    public Long getDonationId() {
        return donationId;
    }

    public String getOperation() {
        return operation;
    }
}
