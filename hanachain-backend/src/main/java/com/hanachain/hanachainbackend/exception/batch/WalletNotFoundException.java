package com.hanachain.hanachainbackend.exception.batch;

/**
 * 지갑 주소 없음 예외
 *
 * 기부자의 지갑 주소가 등록되지 않은 경우 발생합니다.
 * Spring Batch에서 Skip 처리되어 다음 기부 건으로 진행합니다.
 */
public class WalletNotFoundException extends RuntimeException {

    private final Long donationId;
    private final Long userId;

    public WalletNotFoundException(Long donationId, Long userId) {
        super(String.format("Wallet not found for donation %d, user %d", donationId, userId));
        this.donationId = donationId;
        this.userId = userId;
    }

    public WalletNotFoundException(Long donationId, Long userId, String message) {
        super(message);
        this.donationId = donationId;
        this.userId = userId;
    }

    public Long getDonationId() {
        return donationId;
    }

    public Long getUserId() {
        return userId;
    }
}
