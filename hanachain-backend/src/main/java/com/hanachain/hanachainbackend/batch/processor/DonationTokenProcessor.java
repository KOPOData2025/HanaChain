package com.hanachain.hanachainbackend.batch.processor;

import com.hanachain.hanachainbackend.dto.batch.DonationTransferResult;
import com.hanachain.hanachainbackend.entity.Donation;
import com.hanachain.hanachainbackend.entity.UserWallet;
import com.hanachain.hanachainbackend.exception.batch.BlockchainNetworkException;
import com.hanachain.hanachainbackend.exception.batch.InsufficientBalanceException;
import com.hanachain.hanachainbackend.exception.batch.WalletNotFoundException;
import com.hanachain.hanachainbackend.service.WalletService;
import com.hanachain.hanachainbackend.service.blockchain.HanaChainCampaignWrapper;
import com.hanachain.hanachainbackend.service.blockchain.MockUSDCWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

/**
 * 기부 토큰 전송 Processor
 *
 * 기부 내역을 처리하여 USDC 토큰을 기부자 지갑에서 캠페인 수혜자 지갑으로 전송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DonationTokenProcessor implements ItemProcessor<Donation, DonationTransferResult> {

    private final WalletService walletService;
    private final Web3j web3j;
    private final MockUSDCWrapper mockUSDCWrapper;
    private final HanaChainCampaignWrapper hanaChainCampaignWrapper;

    @Value("${blockchain.usdc.contract.address}")
    private String usdcContractAddress;

    @Value("${blockchain.campaign.contract.address}")
    private String campaignContractAddress;

    @Value("${blockchain.network.chain-id}")
    private Long chainId;

    @Value("${blockchain.gas.price.max-gwei:50}")
    private Long maxGasPriceGwei;

    @Value("${blockchain.gas.limit:100000}")
    private Long gasLimit;

    @Value("${blockchain.platform.wallet.private-key}")
    private String platformPrivateKey;

    @Override
    public DonationTransferResult process(Donation donation) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("Processing donation: {}, amount: {}", donation.getId(), donation.getAmount());

        try {
            // 1. 지갑 주소 확인
            validateWallets(donation);

            // 2. 기부자 지갑 정보 조회
            UserWallet donorWallet = getUserWallet(donation);

            // 3. 블록체인 캠페인 컨트랙트 주소 및 campaignId 확인
            String contractAddress = donation.getCampaign().getBlockchainContractAddress();
            if (contractAddress == null || contractAddress.trim().isEmpty()) {
                throw new IllegalStateException("Blockchain contract address not found for campaign: "
                        + donation.getCampaign().getId());
            }

            BigInteger blockchainCampaignId = donation.getCampaign().getBlockchainCampaignId();
            if (blockchainCampaignId == null) {
                throw new IllegalStateException("Blockchain campaign ID not found for campaign: "
                        + donation.getCampaign().getId());
            }

            // 4. 기부 금액 확인
            BigDecimal tokenAmount = donation.getAmount();
            if (tokenAmount == null) {
                throw new IllegalStateException("Donation amount is null for donation: " + donation.getId());
            }

            // 5. 플랫폼 지갑 → 사용자 지갑으로 가스비 ETH 전송
            String gasTxHash = transferGasFeeToUserWallet(donorWallet);
            log.info("⛽ Gas fee (ETH) transferred to user wallet: {} - TX: {}",
                     donorWallet.getWalletAddress(), gasTxHash);

            // 6. 플랫폼 지갑 → 사용자 지갑으로 USDC 충전 (Faucet)
            String chargeTxHash = chargeUSDCToUserWallet(donorWallet, tokenAmount);
            log.info("💰 USDC charged to user wallet: {} - TX: {}",
                     donorWallet.getWalletAddress(), chargeTxHash);

            // 7. 사용자 지갑 USDC 잔액 확인
            checkBalance(donorWallet.getWalletAddress(), tokenAmount);

            // 8. 사용자 지갑 → HanaChainCampaign 컨트랙트로 기부 (approve + donate)
            String donateTxHash = donateToHanaChain(
                    donorWallet,
                    contractAddress,
                    blockchainCampaignId,
                    tokenAmount
            );

            // 9. 가스비 계산
            BigDecimal gasFee = calculateGasFee();

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("✅ Successfully processed donation: {}, donate TX: {}, time: {}ms",
                    donation.getId(), donateTxHash, processingTime);

            return DonationTransferResult.success(
                    donation.getId(),
                    donateTxHash,  // 최종 기부 트랜잭션 해시
                    donorWallet.getWalletAddress(),
                    donation.getCampaign().getBeneficiaryAddress(),
                    tokenAmount,
                    gasFee,
                    processingTime
            );

        } catch (WalletNotFoundException e) {
            log.warn("Wallet not found for donation: {} - SKIPPING", donation.getId(), e);
            long processingTime = System.currentTimeMillis() - startTime;
            return DonationTransferResult.failure(
                    donation.getId(),
                    e.getMessage(),
                    "WALLET_NOT_FOUND",
                    processingTime
            );

        } catch (InsufficientBalanceException e) {
            log.warn("Insufficient balance for donation: {} - SKIPPING", donation.getId(), e);
            long processingTime = System.currentTimeMillis() - startTime;
            return DonationTransferResult.failure(
                    donation.getId(),
                    e.getMessage(),
                    "INSUFFICIENT_BALANCE",
                    processingTime
            );

        } catch (BlockchainNetworkException e) {
            log.error("Network error for donation: {} - RETRYING", donation.getId(), e);
            throw e; // Retry 처리를 위해 예외를 다시 던짐

        } catch (Exception e) {
            log.error("Unexpected error processing donation: {}", donation.getId(), e);
            long processingTime = System.currentTimeMillis() - startTime;
            return DonationTransferResult.failure(
                    donation.getId(),
                    e.getMessage(),
                    "UNKNOWN_ERROR",
                    processingTime
            );
        }
    }

    /**
     * 지갑 주소 검증
     */
    private void validateWallets(Donation donation) {
        if (donation.getUser() == null) {
            throw new WalletNotFoundException(donation.getId(), null,
                    "Donation has no associated user");
        }

        if (donation.getCampaign() == null || donation.getCampaign().getBeneficiaryAddress() == null) {
            throw new IllegalStateException("Campaign or beneficiary address not found");
        }
    }

    /**
     * 사용자 지갑 조회
     */
    private UserWallet getUserWallet(Donation donation) {
        Optional<UserWallet> walletOpt = walletService.getPrimaryWallet(donation.getUser());

        if (walletOpt.isEmpty()) {
            throw new WalletNotFoundException(
                    donation.getId(),
                    donation.getUser().getId(),
                    "No primary wallet found for user: " + donation.getUser().getId()
            );
        }

        return walletOpt.get();
    }

    /**
     * 플랫폼 지갑에서 사용자 지갑으로 가스비용 ETH 전송
     * 사용자 지갑이 USDC를 전송할 때 필요한 가스비를 미리 충전합니다.
     */
    private String transferGasFeeToUserWallet(UserWallet userWallet) {
        try {
            log.debug("⛽ Transferring gas fee to user wallet: {}", userWallet.getWalletAddress());

            // 플랫폼 지갑 Credentials 생성
            Credentials platformCredentials = Credentials.create(platformPrivateKey);

            // 가스비 계산: 0.001 ETH (약 $3, 여유있게 설정)
            BigDecimal gasFeeEth = BigDecimal.valueOf(0.001);

            log.info("💸 Sending {} ETH as gas fee from platform wallet {} to user wallet {}",
                    gasFeeEth,
                    platformCredentials.getAddress(),
                    userWallet.getWalletAddress());

            // ETH 전송 (Web3j Transfer 유틸리티 사용)
            TransactionReceipt receipt = Transfer.sendFunds(
                    web3j,
                    platformCredentials,
                    userWallet.getWalletAddress(),
                    gasFeeEth,
                    Convert.Unit.ETHER
            ).send();

            String txHash = receipt.getTransactionHash();
            log.info("✅ Gas fee transfer successful - TX: {}", txHash);

            return txHash;

        } catch (Exception e) {
            log.error("❌ Failed to transfer gas fee to user wallet: {}", userWallet.getWalletAddress(), e);
            throw new BlockchainNetworkException(
                    null,
                    "gas_transfer",
                    "Failed to transfer gas fee: " + e.getMessage()
            );
        }
    }

    /**
     * 플랫폼 지갑에서 사용자 지갑으로 USDC 충전 (Mint)
     *
     * Note: Platform wallet이 MockUSDC의 owner이므로 mint() 함수 사용 (금액 제한 없음)
     */
    private String chargeUSDCToUserWallet(UserWallet userWallet, BigDecimal amount) {
        try {
            log.info("💰 Charging {} USDC to user wallet: {}", amount, userWallet.getWalletAddress());

            // 플랫폼 지갑 Credentials 생성
            Credentials platformCredentials = Credentials.create(platformPrivateKey);

            // USDC 금액을 6 decimals로 변환 (1 USDC = 1,000,000)
            BigInteger usdcAmount = amount.multiply(BigDecimal.valueOf(1_000_000)).toBigInteger();

            // 플랫폼 지갑(owner)에서 사용자 지갑으로 USDC mint 호출
            // mint()는 owner 전용 함수로 금액 제한이 없음 (faucet limit 100 USDC 회피)
            String txHash = mockUSDCWrapper.mint(
                    platformCredentials,
                    userWallet.getWalletAddress(),
                    usdcAmount
            ).join(); // CompletableFuture blocking wait

            log.info("✅ USDC charging successful - TX: {}", txHash);
            return txHash;

        } catch (Exception e) {
            log.error("❌ Failed to charge USDC to user wallet: {}", userWallet.getWalletAddress(), e);
            throw new BlockchainNetworkException(
                    null,
                    "usdc_charge",
                    "Failed to charge USDC: " + e.getMessage()
            );
        }
    }

    /**
     * USDC 잔액 확인
     */
    private void checkBalance(String walletAddress, BigDecimal requiredAmount) {
        try {
            log.debug("🔍 Checking USDC balance for wallet: {}, required amount: {}", walletAddress, requiredAmount);

            if (requiredAmount == null) {
                log.error("❌ Required amount is null for wallet: {}", walletAddress);
                throw new BlockchainNetworkException(
                        null,
                        "balance_check",
                        "Required amount is null"
                );
            }

            // MockUSDC 컨트랙트에서 잔액 조회
            BigInteger usdcBalance = mockUSDCWrapper.balanceOf(walletAddress).join();

            if (usdcBalance == null) {
                log.error("❌ Balance is null for wallet: {}", walletAddress);
                throw new BlockchainNetworkException(
                        null,
                        "balance_check",
                        "Balance is null"
                );
            }

            // 6 decimals로 변환 (1 USDC = 1,000,000)
            BigDecimal balanceInUsdc = new BigDecimal(usdcBalance).divide(BigDecimal.valueOf(1_000_000));

            log.debug("✅ Wallet {} USDC balance: {}", walletAddress, balanceInUsdc);

            // USDC 잔액과 필요 금액 비교
            if (balanceInUsdc.compareTo(requiredAmount) < 0) {
                throw new InsufficientBalanceException(
                        null,
                        walletAddress,
                        String.format("Insufficient USDC balance: required %s, available %s",
                                requiredAmount, balanceInUsdc)
                );
            }

        } catch (InsufficientBalanceException e) {
            throw e;
        } catch (BlockchainNetworkException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error checking USDC balance for wallet: {}", walletAddress, e);
            throw new BlockchainNetworkException(
                    null,
                    "balance_check",
                    "Failed to check USDC balance: " + e.getMessage()
            );
        }
    }

    /**
     * HanaChainCampaign 컨트랙트를 통한 기부 (approve + donate)
     *
     * @param fromWallet 기부자 지갑
     * @param contractAddress 컨트랙트 주소
     * @param blockchainCampaignId 컨트랙트 내부의 캠페인 ID
     * @param amount 기부 금액
     * @return 기부 트랜잭션 해시
     */
    private String donateToHanaChain(UserWallet fromWallet, String contractAddress, BigInteger blockchainCampaignId, BigDecimal amount) {
        try {
            log.info("💸 Donating {} USDC from {} to contract {} (campaignId: {})",
                    amount, fromWallet.getWalletAddress(), contractAddress, blockchainCampaignId);

            // 사용자 지갑 Credentials 생성 (password 없는 배치용 메서드)
            Credentials userCredentials = walletService.getCredentials(fromWallet);

            // USDC 금액을 6 decimals로 변환 (1 USDC = 1,000,000)
            BigInteger usdcAmount = amount.multiply(BigDecimal.valueOf(1_000_000)).toBigInteger();

            // 1단계: MockUSDC approve (HanaChainCampaign 컨트랙트에 사용 승인)
            log.info("📝 Step 1: Approving USDC for HanaChainCampaign contract: {}", contractAddress);
            TransactionReceipt approveReceipt = mockUSDCWrapper.approve(
                    userCredentials,
                    contractAddress,  // Approve for the specific campaign contract
                    usdcAmount
            ).join(); // CompletableFuture blocking wait

            log.info("✅ USDC approval successful - TX: {}", approveReceipt.getTransactionHash());

            // 2단계: HanaChainCampaign.donate() 호출
            // CRITICAL: Create wrapper instance for the SPECIFIC contract address
            log.info("🎁 Step 2: Creating wrapper for contract {} and calling donate() with campaignId {}", contractAddress, blockchainCampaignId);
            HanaChainCampaignWrapper campaignInstance = new HanaChainCampaignWrapper(
                web3j,  // Use injected Web3j instance
                contractAddress
            );

            TransactionReceipt donateReceipt = campaignInstance.donate(
                    userCredentials,
                    blockchainCampaignId,  // ✅ Use actual blockchain campaign ID
                    usdcAmount
            ).join(); // CompletableFuture blocking wait

            String donateTxHash = donateReceipt.getTransactionHash();
            log.info("✅ Donation successful - TX: {}", donateTxHash);

            return donateTxHash;

        } catch (Exception e) {
            log.error("❌ Failed to donate USDC from {} to contract {} (campaignId: {})",
                    fromWallet.getWalletAddress(), contractAddress, blockchainCampaignId, e);
            throw new BlockchainNetworkException(
                    null,
                    "donation_failed",
                    "Failed to donate to HanaChain campaign: " + e.getMessage()
            );
        }
    }

    /**
     * 가스비 계산
     */
    private BigDecimal calculateGasFee() {
        // 실제로는 트랜잭션 영수증에서 사용된 가스를 가져와야 함
        // gasUsed * effectiveGasPrice
        BigInteger gasPriceWei = Convert.toWei(BigDecimal.valueOf(maxGasPriceGwei), Convert.Unit.GWEI).toBigInteger();
        BigInteger totalGasWei = gasPriceWei.multiply(BigInteger.valueOf(gasLimit));
        return Convert.fromWei(totalGasWei.toString(), Convert.Unit.ETHER);
    }
}
