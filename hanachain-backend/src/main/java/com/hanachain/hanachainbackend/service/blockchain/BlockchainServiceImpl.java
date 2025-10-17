package com.hanachain.hanachainbackend.service.blockchain;

import com.hanachain.hanachainbackend.entity.BlockchainStatus;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.exception.BlockchainException;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.util.BlockchainUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * BlockchainService의 구현체
 * Option B 방식: 비동기 캠페인 생성 및 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainServiceImpl implements BlockchainService {
    
    private final Web3j web3j;
    private final HanaChainCampaignWrapper campaignWrapper;
    private final MockUSDCWrapper usdcWrapper;
    private final CampaignRepository campaignRepository;
    
    @Value("${blockchain.wallet.private-key:}")
    private String privateKey;
    
    @Value("${blockchain.contracts.campaign-address:}")
    private String campaignContractAddress;
    
    @Value("${blockchain.gas.price-gwei:20}")
    private int defaultGasPriceGwei;
    
    @Value("${blockchain.gas.limit:300000}")
    private long defaultGasLimit;
    
    /**
     * 블록체인 서비스 초기화 시 환경 변수 설정 상태를 검증하고 로깅합니다
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("===== 블록체인 서비스 설정 검증 =====");
        
        // Private Key 검증
        if (privateKey == null || privateKey.trim().isEmpty()) {
            log.error("❌ BLOCKCHAIN_PRIVATE_KEY가 설정되지 않았습니다! 블록체인 트랜잭션을 전송할 수 없습니다.");
        } else {
            String maskedKey = privateKey.length() > 10 ? 
                privateKey.substring(0, 10) + "..." + privateKey.substring(privateKey.length() - 4) : 
                "****";
            log.info("✅ BLOCKCHAIN_PRIVATE_KEY 설정됨: {}", maskedKey);
        }
        
        // Campaign Contract Address 검증
        if (campaignContractAddress == null || campaignContractAddress.trim().isEmpty()) {
            log.error("❌ Campaign Contract Address가 설정되지 않았습니다!");
        } else {
            log.info("✅ Campaign Contract Address: {}", campaignContractAddress);
        }
        
        // Gas 설정 로깅
        log.info("✅ Gas Price: {} gwei", defaultGasPriceGwei);
        log.info("✅ Gas Limit: {}", defaultGasLimit);
        
        // 계정 정보 확인 및 로깅
        if (privateKey != null && !privateKey.trim().isEmpty()) {
            try {
                Credentials credentials = Credentials.create(privateKey);
                BlockchainUtil.logBlockchainInfo(web3j, credentials);
            } catch (Exception e) {
                log.error("❌ Private Key가 잘못되었습니다: {}", e.getMessage());
            }
        }
        
        log.info("===== 블록체인 서비스 초기화 완료 =====");
    }
    
    // =========================== 캠페인 관리 ===========================
    
    @Override
    @Async("blockchainTaskExecutor")
    public CompletableFuture<String> createCampaignAsync(
            Long campaignId,
            String beneficiaryAddress,
            BigInteger goalAmount,
            BigInteger duration,
            String title,
            String description) {
        
        log.info("Starting async campaign creation - campaignId: {}, beneficiary: {}, goal: {}", 
                campaignId, beneficiaryAddress, goalAmount);
        
        try {
            // 1. 입력값 검증
            validateCampaignCreationInput(beneficiaryAddress, goalAmount, duration, title);
            
            // 2. 자격증명 생성
            Credentials credentials = getCredentials();
            
            // 3. 캠페인 상태를 BLOCKCHAIN_PENDING으로 설정 (트랜잭션 전송 준비)
            updateCampaignBlockchainStatus(campaignId, BlockchainStatus.BLOCKCHAIN_PENDING, null);
            
            // 4. 블록체인에 캠페인 생성 트랜잭션 전송
            log.info("🚀 [BLOCKCHAIN TRANSACTION] 트랜잭션 전송 시작 - campaignId: {}", campaignId);
            return campaignWrapper.createCampaign(
                    credentials, beneficiaryAddress, goalAmount, duration, title, description)
                .thenCompose(receipt -> {
                    String txHash = receipt.getTransactionHash();
                    log.info("📤 [BLOCKCHAIN TRANSACTION] 트랜잭션 전송 성공 - campaignId: {}, txHash: {}", 
                            campaignId, txHash);
                    
                    // 4.1. 트랜잭션 전송 즉시 해시 저장 및 PROCESSING 상태로 변경
                    log.info("🔄 [BLOCKCHAIN PROCESSING] PENDING → PROCESSING 상태 변경 시작 - campaignId: {}", campaignId);
                    updateCampaignBlockchainStatus(campaignId, BlockchainStatus.BLOCKCHAIN_PROCESSING, txHash);
                    log.info("✅ [BLOCKCHAIN PROCESSING] PROCESSING 상태 변경 완료 - campaignId: {}, txHash: {}", campaignId, txHash);
                    
                    // 5. 트랜잭션 확인 대기
                    return waitForTransactionAsync(txHash, 300) // 5분 타임아웃
                        .thenApply(confirmedReceipt -> {
                            if (confirmedReceipt.isStatusOK()) {
                                // 성공: 캠페인 상태를 ACTIVE로 업데이트
                                // TODO: 블록체인에서 생성된 campaignId를 추출하여 DB에 저장
                                updateCampaignBlockchainStatus(campaignId, BlockchainStatus.ACTIVE, txHash);
                                log.info("Campaign creation completed successfully - campaignId: {}, txHash: {}", 
                                        campaignId, txHash);
                                return txHash;
                            } else {
                                // 실패: 캠페인 상태를 BLOCKCHAIN_FAILED로 업데이트
                                updateCampaignBlockchainStatus(campaignId, BlockchainStatus.BLOCKCHAIN_FAILED, txHash);
                                throw new BlockchainException(
                                    "캠페인 생성 트랜잭션이 실패했습니다",
                                    txHash,
                                    BlockchainException.BlockchainErrorType.TRANSACTION_FAILED
                                );
                            }
                        });
                })
                .exceptionally(throwable -> {
                    log.error("Campaign creation failed - campaignId: {}", campaignId, throwable);
                    // 실패 시에도 기존 트랜잭션 해시 유지 (있다면)
                    updateCampaignBlockchainStatus(campaignId, BlockchainStatus.BLOCKCHAIN_FAILED, null);
                    throw new RuntimeException(throwable);
                });
                
        } catch (Exception e) {
            log.error("Error in async campaign creation - campaignId: {}", campaignId, e);
            updateCampaignBlockchainStatus(campaignId, BlockchainStatus.BLOCKCHAIN_FAILED, null);
            return CompletableFuture.failedFuture(new BlockchainException(
                "캠페인 생성 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }
    
    @Override
    public CampaignInfo getCampaignFromBlockchain(BigInteger blockchainCampaignId) {
        log.info("Getting campaign info from blockchain - campaignId: {}", blockchainCampaignId);
        
        try {
            return campaignWrapper.getCampaign(blockchainCampaignId)
                .get(30, TimeUnit.SECONDS); // 30초 타임아웃
        } catch (Exception e) {
            log.error("Error getting campaign from blockchain - campaignId: {}", blockchainCampaignId, e);
            throw new BlockchainException(
                "블록체인에서 캠페인 정보를 조회하는 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.CONTRACT_ERROR,
                e
            );
        }
    }
    
    @Override
    @Async("blockchainTaskExecutor")
    public CompletableFuture<String> finalizeCampaignAsync(BigInteger blockchainCampaignId) {
        log.info("Starting async campaign finalization - campaignId: {}", blockchainCampaignId);
        
        try {
            Credentials credentials = getCredentials();
            
            return campaignWrapper.finalizeCampaign(credentials, blockchainCampaignId)
                .thenCompose(receipt -> {
                    String txHash = receipt.getTransactionHash();
                    log.info("Campaign finalization transaction sent - campaignId: {}, txHash: {}", 
                            blockchainCampaignId, txHash);
                    
                    return waitForTransactionAsync(txHash, 300)
                        .thenApply(confirmedReceipt -> {
                            if (confirmedReceipt.isStatusOK()) {
                                log.info("Campaign finalization completed - campaignId: {}, txHash: {}", 
                                        blockchainCampaignId, txHash);
                                return txHash;
                            } else {
                                throw new BlockchainException(
                                    "캠페인 완료 처리 트랜잭션이 실패했습니다",
                                    txHash,
                                    BlockchainException.BlockchainErrorType.TRANSACTION_FAILED
                                );
                            }
                        });
                });
                
        } catch (Exception e) {
            log.error("Error in async campaign finalization - campaignId: {}", blockchainCampaignId, e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "캠페인 완료 처리 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }
    
    // =========================== 기부 관리 ===========================
    
    @Override
    @Async("blockchainTaskExecutor")
    public CompletableFuture<String> processDonationAsync(
            BigInteger campaignId,
            String donorAddress,
            BigInteger amount) {
        
        log.info("Starting async donation processing - campaignId: {}, donor: {}, amount: {}", 
                campaignId, donorAddress, amount);
        
        try {
            // 1. 입력값 검증
            validateDonationInput(campaignId, donorAddress, amount);
            
            // 2. 기부자 자격증명 생성 (실제로는 프론트엔드에서 서명된 트랜잭션을 받아야 함)
            // 현재는 임시로 서버의 키를 사용
            Credentials credentials = getCredentials();
            
            // 3. USDC 승인 단계
            return usdcWrapper.approve(credentials, getCampaignContractAddress(), amount)
                .thenCompose(approvalReceipt -> {
                    log.info("USDC approval completed - txHash: {}", approvalReceipt.getTransactionHash());
                    
                    // 4. 기부 실행 단계
                    return campaignWrapper.donate(credentials, campaignId, amount);
                })
                .thenCompose(donationReceipt -> {
                    String txHash = donationReceipt.getTransactionHash();
                    log.info("Donation transaction sent - campaignId: {}, txHash: {}", campaignId, txHash);
                    
                    // 5. 트랜잭션 확인 대기
                    return waitForTransactionAsync(txHash, 300)
                        .thenApply(confirmedReceipt -> {
                            if (confirmedReceipt.isStatusOK()) {
                                log.info("Donation completed successfully - campaignId: {}, amount: {}, txHash: {}", 
                                        campaignId, amount, txHash);
                                return txHash;
                            } else {
                                throw new BlockchainException(
                                    "기부 트랜잭션이 실패했습니다",
                                    txHash,
                                    BlockchainException.BlockchainErrorType.TRANSACTION_FAILED
                                );
                            }
                        });
                });
                
        } catch (Exception e) {
            log.error("Error in async donation processing - campaignId: {}, donor: {}, amount: {}", 
                    campaignId, donorAddress, amount, e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "기부 처리 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }
    
    @Override
    public BigInteger getUSDCBalance(String address) {
        log.info("Getting USDC balance for address: {}", address);
        
        try {
            return usdcWrapper.balanceOf(address)
                .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error getting USDC balance for address: {}", address, e);
            throw new BlockchainException(
                "USDC 잔액 조회 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.CONTRACT_ERROR,
                e
            );
        }
    }
    
    @Override
    @Async("blockchainTaskExecutor")
    public CompletableFuture<List<DonationInfo>> getDonationHistoryAsync(
            BigInteger campaignId,
            String donorAddress) {

        log.info("Getting donation history - campaignId: {}, donor: {}", campaignId, donorAddress);

        // TODO: 블록체인 이벤트 로그를 조회하여 기부 내역을 가져오는 로직 구현
        // 현재는 기본 구조만 제공

        return CompletableFuture.supplyAsync(() -> {
            // 임시 구현 - 실제로는 이벤트 로그를 파싱해야 함
            return List.of();
        });
    }

    @Override
    public com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransactionListResponse getCampaignTransactions(
            Long campaignId,
            int limit) {

        log.info("Getting blockchain transactions for campaign - campaignId: {}, limit: {}", campaignId, limit);

        try {
            // 1. 캠페인 조회
            Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다: " + campaignId));

            // 2. 블록체인 상태 확인
            if (campaign.getBlockchainStatus() != BlockchainStatus.ACTIVE) {
                log.warn("Campaign is not active on blockchain - campaignId: {}, status: {}",
                        campaignId, campaign.getBlockchainStatus());
                return com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransactionListResponse.builder()
                    .transactions(List.of())
                    .totalCount(0)
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build();
            }

            BigInteger blockchainCampaignId = campaign.getBlockchainCampaignId();

            // 3. 블록체인 이벤트 로그 조회
            // blockchainCampaignId가 null이어도 생성 트랜잭션은 조회 가능
            List<com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction> transactions =
                    fetchBlockchainEvents(blockchainCampaignId, campaign.getBlockchainTransactionHash(), limit);

            // 4. 응답 생성
            return com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransactionListResponse.builder()
                .transactions(transactions)
                .totalCount(transactions.size())
                .lastUpdated(java.time.LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Error getting campaign transactions - campaignId: {}", campaignId, e);
            throw new BlockchainException(
                "캠페인 트랜잭션 조회 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.CONTRACT_ERROR,
                e
            );
        }
    }

    /**
     * 블록체인 이벤트 로그를 조회하여 트랜잭션 목록을 생성합니다
     */
    private List<com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction> fetchBlockchainEvents(
            BigInteger blockchainCampaignId,
            String creationTxHash,
            int limit) {

        List<com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction> transactions = new java.util.ArrayList<>();

        try {
            // 1. 캠페인 생성 트랜잭션 추가
            if (creationTxHash != null && !creationTxHash.trim().isEmpty()) {
                com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction creationTx =
                        buildTransactionFromHash(
                                creationTxHash,
                                com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction.EventType.CAMPAIGN_CREATED
                        );
                if (creationTx != null) {
                    transactions.add(creationTx);
                }
            }

            // 2. TODO: 기부 이벤트 로그 조회 (DonationMade 이벤트)
            // Web3j의 ethGetLogs를 사용하여 DonationMade 이벤트 필터링
            // 현재는 mock 데이터로 대체

            // 3. 최신순 정렬 및 limit 적용
            transactions.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
            if (transactions.size() > limit) {
                transactions = transactions.subList(0, limit);
            }

        } catch (Exception e) {
            log.error("Error fetching blockchain events - campaignId: {}", blockchainCampaignId, e);
        }

        return transactions;
    }

    /**
     * 트랜잭션 해시로부터 트랜잭션 정보를 조회합니다
     */
    private com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction buildTransactionFromHash(
            String txHash,
            com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction.EventType eventType) {

        try {
            // Web3j로 트랜잭션 정보 조회
            var ethTransaction = web3j.ethGetTransactionByHash(txHash).send();
            if (!ethTransaction.getTransaction().isPresent()) {
                return null;
            }

            var transaction = ethTransaction.getTransaction().get();

            // 트랜잭션 수신 조회 (블록 번호 및 타임스탬프용)
            var ethReceipt = web3j.ethGetTransactionReceipt(txHash).send();
            if (!ethReceipt.getTransactionReceipt().isPresent()) {
                return null;
            }

            var receipt = ethReceipt.getTransactionReceipt().get();

            // 블록 타임스탬프 조회
            var ethBlock = web3j.ethGetBlockByNumber(
                    org.web3j.protocol.core.DefaultBlockParameter.valueOf(receipt.getBlockNumber()),
                    false
            ).send();

            java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
            if (ethBlock.getBlock() != null) {
                long blockTimestamp = ethBlock.getBlock().getTimestamp().longValue();
                timestamp = java.time.LocalDateTime.ofEpochSecond(blockTimestamp, 0, java.time.ZoneOffset.UTC);
            }

            // DTO 생성
            return com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction.builder()
                .transactionHash(txHash)
                .blockNumber(receipt.getBlockNumber().toString())
                .timestamp(timestamp)
                .from(transaction.getFrom())
                .to(transaction.getTo())
                .value("0") // 기본값 (USDC 금액은 이벤트 로그에서 파싱 필요)
                .eventType(eventType)
                .build();

        } catch (Exception e) {
            log.error("Error building transaction from hash: {}", txHash, e);
            return null;
        }
    }

    // =========================== 유틸리티 ===========================
    
    @Override
    public TransactionStatus getTransactionStatus(String transactionHash) {
        log.info("Getting transaction status for hash: {}", transactionHash);
        
        try {
            var ethGetTransactionReceipt = web3j.ethGetTransactionReceipt(transactionHash).send();
            
            if (ethGetTransactionReceipt.hasError()) {
                return new TransactionStatus(
                    transactionHash, false, false, null, null, 
                    ethGetTransactionReceipt.getError().getMessage()
                );
            }
            
            var receiptOptional = ethGetTransactionReceipt.getTransactionReceipt();
            if (receiptOptional.isPresent()) {
                var receipt = receiptOptional.get();
                boolean successful = receipt.isStatusOK();
                
                return new TransactionStatus(
                    transactionHash, true, successful,
                    receipt.getBlockNumber(), receipt.getGasUsed(), null
                );
            } else {
                // 트랜잭션이 아직 마이닝되지 않음
                return new TransactionStatus(
                    transactionHash, false, false, null, null, "Transaction pending"
                );
            }
            
        } catch (Exception e) {
            log.error("Error getting transaction status for hash: {}", transactionHash, e);
            return new TransactionStatus(
                transactionHash, false, false, null, null, 
                "Error checking status: " + e.getMessage()
            );
        }
    }
    
    @Override
    public BigInteger estimateGasPrice() {
        try {
            EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
            if (ethGasPrice.hasError()) {
                log.warn("Failed to get gas price, using default: {} gwei", defaultGasPriceGwei);
                return BigInteger.valueOf(defaultGasPriceGwei).multiply(BigInteger.valueOf(1_000_000_000)); // gwei to wei
            }
            
            BigInteger gasPrice = ethGasPrice.getGasPrice();
            log.info("Current gas price: {} wei", gasPrice);
            return gasPrice;
            
        } catch (Exception e) {
            log.error("Error estimating gas price", e);
            BigInteger defaultPrice = BigInteger.valueOf(defaultGasPriceGwei).multiply(BigInteger.valueOf(1_000_000_000));
            log.warn("Using default gas price: {} wei", defaultPrice);
            return defaultPrice;
        }
    }
    
    @Override
    public CompletableFuture<TransactionReceipt> waitForTransactionAsync(String transactionHash, int timeoutSeconds) {
        log.info("Waiting for transaction confirmation - hash: {}, timeout: {}s", transactionHash, timeoutSeconds);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 트랜잭션 수신 대기 (간단한 폴링 구현)
                int attempts = 0;
                int maxAttempts = timeoutSeconds * 2; // 0.5초마다 폴링
                
                while (attempts < maxAttempts) {
                    try {
                        org.web3j.protocol.core.methods.response.EthGetTransactionReceipt response = 
                            web3j.ethGetTransactionReceipt(transactionHash).send();
                        
                        if (response.getTransactionReceipt().isPresent()) {
                            return response.getTransactionReceipt().get();
                        }
                        
                        Thread.sleep(500); // 0.5초 대기
                        attempts++;
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new BlockchainException(
                            "트랜잭션 수신 대기가 중단되었습니다",
                            BlockchainException.BlockchainErrorType.TIMEOUT,
                            e
                        );
                    } catch (Exception e) {
                        log.debug("트랜잭션 수신 조회 중 일시적 오류 - attempt {}/{}", attempts + 1, maxAttempts, e);
                        Thread.sleep(500);
                        attempts++;
                    }
                }
                
                // 타임아웃 발생
                throw new BlockchainException(
                    "트랜잭션 수신 대기가 타임아웃되었습니다: " + transactionHash,
                    BlockchainException.BlockchainErrorType.TIMEOUT
                );
                
            } catch (Exception e) {
                log.error("Error waiting for transaction confirmation - hash: {}", transactionHash, e);
                throw new BlockchainException(
                    "트랜잭션 확인 대기 중 오류가 발생했습니다: " + e.getMessage(),
                    transactionHash,
                    BlockchainException.BlockchainErrorType.TIMEOUT,
                    e
                );
            }
        });
    }
    
    // =========================== 내부 유틸리티 메서드 ===========================
    
    /**
     * 블록체인 트랜잭션용 자격증명을 가져옵니다
     */
    private Credentials getCredentials() {
        if (privateKey == null || privateKey.trim().isEmpty()) {
            throw new BlockchainException(
                "블록체인 private key가 설정되지 않았습니다",
                BlockchainException.BlockchainErrorType.GENERAL
            );
        }
        
        try {
            return Credentials.create(privateKey);
        } catch (Exception e) {
            throw new BlockchainException(
                "유효하지 않은 private key입니다",
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            );
        }
    }
    
    /**
     * 캠페인 생성 입력값을 검증합니다
     */
    private void validateCampaignCreationInput(String beneficiaryAddress, BigInteger goalAmount, 
                                             BigInteger duration, String title) {
        if (beneficiaryAddress == null || beneficiaryAddress.trim().isEmpty()) {
            throw new BlockchainException(
                "수혜자 주소가 입력되지 않았습니다",
                BlockchainException.BlockchainErrorType.INVALID_ADDRESS
            );
        }
        
        if (goalAmount == null || goalAmount.compareTo(BigInteger.ZERO) <= 0) {
            throw new BlockchainException(
                "목표 금액은 0보다 커야 합니다",
                BlockchainException.BlockchainErrorType.INVALID_AMOUNT
            );
        }
        
        if (duration == null || duration.compareTo(BigInteger.ZERO) <= 0) {
            throw new BlockchainException(
                "캠페인 기간은 0보다 커야 합니다",
                BlockchainException.BlockchainErrorType.GENERAL
            );
        }
        
        if (title == null || title.trim().isEmpty()) {
            throw new BlockchainException(
                "캠페인 제목이 입력되지 않았습니다",
                BlockchainException.BlockchainErrorType.GENERAL
            );
        }
    }
    
    /**
     * 기부 입력값을 검증합니다
     */
    private void validateDonationInput(BigInteger campaignId, String donorAddress, BigInteger amount) {
        if (campaignId == null || campaignId.compareTo(BigInteger.ZERO) <= 0) {
            throw new BlockchainException(
                "유효하지 않은 캠페인 ID입니다",
                BlockchainException.BlockchainErrorType.GENERAL
            );
        }
        
        if (donorAddress == null || donorAddress.trim().isEmpty()) {
            throw new BlockchainException(
                "기부자 주소가 입력되지 않았습니다",
                BlockchainException.BlockchainErrorType.INVALID_ADDRESS
            );
        }
        
        if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0) {
            throw new BlockchainException(
                "기부 금액은 0보다 커야 합니다",
                BlockchainException.BlockchainErrorType.INVALID_AMOUNT
            );
        }
    }
    
    /**
     * 캠페인의 블록체인 상태를 업데이트합니다 (새 트랜잭션에서 실행하여 격리 문제 해결)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateCampaignBlockchainStatus(Long campaignId, BlockchainStatus status, String transactionHash) {
        log.info("🔄 [BLOCKCHAIN STATUS UPDATE] campaignId: {}, status: {} → {}, txHash: {}", 
                campaignId, "이전상태조회중", status, transactionHash);
        
        try {
            // 새로운 트랜잭션에서 캠페인 조회 (REQUIRES_NEW로 격리 문제 해결)
            Campaign campaign = campaignRepository.findById(campaignId)
                .orElseGet(() -> {
                    log.warn("⚠️ [BLOCKCHAIN STATUS] 캠페인 조회 실패, 재시도 - campaignId: {}", campaignId);
                    // 약간의 지연 후 재시도 (격리 문제 해결용)
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return campaignRepository.findById(campaignId)
                        .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다: " + campaignId));
                });
            
            BlockchainStatus previousStatus = campaign.getBlockchainStatus();
            String previousTxHash = campaign.getBlockchainTransactionHash();
            
            log.info("🔍 [BLOCKCHAIN STATUS] 현재 상태 - campaignId: {}, 이전상태: {}, 이전해시: {}", 
                    campaignId, previousStatus, previousTxHash);
            
            // 블록체인 상태 업데이트
            campaign.updateBlockchainStatus(status, null);
            
            // 트랜잭션 해시 처리: 새 해시가 있으면 설정, null이면 기존 값 유지
            if (transactionHash != null) {
                campaign.setBlockchainTransactionHash(transactionHash);
                log.info("🔗 [TRANSACTION HASH] 트랜잭션 해시 업데이트 - campaignId: {}, newHash: {}, previousHash: {}", 
                        campaignId, transactionHash, previousTxHash);
            } else {
                log.debug("📝 [TRANSACTION HASH] 해시 미제공, 기존 값 유지 - campaignId: {}, existingHash: {}", 
                        campaignId, previousTxHash);
            }
            
            // 성공 시 블록체인 ID 추출 및 저장 (향후 구현)
            if (status == BlockchainStatus.ACTIVE && transactionHash != null) {
                log.info("🎯 [BLOCKCHAIN ACTIVE] 캠페인 활성화 완료 - campaignId: {}, txHash: {}", 
                        campaignId, transactionHash);
                // TODO: 트랜잭션 로그에서 실제 블록체인 캠페인 ID 추출
                // campaign.setBlockchainCampaignId(extractedBlockchainId);
            }
            
            campaignRepository.save(campaign);
            log.info("✅ [BLOCKCHAIN STATUS] 상태 업데이트 완료 - campaignId: {}, {} → {}, txHash: {}", 
                    campaignId, previousStatus, status, campaign.getBlockchainTransactionHash());
            
        } catch (Exception e) {
            log.error("❌ [BLOCKCHAIN STATUS] 상태 업데이트 실패 - campaignId: {}", campaignId, e);
            throw new RuntimeException("블록체인 상태 업데이트 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 캠페인 컨트랙트 주소를 반환합니다
     */
    private String getCampaignContractAddress() {
        return campaignContractAddress;
    }
}