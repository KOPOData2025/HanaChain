package com.hanachain.hanachainbackend.exception.batch;

/**
 * 잔액 부족 예외
 *
 * USDC 토큰 전송 시 기부자의 잔액이 부족한 경우 발생합니다.
 * Spring Batch에서 Skip 처리되어 다음 기부 건으로 진행합니다.
 */
public class InsufficientBalanceException extends RuntimeException {

    private final Long donationId;
    private final String walletAddress;

    public InsufficientBalanceException(Long donationId, String walletAddress) {
        super(String.format("Insufficient balance for donation %d from wallet %s", donationId, walletAddress));
        this.donationId = donationId;
        this.walletAddress = walletAddress;
    }

    public InsufficientBalanceException(Long donationId, String walletAddress, String message) {
        super(message);
        this.donationId = donationId;
        this.walletAddress = walletAddress;
    }

    public Long getDonationId() {
        return donationId;
    }

    public String getWalletAddress() {
        return walletAddress;
    }
}
