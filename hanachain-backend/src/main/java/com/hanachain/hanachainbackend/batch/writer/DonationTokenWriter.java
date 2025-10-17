package com.hanachain.hanachainbackend.batch.writer;

import com.hanachain.hanachainbackend.dto.batch.DonationTransferResult;
import com.hanachain.hanachainbackend.entity.BlockchainStatus;
import com.hanachain.hanachainbackend.entity.Donation;
import com.hanachain.hanachainbackend.repository.DonationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 기부 토큰 전송 결과 Writer
 *
 * ItemProcessor에서 처리된 토큰 전송 결과를 DB에 저장합니다.
 * 성공한 경우 트랜잭션 해시를 저장하고, 실패한 경우 에러 정보를 기록합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DonationTokenWriter implements ItemWriter<DonationTransferResult> {

    private final DonationRepository donationRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends DonationTransferResult> chunk) throws Exception {
        List<? extends DonationTransferResult> results = chunk.getItems();

        log.info("Writing {} donation transfer results", results.size());

        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new ArrayList<>();

        for (DonationTransferResult result : results) {
            try {
                if (Boolean.TRUE.equals(result.getSuccess())) {
                    updateSuccessfulTransfer(result);
                    successCount++;
                } else {
                    updateFailedTransfer(result);
                    failureCount++;
                    errors.add(String.format("Donation %d: %s (%s)",
                            result.getDonationId(), result.getErrorMessage(), result.getErrorType()));
                }
            } catch (Exception e) {
                log.error("Failed to write result for donation: {}", result.getDonationId(), e);
                failureCount++;
                errors.add(String.format("Donation %d: Write error - %s",
                        result.getDonationId(), e.getMessage()));
            }
        }

        log.info("Write completed - Success: {}, Failure: {}", successCount, failureCount);

        if (!errors.isEmpty()) {
            log.warn("Failed transfers: {}", String.join("; ", errors));
        }
    }

    /**
     * 성공한 토큰 전송 정보 업데이트
     */
    private void updateSuccessfulTransfer(DonationTransferResult result) {
        Donation donation = donationRepository.findById(result.getDonationId())
                .orElseThrow(() -> new IllegalStateException(
                        "Donation not found: " + result.getDonationId()));

        // 블록체인 정보 업데이트
        donation.setDonationTransactionHash(result.getTransactionHash());
        donation.setDonorWalletAddress(result.getDonorWalletAddress());
        donation.setTokenAmount(result.getTokenAmount());
        donation.setGasFee(result.getGasFee());
        donation.setTokenType("USDC");

        // 블록체인 기록 완료 처리
        donation.onBlockchainRecorded(
                result.getTransactionHash(),
                result.getDonorWalletAddress()
        );

        donationRepository.save(donation);

        log.debug("Updated successful transfer for donation: {}, tx: {}",
                donation.getId(), result.getTransactionHash());
    }

    /**
     * 실패한 토큰 전송 정보 업데이트
     */
    private void updateFailedTransfer(DonationTransferResult result) {
        Donation donation = donationRepository.findById(result.getDonationId())
                .orElseThrow(() -> new IllegalStateException(
                        "Donation not found: " + result.getDonationId()));

        // 실패 정보 업데이트
        String errorMessage = String.format("[%s] %s",
                result.getErrorType(), result.getErrorMessage());

        donation.updateBlockchainStatus(BlockchainStatus.BLOCKCHAIN_FAILED, errorMessage);

        donationRepository.save(donation);

        log.warn("Updated failed transfer for donation: {}, error: {}",
                donation.getId(), errorMessage);
    }
}
