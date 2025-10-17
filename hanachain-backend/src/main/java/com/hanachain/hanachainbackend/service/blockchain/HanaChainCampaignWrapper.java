package com.hanachain.hanachainbackend.service.blockchain;

import com.hanachain.hanachainbackend.exception.BlockchainException;
import com.hanachain.hanachainbackend.util.BlockchainUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * HanaChainCampaign 스마트 컨트랙트와의 상호작용을 담당하는 래퍼 클래스
 */
@Component
@Slf4j
public class HanaChainCampaignWrapper {
    
    private final Web3j web3j;
    private final String contractAddress;
    private final DefaultGasProvider gasProvider;
    
    @Autowired
    public HanaChainCampaignWrapper(
            Web3j web3j,
            @Qualifier("campaignContractAddress") String contractAddress) {
        this.web3j = web3j;
        this.contractAddress = contractAddress;
        this.gasProvider = new DefaultGasProvider();
        
        log.info("Initialized HanaChainCampaignWrapper with contract address: {}", contractAddress);
    }
    
    /**
     * 새로운 캠페인을 생성합니다
     * 
     * @param credentials 트랜잭션 서명용 자격증명
     * @param beneficiary 수혜자 주소
     * @param goalAmount 목표 금액
     * @param duration 캠페인 기간 (초)
     * @param title 캠페인 제목
     * @param description 캠페인 설명
     * @return 트랜잭션 수신 정보
     */
    public CompletableFuture<TransactionReceipt> createCampaign(
            Credentials credentials,
            String beneficiary,
            BigInteger goalAmount,
            BigInteger duration,
            String title,
            String description) {
        
        log.info("Creating campaign - beneficiary: {}, goalAmount: {}, duration: {}, title: {}",
                beneficiary, goalAmount, duration, title);
        
        try {
            // 함수 파라미터 정의
            List<Type> inputParameters = Arrays.asList(
                new Address(beneficiary),
                new Uint256(goalAmount),
                new Uint256(duration),
                new Utf8String(title),
                new Utf8String(description)
            );
            
            // 함수 출력 파라미터 정의
            List<TypeReference<?>> outputParameters = Arrays.asList(
                new TypeReference<Uint256>() {} // campaignId
            );
            
            // 함수 정의
            Function createCampaignFunction = new Function(
                "createCampaign",
                inputParameters,
                outputParameters
            );
            
            // 트랜잭션 실행
            return executeTransaction(credentials, createCampaignFunction)
                .whenComplete((receipt, throwable) -> {
                    if (throwable != null) {
                        log.error("Campaign creation failed", throwable);
                        throw new BlockchainException(
                            "캠페인 생성 중 오류가 발생했습니다: " + throwable.getMessage(),
                            BlockchainException.BlockchainErrorType.TRANSACTION_FAILED,
                            throwable
                        );
                    } else {
                        log.info("Campaign created successfully. Transaction hash: {}", 
                                receipt.getTransactionHash());
                    }
                });
                
        } catch (Exception e) {
            log.error("Error creating campaign", e);
            throw new BlockchainException(
                "캠페인 생성 준비 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            );
        }
    }
    
    /**
     * 캠페인 정보를 조회합니다
     * 
     * @param campaignId 캠페인 ID
     * @return 캠페인 정보
     */
    public CompletableFuture<BlockchainService.CampaignInfo> getCampaign(BigInteger campaignId) {
        log.info("Getting campaign info for ID: {}", campaignId);
        
        try {
            // 함수 파라미터 정의
            List<Type> inputParameters = Collections.singletonList(
                new Uint256(campaignId)
            );
            
            // 함수 출력 파라미터 정의 (Campaign struct)
            List<TypeReference<?>> outputParameters = Arrays.asList(
                new TypeReference<Uint256>() {},  // id
                new TypeReference<Address>() {},   // beneficiary
                new TypeReference<Uint256>() {},  // goalAmount
                new TypeReference<Uint256>() {},  // totalRaised
                new TypeReference<Uint256>() {},  // deadline
                new TypeReference<Bool>() {},     // finalized
                new TypeReference<Bool>() {},     // exists
                new TypeReference<Utf8String>() {}, // title
                new TypeReference<Utf8String>() {}  // description
            );
            
            // 함수 정의
            Function getCampaignFunction = new Function(
                "getCampaign",
                inputParameters,
                outputParameters
            );
            
            return executeCall(getCampaignFunction)
                .thenApply(result -> {
                    if (result.size() >= 9) {
                        return new BlockchainService.CampaignInfo(
                            (BigInteger) result.get(0).getValue(),  // id
                            result.get(1).getValue().toString(),    // beneficiary
                            (BigInteger) result.get(2).getValue(),  // goalAmount
                            (BigInteger) result.get(3).getValue(),  // totalRaised
                            (BigInteger) result.get(4).getValue(),  // deadline
                            (Boolean) result.get(5).getValue(),     // finalized
                            (Boolean) result.get(6).getValue(),     // exists
                            result.get(7).getValue().toString(),    // title
                            result.get(8).getValue().toString()     // description
                        );
                    } else {
                        throw new BlockchainException(
                            "캠페인 정보를 올바르게 조회할 수 없습니다",
                            BlockchainException.BlockchainErrorType.CONTRACT_ERROR
                        );
                    }
                })
                .whenComplete((campaignInfo, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to get campaign info for ID: {}", campaignId, throwable);
                    } else {
                        log.info("Successfully retrieved campaign info for ID: {}", campaignId);
                    }
                });
                
        } catch (Exception e) {
            log.error("Error getting campaign info", e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "캠페인 정보 조회 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }
    
    /**
     * 캠페인에 기부합니다
     * 
     * @param credentials 트랜잭션 서명용 자격증명
     * @param campaignId 캠페인 ID
     * @param amount 기부 금액
     * @return 트랜잭션 수신 정보
     */
    public CompletableFuture<TransactionReceipt> donate(
            Credentials credentials,
            BigInteger campaignId,
            BigInteger amount) {
        
        log.info("Making donation - campaignId: {}, amount: {}", campaignId, amount);
        
        try {
            // 함수 파라미터 정의
            List<Type> inputParameters = Arrays.asList(
                new Uint256(campaignId),
                new Uint256(amount)
            );
            
            // 함수 출력 파라미터 정의 (void 함수)
            List<TypeReference<?>> outputParameters = Collections.emptyList();
            
            // 함수 정의
            Function donateFunction = new Function(
                "donate",
                inputParameters,
                outputParameters
            );
            
            // 트랜잭션 실행
            return executeTransaction(credentials, donateFunction)
                .whenComplete((receipt, throwable) -> {
                    if (throwable != null) {
                        log.error("Donation failed - campaignId: {}, amount: {}", 
                                campaignId, amount, throwable);
                    } else {
                        log.info("Donation successful. Transaction hash: {}", 
                                receipt.getTransactionHash());
                    }
                });
                
        } catch (Exception e) {
            log.error("Error making donation", e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "기부 처리 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }
    
    /**
     * 캠페인을 완료 처리합니다
     *
     * @param credentials 트랜잭션 서명용 자격증명
     * @param campaignId 캠페인 ID
     * @return 트랜잭션 수신 정보
     */
    public CompletableFuture<TransactionReceipt> finalizeCampaign(
            Credentials credentials,
            BigInteger campaignId) {

        log.info("Finalizing campaign - campaignId: {}", campaignId);

        try {
            // 함수 파라미터 정의
            List<Type> inputParameters = Collections.singletonList(
                new Uint256(campaignId)
            );

            // 함수 출력 파라미터 정의 (void 함수)
            List<TypeReference<?>> outputParameters = Collections.emptyList();

            // 함수 정의
            Function finalizeFunction = new Function(
                "finalizeCampaign",
                inputParameters,
                outputParameters
            );

            // 트랜잭션 실행
            return executeTransaction(credentials, finalizeFunction)
                .whenComplete((receipt, throwable) -> {
                    if (throwable != null) {
                        log.error("Campaign finalization failed - campaignId: {}", campaignId, throwable);
                    } else {
                        log.info("Campaign finalized successfully. Transaction hash: {}",
                                receipt.getTransactionHash());
                    }
                });

        } catch (Exception e) {
            log.error("Error finalizing campaign", e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "캠페인 완료 처리 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }

    /**
     * 캠페인의 모든 이벤트 로그를 조회합니다
     *
     * @param fromBlock 시작 블록 번호
     * @param toBlock 종료 블록 번호
     * @return 이벤트 로그 리스트
     */
    public CompletableFuture<org.web3j.protocol.core.methods.response.EthLog> getCampaignEvents(
            BigInteger fromBlock,
            BigInteger toBlock) {

        log.info("Getting campaign events - fromBlock: {}, toBlock: {}", fromBlock, toBlock);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 이벤트 필터 생성
                org.web3j.protocol.core.methods.request.EthFilter filter =
                    new org.web3j.protocol.core.methods.request.EthFilter(
                        org.web3j.protocol.core.DefaultBlockParameter.valueOf(fromBlock),
                        org.web3j.protocol.core.DefaultBlockParameter.valueOf(toBlock),
                        contractAddress
                    );

                // 모든 캠페인 관련 이벤트 조회
                // 이벤트 시그니처를 지정하지 않으면 모든 이벤트가 조회됨

                // 이벤트 로그 조회
                org.web3j.protocol.core.methods.response.EthLog ethLog =
                    web3j.ethGetLogs(filter).send();

                if (ethLog.hasError()) {
                    throw new BlockchainException(
                        "이벤트 로그 조회 실패: " + ethLog.getError().getMessage(),
                        BlockchainException.BlockchainErrorType.CONTRACT_ERROR
                    );
                }

                log.info("Campaign events retrieved successfully - count: {}",
                        ethLog.getLogs().size());
                return ethLog;

            } catch (Exception e) {
                log.error("Error getting campaign events", e);
                throw new BlockchainException(
                    "이벤트 로그 조회 중 오류가 발생했습니다: " + e.getMessage(),
                    BlockchainException.BlockchainErrorType.CONTRACT_ERROR,
                    e
                );
            }
        });
    }
    
    // =========================== 내부 유틸리티 메서드 ===========================
    
    /**
     * 트랜잭션을 실행합니다
     */
    private CompletableFuture<TransactionReceipt> executeTransaction(
            Credentials credentials, Function function) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedFunction = FunctionEncoder.encode(function);
                log.debug("Executing transaction with encoded function: {}", encodedFunction);
                
                // RawTransactionManager 사용 (private key 서명)
                RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials);
                
                // 동적 가스 가격 및 한도 계산
                BigInteger currentGasPrice = BlockchainUtil.getCurrentGasPrice(web3j);
                BigInteger estimatedGasLimit = BlockchainUtil.estimateGas(web3j, 
                    credentials.getAddress(), contractAddress, encodedFunction);
                
                log.info("Campaign 트랜잭션 가스 설정 - 가격: {} gwei, 한도: {}", 
                    org.web3j.utils.Convert.fromWei(currentGasPrice.toString(), org.web3j.utils.Convert.Unit.GWEI), 
                    estimatedGasLimit);
                
                // 트랜잭션 전송
                org.web3j.protocol.core.methods.response.EthSendTransaction response = 
                    transactionManager.sendTransaction(
                        currentGasPrice,
                        estimatedGasLimit,
                        contractAddress,
                        encodedFunction,
                        BigInteger.ZERO // value (컨트랙트 호출이므로 0)
                    );
                
                if (response.hasError()) {
                    throw new BlockchainException(
                        "트랜잭션 전송 실패: " + response.getError().getMessage(),
                        BlockchainException.BlockchainErrorType.TRANSACTION_FAILED
                    );
                }
                
                String transactionHash = response.getTransactionHash();
                log.info("Transaction sent successfully: {}", transactionHash);
                
                // 트랜잭션 수신 대기 (최대 60초)
                TransactionReceipt receipt = waitForTransactionReceipt(transactionHash, 60);
                
                // 수신 상태 확인
                if (receipt == null || !"0x1".equals(receipt.getStatus())) {
                    throw new BlockchainException(
                        "트랜잭션 실행 실패 - Hash: " + transactionHash + ", Status: " + 
                        (receipt != null ? receipt.getStatus() : "null"),
                        transactionHash,
                        BlockchainException.BlockchainErrorType.TRANSACTION_FAILED
                    );
                }
                
                log.info("Transaction executed successfully: {}", transactionHash);
                return receipt;
                
            } catch (BlockchainException e) {
                throw e;
            } catch (Exception e) {
                log.error("Transaction execution error", e);
                throw new BlockchainException(
                    "트랜잭션 실행 중 오류가 발생했습니다: " + e.getMessage(),
                    BlockchainException.BlockchainErrorType.TRANSACTION_FAILED,
                    e
                );
            }
        });
    }
    
    /**
     * 트랜잭션 수신을 폴링으로 대기합니다
     */
    private TransactionReceipt waitForTransactionReceipt(String transactionHash, int timeoutSeconds) {
        try {
            int attempts = 0;
            int maxAttempts = timeoutSeconds * 2; // 0.5초 간격으로 폴링
            
            while (attempts < maxAttempts) {
                TransactionReceipt receipt = web3j
                    .ethGetTransactionReceipt(transactionHash)
                    .send()
                    .getTransactionReceipt()
                    .orElse(null);
                
                if (receipt != null) {
                    return receipt;
                }
                
                Thread.sleep(500); // 0.5초 대기
                attempts++;
                
                if (attempts % 10 == 0) {
                    log.debug("Waiting for transaction receipt... Attempt: {}/{}", attempts, maxAttempts);
                }
            }
            
            log.warn("Transaction receipt timeout after {} seconds for hash: {}", timeoutSeconds, transactionHash);
            return null;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Transaction receipt polling interrupted", e);
            return null;
        } catch (Exception e) {
            log.error("Error while waiting for transaction receipt", e);
            return null;
        }
    }
    
    /**
     * 읽기 전용 함수 호출을 실행합니다
     */
    private CompletableFuture<List<Type>> executeCall(Function function) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedFunction = FunctionEncoder.encode(function);
                log.debug("Executing call for function: {}", function.getName());
                
                // 읽기 전용 호출 생성
                org.web3j.protocol.core.methods.request.Transaction transaction = 
                    org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                        null, // from (null for read-only calls)
                        contractAddress,
                        encodedFunction
                    );
                
                // 함수 호출 실행
                org.web3j.protocol.core.methods.response.EthCall ethCall = 
                    web3j.ethCall(transaction, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();
                
                if (ethCall.hasError()) {
                    throw new BlockchainException(
                        "함수 호출 실패: " + ethCall.getError().getMessage(),
                        BlockchainException.BlockchainErrorType.CONTRACT_ERROR
                    );
                }
                
                String result = ethCall.getValue();
                log.debug("Call result: {}", result);
                
                // 결과 디코딩
                List<Type> decodedResult = org.web3j.abi.FunctionReturnDecoder.decode(
                    result, function.getOutputParameters());
                
                log.debug("Decoded result size: {}", decodedResult.size());
                return decodedResult;
                
            } catch (BlockchainException e) {
                throw e;
            } catch (Exception e) {
                log.error("Function call error", e);
                throw new BlockchainException(
                    "함수 호출 중 오류가 발생했습니다: " + e.getMessage(),
                    BlockchainException.BlockchainErrorType.CONTRACT_ERROR,
                    e
                );
            }
        });
    }
}