package com.hanachain.hanachainbackend.service.blockchain;

import com.hanachain.hanachainbackend.entity.BlockchainStatus;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.Donation;
import com.hanachain.hanachainbackend.exception.BlockchainException;
import com.hanachain.hanachainbackend.repository.CampaignRepository;
import com.hanachain.hanachainbackend.repository.DonationRepository;
import com.hanachain.hanachainbackend.util.BlockchainUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
    private final DonationRepository donationRepository;

    // 이벤트 시그니처 상수 (이벤트 토픽 계산에 사용)
    private static final String EVENT_CAMPAIGN_CREATED = "CampaignCreated(uint256,address,uint256,uint256,string)";
    private static final String EVENT_DONATION_MADE = "DonationMade(uint256,address,uint256)";
    private static final String EVENT_CAMPAIGN_FINALIZED = "CampaignFinalized(uint256,uint256,uint256,uint256)";
    private static final String EVENT_CAMPAIGN_CANCELLED = "CampaignCancelled(uint256,uint256)";
    
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
                                log.info("✅ [TRANSACTION CONFIRMED] 트랜잭션 성공 확인 - txHash: {}", txHash);

                                // 트랜잭션 receipt에서 컨트랙트 주소 및 campaignId 추출
                                ExtractedCampaignInfo campaignInfo = extractCampaignInfo(confirmedReceipt);

                                if (campaignInfo != null) {
                                    // 성공: 캠페인 상태를 ACTIVE로 + 컨트랙트 주소 및 campaignId 저장
                                    updateCampaignBlockchainStatusWithAddress(
                                        campaignId,
                                        BlockchainStatus.ACTIVE,
                                        txHash,
                                        campaignInfo.getContractAddress(),
                                        campaignInfo.getCampaignId()
                                    );

                                    log.info("✅ [CAMPAIGN CREATED] 캠페인 생성 완료 - campaignId: {}, txHash: {}, contractAddress: {}, blockchainCampaignId: {}",
                                            campaignId, txHash, campaignInfo.getContractAddress(), campaignInfo.getCampaignId());
                                } else {
                                    // 컨트랙트 주소 추출 실패
                                    log.error("❌ [CAMPAIGN CREATED] 캠페인 정보 추출 실패 - campaignId: {}, txHash: {}",
                                            campaignId, txHash);

                                    // 상태는 ACTIVE로 변경하되 컨트랙트 주소는 NULL
                                    updateCampaignBlockchainStatus(campaignId, BlockchainStatus.ACTIVE, txHash);
                                }

                                return txHash;
                            } else {
                                // 실패: 캠페인 상태를 BLOCKCHAIN_FAILED로 업데이트
                                log.error("❌ [TRANSACTION FAILED] 트랜잭션 실패 - txHash: {}", txHash);
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
    public CampaignInfo getCampaignFromBlockchain(String contractAddress) {
        log.info("Getting campaign info from blockchain - contractAddress: {}", contractAddress);

        try {
            // Create a new wrapper instance for the specific contract address
            HanaChainCampaignWrapper campaignInstance = new HanaChainCampaignWrapper(web3j, contractAddress);

            // Each HanaChainCampaign contract uses campaignId=1
            return campaignInstance.getCampaign(BigInteger.ONE)
                .get(30, TimeUnit.SECONDS); // 30초 타임아웃
        } catch (Exception e) {
            log.error("Error getting campaign from blockchain - contractAddress: {}", contractAddress, e);
            throw new BlockchainException(
                "블록체인에서 캠페인 정보를 조회하는 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.CONTRACT_ERROR,
                e
            );
        }
    }
    
    @Override
    @Async("blockchainTaskExecutor")
    public CompletableFuture<String> finalizeCampaignAsync(String contractAddress) {
        log.info("Starting async campaign finalization - contractAddress: {}", contractAddress);

        try {
            Credentials credentials = getCredentials();

            // Create a new wrapper instance for the specific contract address
            HanaChainCampaignWrapper campaignInstance = new HanaChainCampaignWrapper(web3j, contractAddress);

            // Each HanaChainCampaign contract uses campaignId=1
            return campaignInstance.finalizeCampaign(credentials, BigInteger.ONE)
                .thenCompose(receipt -> {
                    String txHash = receipt.getTransactionHash();
                    log.info("Campaign finalization transaction sent - contractAddress: {}, txHash: {}",
                            contractAddress, txHash);

                    return waitForTransactionAsync(txHash, 300)
                        .thenApply(confirmedReceipt -> {
                            if (confirmedReceipt.isStatusOK()) {
                                log.info("Campaign finalization completed - contractAddress: {}, txHash: {}",
                                        contractAddress, txHash);
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
            log.error("Error in async campaign finalization - contractAddress: {}", contractAddress, e);
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

            String contractAddress = campaign.getBlockchainContractAddress();

            // 컨트랙트 주소가 없으면 생성 트랜잭션만 조회
            if (contractAddress == null || contractAddress.trim().isEmpty()) {
                log.warn("No contract address for campaign - campaignId: {}", campaignId);
                // 생성 트랜잭션만 반환
                if (campaign.getBlockchainTransactionHash() != null) {
                    List<com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction> transactions =
                        fetchBlockchainEvents(null, campaign.getBlockchainTransactionHash(), limit);

                    return com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransactionListResponse.builder()
                        .transactions(transactions)
                        .totalCount(transactions.size())
                        .lastUpdated(java.time.LocalDateTime.now())
                        .build();
                }
                return com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransactionListResponse.builder()
                    .transactions(List.of())
                    .totalCount(0)
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build();
            }

            // 3. 블록체인 이벤트 로그 조회 (컨트랙트 주소로)
            List<com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction> transactions =
                    fetchBlockchainEvents(contractAddress, campaign.getBlockchainTransactionHash(), limit);

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
            String contractAddress,  // Changed from BigInteger - now accepts contract address as String
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

            // 2. 모든 블록체인 이벤트 로그 조회
            try {
                org.web3j.protocol.core.methods.response.EthLog ethLog =
                        campaignWrapper.getCampaignEvents(
                            BigInteger.ZERO, // fromBlock (genesis)
                            BigInteger.valueOf(99999999) // toBlock (latest)
                        ).get(30, TimeUnit.SECONDS);

                // 3. 이벤트 로그 파싱 및 변환
                for (org.web3j.protocol.core.methods.response.EthLog.LogResult<?> logResult : ethLog.getLogs()) {
                    if (logResult instanceof org.web3j.protocol.core.methods.response.EthLog.LogObject) {
                        Log log = ((org.web3j.protocol.core.methods.response.EthLog.LogObject) logResult).get();

                        // 이벤트 로그를 BlockchainTransaction으로 파싱
                        com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction tx = parseEventLog(log);
                        if (tx != null) {
                            transactions.add(tx);
                        }
                    }
                }

                log.info("Fetched {} blockchain events for contract {}", transactions.size(), contractAddress);

            } catch (Exception e) {
                log.error("Error fetching blockchain events - contractAddress: {}", contractAddress, e);
                // 이벤트 조회 실패는 치명적이지 않으므로 계속 진행
            }

            // 4. 최신순 정렬 및 limit 적용
            transactions.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
            if (transactions.size() > limit) {
                transactions = transactions.subList(0, limit);
            }

        } catch (Exception e) {
            log.error("Error fetching blockchain events - contractAddress: {}", contractAddress, e);
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

    /**
     * 이벤트 로그를 BlockchainTransaction DTO로 변환합니다
     */
    private com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction parseEventLog(Log eventLog) {
        try {
            // 이벤트 타입 판별 (첫 번째 토픽이 이벤트 시그니처)
            if (eventLog.getTopics().isEmpty()) {
                log.debug("Event log has no topics, skipping");
                return null;
            }

            String eventSignature = eventLog.getTopics().get(0);
            log.debug("Processing event with signature: {}, txHash: {}", eventSignature, eventLog.getTransactionHash());

            // 이벤트 시그니처 해시 계산
            String campaignCreatedHash = EventEncoder.encode(
                new org.web3j.abi.datatypes.Event(
                    "CampaignCreated",
                    Arrays.asList(
                        new TypeReference<Uint256>(true) {}, // campaignId indexed
                        new TypeReference<org.web3j.abi.datatypes.Address>(true) {}, // beneficiary indexed
                        new TypeReference<Uint256>() {}, // goalAmount
                        new TypeReference<Uint256>() {}, // deadline
                        new TypeReference<org.web3j.abi.datatypes.Utf8String>() {} // title
                    )
                )
            );

            String donationMadeHash = EventEncoder.encode(
                new org.web3j.abi.datatypes.Event(
                    "DonationMade",
                    Arrays.asList(
                        new TypeReference<Uint256>(true) {}, // campaignId indexed
                        new TypeReference<org.web3j.abi.datatypes.Address>(true) {}, // donor indexed
                        new TypeReference<Uint256>() {} // amount
                    )
                )
            );

            String campaignFinalizedHash = EventEncoder.encode(
                new org.web3j.abi.datatypes.Event(
                    "CampaignFinalized",
                    Arrays.asList(
                        new TypeReference<Uint256>(true) {}, // campaignId indexed
                        new TypeReference<Uint256>() {}, // totalRaised
                        new TypeReference<Uint256>() {}, // platformFeeAmount
                        new TypeReference<Uint256>() {} // beneficiaryAmount
                    )
                )
            );

            String campaignCancelledHash = EventEncoder.encode(
                new org.web3j.abi.datatypes.Event(
                    "CampaignCancelled",
                    Arrays.asList(
                        new TypeReference<Uint256>(true) {}, // campaignId indexed
                        new TypeReference<Uint256>() {} // totalRefunded
                    )
                )
            );

            // 이벤트 타입에 따라 파싱
            if (eventSignature.equals(donationMadeHash)) {
                log.debug("Parsing DonationMade event");
                return parseDonationMadeEvent(eventLog);
            } else if (eventSignature.equals(campaignFinalizedHash)) {
                log.debug("Parsing CampaignFinalized event");
                return parseCampaignFinalizedEvent(eventLog);
            } else if (eventSignature.equals(campaignCancelledHash)) {
                log.debug("Parsing CampaignCancelled event");
                return parseCampaignCancelledEvent(eventLog);
            } else if (eventSignature.equals(campaignCreatedHash)) {
                log.debug("Skipping CampaignCreated event (already processed)");
                // CampaignCreated는 이미 buildTransactionFromHash에서 처리됨
                return null;
            }

            log.warn("Unknown event signature: {}", eventSignature);
            return null;

        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Error parsing event log", e);
            return null;
        }
    }

    /**
     * DonationMade 이벤트 파싱
     */
    private com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction parseDonationMadeEvent(Log eventLog) {
        try {
            log.debug("Parsing DonationMade event - txHash: {}, topics count: {}",
                     eventLog.getTransactionHash(), eventLog.getTopics().size());

            // indexed 파라미터: topics[1] = campaignId, topics[2] = donor address
            String donor = eventLog.getTopics().get(2);
            // 주소 형식: 0x000...address (64자리 hex) → 실제 주소 추출
            String donorAddress = "0x" + donor.substring(donor.length() - 40);

            log.debug("Donor address extracted: {}", donorAddress);

            // non-indexed 파라미터: amount
            @SuppressWarnings({"rawtypes", "unchecked"})
            List outputParams = Arrays.asList(new TypeReference<Uint256>() {});

            List<Type> params = FunctionReturnDecoder.decode(
                eventLog.getData(),
                outputParams
            );

            BigInteger amount = (BigInteger) params.get(0).getValue();

            // 트랜잭션 해시로 기부자 이름 조회
            String donorName = getDonorNameByTxHash(eventLog.getTransactionHash());

            // 블록 타임스탬프 조회
            var ethBlock = web3j.ethGetBlockByNumber(
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(eventLog.getBlockNumber()),
                false
            ).send();

            java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
            if (ethBlock.getBlock() != null) {
                long blockTimestamp = ethBlock.getBlock().getTimestamp().longValue();
                timestamp = java.time.LocalDateTime.ofEpochSecond(blockTimestamp, 0, java.time.ZoneOffset.UTC);
            }

            // USDC는 6 decimals이므로 10^6으로 나누어 실제 금액 계산
            String usdcAmount = String.valueOf(amount.divide(BigInteger.valueOf(1_000_000)));

            return com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction.builder()
                .transactionHash(eventLog.getTransactionHash())
                .blockNumber(eventLog.getBlockNumber().toString())
                .timestamp(timestamp)
                .from(donorAddress)
                .to(eventLog.getAddress()) // 컨트랙트 주소
                .value(usdcAmount)
                .eventType(com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction.EventType.DONATION_MADE)
                .donorName(donorName)
                .build();

        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Error parsing DonationMade event", e);
            return null;
        }
    }

    /**
     * CampaignFinalized 이벤트 파싱
     */
    private com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction parseCampaignFinalizedEvent(Log eventLog) {
        try {
            // non-indexed 파라미터: totalRaised, platformFeeAmount, beneficiaryAmount
            @SuppressWarnings({"rawtypes", "unchecked"})
            List outputParams = Arrays.asList(
                new TypeReference<Uint256>() {}, // totalRaised
                new TypeReference<Uint256>() {}, // platformFeeAmount
                new TypeReference<Uint256>() {}  // beneficiaryAmount
            );

            List<Type> params = FunctionReturnDecoder.decode(
                eventLog.getData(),
                outputParams
            );

            BigInteger totalRaised = (BigInteger) params.get(0).getValue();

            // 블록 타임스탬프 조회
            var ethBlock = web3j.ethGetBlockByNumber(
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(eventLog.getBlockNumber()),
                false
            ).send();

            java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
            if (ethBlock.getBlock() != null) {
                long blockTimestamp = ethBlock.getBlock().getTimestamp().longValue();
                timestamp = java.time.LocalDateTime.ofEpochSecond(blockTimestamp, 0, java.time.ZoneOffset.UTC);
            }

            // USDC 금액 변환
            String usdcAmount = String.valueOf(totalRaised.divide(BigInteger.valueOf(1_000_000)));

            return com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction.builder()
                .transactionHash(eventLog.getTransactionHash())
                .blockNumber(eventLog.getBlockNumber().toString())
                .timestamp(timestamp)
                .from(null)
                .to(eventLog.getAddress())
                .value(usdcAmount)
                .eventType(com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction.EventType.CAMPAIGN_FINALIZED)
                .build();

        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Error parsing CampaignFinalized event", e);
            return null;
        }
    }

    /**
     * CampaignCancelled 이벤트 파싱
     */
    private com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction parseCampaignCancelledEvent(Log eventLog) {
        try {
            // non-indexed 파라미터: totalRefunded
            @SuppressWarnings({"rawtypes", "unchecked"})
            List outputParams = Arrays.asList(new TypeReference<Uint256>() {});

            List<Type> params = FunctionReturnDecoder.decode(
                eventLog.getData(),
                outputParams
            );

            BigInteger totalRefunded = (BigInteger) params.get(0).getValue();

            // 블록 타임스탬프 조회
            var ethBlock = web3j.ethGetBlockByNumber(
                org.web3j.protocol.core.DefaultBlockParameter.valueOf(eventLog.getBlockNumber()),
                false
            ).send();

            java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
            if (ethBlock.getBlock() != null) {
                long blockTimestamp = ethBlock.getBlock().getTimestamp().longValue();
                timestamp = java.time.LocalDateTime.ofEpochSecond(blockTimestamp, 0, java.time.ZoneOffset.UTC);
            }

            // USDC 금액 변환
            String usdcAmount = String.valueOf(totalRefunded.divide(BigInteger.valueOf(1_000_000)));

            return com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction.builder()
                .transactionHash(eventLog.getTransactionHash())
                .blockNumber(eventLog.getBlockNumber().toString())
                .timestamp(timestamp)
                .from(null)
                .to(eventLog.getAddress())
                .value(usdcAmount)
                .eventType(com.hanachain.hanachainbackend.dto.blockchain.BlockchainTransaction.EventType.CAMPAIGN_CANCELLED)
                .build();

        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Error parsing CampaignCancelled event", e);
            return null;
        }
    }

    /**
     * 기부 트랜잭션 해시로 기부자 이름 조회
     */
    private String getDonorNameByTxHash(String transactionHash) {
        try {
            Optional<Donation> donation = donationRepository.findByDonationTransactionHash(transactionHash);

            if (donation.isPresent()) {
                Donation d = donation.get();

                // 익명 기부인 경우
                if (Boolean.TRUE.equals(d.getAnonymous())) {
                    return "익명";
                }

                // donorName이 있으면 사용
                if (d.getDonorName() != null && !d.getDonorName().trim().isEmpty()) {
                    return d.getDonorName();
                }

                // User가 있으면 User 이름 사용
                if (d.getUser() != null && d.getUser().getName() != null) {
                    return d.getUser().getName();
                }
            }

            // 기본값
            return "익명";

        } catch (Exception e) {
            log.error("Error getting donor name by tx hash: {}", transactionHash, e);
            return "익명";
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
     * 캠페인의 블록체인 상태를 업데이트합니다
     *
     * 주의: REQUIRES_NEW 전파 - 별도의 트랜잭션에서 실행하여 롤백 방지
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

    /**
     * HanaChainCampaign의 CampaignCreated 이벤트에서 컨트랙트 주소와 campaignId 추출
     *
     * HanaChainCampaign 컨트랙트 이벤트 구조:
     * event CampaignCreated(
     *     uint256 indexed campaignId,       ← topics[1]에 저장됨
     *     address indexed beneficiary,      ← topics[2]에 저장됨
     *     uint256 goalAmount,                ← data에 저장됨
     *     uint256 deadline,                  ← data에 저장됨
     *     string title                       ← data에 저장됨
     * );
     *
     * 이벤트를 emit한 컨트랙트 주소 = 배포된 HanaChainCampaign 주소
     *
     * @return ExtractedCampaignInfo (contractAddress, campaignId) 또는 null
     */
    private ExtractedCampaignInfo extractCampaignInfo(TransactionReceipt receipt) {
        log.info("🔍 [CONTRACT EXTRACTION] 트랜잭션 receipt에서 컨트랙트 주소 및 campaignId 추출 시작");

        try {
            // 트랜잭션 receipt의 모든 로그 확인
            for (Log eventLog : receipt.getLogs()) {
                if (eventLog.getTopics().isEmpty()) {
                    continue;
                }

                // 이벤트 시그니처 (topics[0])
                String eventSignature = eventLog.getTopics().get(0);

                // HanaChainCampaign의 CampaignCreated 이벤트 시그니처
                // Signature: CampaignCreated(uint256,address,uint256,uint256,string)
                // Hash: 0x2e64da7e06e88ccd84d69eccb21119ca81e40e5648611e89edfe888c8fa62597
                String campaignCreatedHash = EventEncoder.encode(
                    new org.web3j.abi.datatypes.Event(
                        "CampaignCreated",
                        Arrays.asList(
                            new TypeReference<Uint256>(true) {},                          // campaignId (indexed)
                            new TypeReference<org.web3j.abi.datatypes.Address>(true) {}, // beneficiary (indexed)
                            new TypeReference<Uint256>() {},                              // goalAmount
                            new TypeReference<Uint256>() {},                              // deadline
                            new TypeReference<org.web3j.abi.datatypes.Utf8String>() {}   // title
                        )
                    )
                );

                // 이벤트 매칭
                if (eventSignature.equals(campaignCreatedHash)) {
                    log.info("✅ [CONTRACT EXTRACTION] CampaignCreated 이벤트 발견!");

                    // 1. 이벤트를 emit한 컨트랙트 주소 = 배포된 HanaChainCampaign 주소
                    String contractAddress = eventLog.getAddress();

                    // 2. topics[1]에서 campaignId 추출 (indexed parameter)
                    if (eventLog.getTopics().size() < 2) {
                        log.error("❌ [CONTRACT EXTRACTION] topics가 부족합니다 - size: {}", eventLog.getTopics().size());
                        continue;
                    }

                    String campaignIdHex = eventLog.getTopics().get(1);  // topics[1] = campaignId
                    BigInteger campaignId = new BigInteger(campaignIdHex.substring(2), 16);  // "0x..." → BigInteger

                    log.info("🎯 [CONTRACT EXTRACTION] 캠페인 정보 추출 성공");
                    log.info("   - contractAddress: {}", contractAddress);
                    log.info("   - campaignId: {}", campaignId);

                    return new ExtractedCampaignInfo(contractAddress, campaignId);
                }
            }

            log.error("❌ [CONTRACT EXTRACTION] CampaignCreated 이벤트를 찾을 수 없습니다");
            log.error("📋 [CONTRACT EXTRACTION] Receipt logs count: {}", receipt.getLogs().size());

            // 디버깅: 모든 이벤트 시그니처 출력
            for (Log eventLog : receipt.getLogs()) {
                if (!eventLog.getTopics().isEmpty()) {
                    log.error("📋 [CONTRACT EXTRACTION] Event signature found: {}", eventLog.getTopics().get(0));
                }
            }

            return null;

        } catch (Exception e) {
            log.error("❌ [CONTRACT EXTRACTION] 컨트랙트 주소 추출 중 오류 발생", e);
            return null;
        }
    }

    /**
     * 캠페인의 블록체인 상태 + 컨트랙트 주소 + campaignId를 동시에 업데이트
     *
     * @param campaignId 캠페인 ID (데이터베이스)
     * @param status 블록체인 상태
     * @param transactionHash 트랜잭션 해시
     * @param contractAddress 배포된 HanaChainCampaign 컨트랙트 주소 (0x... 형식)
     * @param blockchainCampaignId 컨트랙트 내부의 캠페인 ID (1, 2, 3, ...)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateCampaignBlockchainStatusWithAddress(
            Long campaignId,
            BlockchainStatus status,
            String transactionHash,
            String contractAddress,
            BigInteger blockchainCampaignId) {

        log.info("🔄 [BLOCKCHAIN UPDATE] campaignId: {}, status: {}, txHash: {}, contractAddress: {}, blockchainCampaignId: {}",
                campaignId, status, transactionHash, contractAddress, blockchainCampaignId);

        try {
            Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("캠페인을 찾을 수 없습니다: " + campaignId));

            // 1. 블록체인 상태 업데이트
            campaign.updateBlockchainStatus(status, null);

            // 2. 트랜잭션 해시 저장
            if (transactionHash != null) {
                campaign.setBlockchainTransactionHash(transactionHash);
            }

            // 3. 컨트랙트 주소 저장 (새로운 필드 - blockchainContractAddress)
            if (contractAddress != null && !contractAddress.trim().isEmpty()) {
                campaign.setBlockchainContractAddress(contractAddress);
                log.info("✅ [CONTRACT ADDRESS] 블록체인 컨트랙트 주소 저장 완료 - campaignId: {}, address: {}",
                        campaignId, contractAddress);
            } else {
                log.warn("⚠️ [CONTRACT ADDRESS] 컨트랙트 주소가 제공되지 않았습니다 - campaignId: {}", campaignId);
            }

            // 4. 블록체인 캠페인 ID 저장 (컨트랙트 내부의 캠페인 ID)
            if (blockchainCampaignId != null) {
                campaign.setBlockchainCampaignId(blockchainCampaignId);
                log.info("✅ [BLOCKCHAIN CAMPAIGN ID] 블록체인 캠페인 ID 저장 완료 - campaignId: {}, blockchainCampaignId: {}",
                        campaignId, blockchainCampaignId);
            } else {
                log.warn("⚠️ [BLOCKCHAIN CAMPAIGN ID] 블록체인 캠페인 ID가 제공되지 않았습니다 - campaignId: {}", campaignId);
            }

            campaignRepository.save(campaign);

            log.info("✅ [BLOCKCHAIN UPDATE] 업데이트 완료 - campaignId: {}, status: {}, contractAddress: {}, blockchainCampaignId: {}",
                    campaignId, status, contractAddress, blockchainCampaignId);

        } catch (Exception e) {
            log.error("❌ [BLOCKCHAIN UPDATE] 업데이트 실패 - campaignId: {}", campaignId, e);
            throw new RuntimeException("블록체인 상태 업데이트 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 블록체인 이벤트에서 추출한 캠페인 정보를 담는 내부 DTO
     * (BlockchainService.CampaignInfo와는 별개)
     */
    private static class ExtractedCampaignInfo {
        private final String contractAddress;
        private final BigInteger campaignId;

        public ExtractedCampaignInfo(String contractAddress, BigInteger campaignId) {
            this.contractAddress = contractAddress;
            this.campaignId = campaignId;
        }

        public String getContractAddress() {
            return contractAddress;
        }

        public BigInteger getCampaignId() {
            return campaignId;
        }
    }
}