package com.hanachain.hanachainbackend.service.blockchain;

import com.hanachain.hanachainbackend.exception.BlockchainException;
import com.hanachain.hanachainbackend.util.BlockchainUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MockUSDC 스마트 컨트랙트와의 상호작용을 담당하는 래퍼 클래스
 */
@Component
@Slf4j
public class MockUSDCWrapper {
    
    private final Web3j web3j;
    private final String contractAddress;
    private final DefaultGasProvider gasProvider;
    
    @Autowired
    public MockUSDCWrapper(
            Web3j web3j,
            @Qualifier("usdcContractAddress") String contractAddress) {
        this.web3j = web3j;
        this.contractAddress = contractAddress;
        this.gasProvider = new DefaultGasProvider();
        
        log.info("Initialized MockUSDCWrapper with contract address: {}", contractAddress);
    }
    
    /**
     * 특정 주소의 USDC 잔액을 조회합니다
     * 
     * @param address 조회할 지갑 주소
     * @return USDC 잔액 (6 decimals)
     */
    public CompletableFuture<BigInteger> balanceOf(String address) {
        log.info("Getting USDC balance for address: {}", address);
        
        try {
            // 함수 파라미터 정의
            List<Type> inputParameters = Collections.singletonList(
                new Address(address)
            );
            
            // 함수 출력 파라미터 정의
            List<TypeReference<?>> outputParameters = Collections.singletonList(
                new TypeReference<Uint256>() {}
            );
            
            // 함수 정의
            Function balanceOfFunction = new Function(
                "balanceOf",
                inputParameters,
                outputParameters
            );
            
            return executeCall(balanceOfFunction)
                .thenApply(result -> {
                    if (!result.isEmpty()) {
                        BigInteger balance = (BigInteger) result.get(0).getValue();
                        log.info("USDC balance for {}: {}", address, balance);
                        return balance;
                    } else {
                        throw new BlockchainException(
                            "잔액 조회 결과가 올바르지 않습니다",
                            BlockchainException.BlockchainErrorType.CONTRACT_ERROR
                        );
                    }
                })
                .whenComplete((balance, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to get USDC balance for address: {}", address, throwable);
                    }
                });
                
        } catch (Exception e) {
            log.error("Error getting USDC balance", e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "USDC 잔액 조회 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }
    
    /**
     * USDC 사용 승인을 설정합니다
     * 
     * @param credentials 트랜잭션 서명용 자격증명
     * @param spender 승인할 주소 (보통 Campaign 컨트랙트 주소)
     * @param amount 승인할 금액
     * @return 트랜잭션 수신 정보
     */
    public CompletableFuture<TransactionReceipt> approve(
            Credentials credentials,
            String spender,
            BigInteger amount) {
        
        log.info("Approving USDC - spender: {}, amount: {}", spender, amount);
        
        try {
            // 함수 파라미터 정의
            List<Type> inputParameters = Arrays.asList(
                new Address(spender),
                new Uint256(amount)
            );
            
            // 함수 출력 파라미터 정의
            List<TypeReference<?>> outputParameters = Collections.singletonList(
                new TypeReference<Bool>() {}
            );
            
            // 함수 정의
            Function approveFunction = new Function(
                "approve",
                inputParameters,
                outputParameters
            );
            
            return executeTransaction(credentials, approveFunction)
                .whenComplete((receipt, throwable) -> {
                    if (throwable != null) {
                        log.error("USDC approval failed - spender: {}, amount: {}", 
                                spender, amount, throwable);
                    } else {
                        log.info("USDC approval successful. Transaction hash: {}", 
                                receipt.getTransactionHash());
                    }
                });
                
        } catch (Exception e) {
            log.error("Error approving USDC", e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "USDC 승인 처리 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }
    
    /**
     * 승인된 USDC 금액을 조회합니다
     * 
     * @param owner 토큰 소유자 주소
     * @param spender 승인받은 주소
     * @return 승인된 금액
     */
    public CompletableFuture<BigInteger> allowance(String owner, String spender) {
        log.info("Getting USDC allowance - owner: {}, spender: {}", owner, spender);
        
        try {
            // 함수 파라미터 정의
            List<Type> inputParameters = Arrays.asList(
                new Address(owner),
                new Address(spender)
            );
            
            // 함수 출력 파라미터 정의
            List<TypeReference<?>> outputParameters = Collections.singletonList(
                new TypeReference<Uint256>() {}
            );
            
            // 함수 정의
            Function allowanceFunction = new Function(
                "allowance",
                inputParameters,
                outputParameters
            );
            
            return executeCall(allowanceFunction)
                .thenApply(result -> {
                    if (!result.isEmpty()) {
                        BigInteger allowanceAmount = (BigInteger) result.get(0).getValue();
                        log.info("USDC allowance for {} -> {}: {}", owner, spender, allowanceAmount);
                        return allowanceAmount;
                    } else {
                        throw new BlockchainException(
                            "승인 금액 조회 결과가 올바르지 않습니다",
                            BlockchainException.BlockchainErrorType.CONTRACT_ERROR
                        );
                    }
                })
                .whenComplete((allowanceAmount, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to get USDC allowance - owner: {}, spender: {}", 
                                owner, spender, throwable);
                    }
                });
                
        } catch (Exception e) {
            log.error("Error getting USDC allowance", e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "USDC 승인 금액 조회 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }
    
    /**
     * 테스트용 USDC를 요청합니다 (faucet 기능)
     *
     * @param credentials 트랜잭션 서명용 자격증명
     * @param to 받을 주소
     * @param amount 요청할 금액
     * @return 트랜잭션 해시
     */
    public CompletableFuture<String> faucet(
            Credentials credentials,
            String to,
            BigInteger amount) {

        log.info("Requesting USDC from faucet - to: {}, amount: {}", to, amount);

        try {
            // 함수 파라미터 정의
            List<Type> inputParameters = Arrays.asList(
                new Address(to),
                new Uint256(amount)
            );

            // 함수 출력 파라미터 정의 (void 함수)
            List<TypeReference<?>> outputParameters = Collections.emptyList();

            // 함수 정의
            Function faucetFunction = new Function(
                "faucet",
                inputParameters,
                outputParameters
            );

            return executeTransaction(credentials, faucetFunction)
                .thenApply(receipt -> {
                    String txHash = receipt.getTransactionHash();
                    log.info("USDC faucet request successful. Transaction hash: {}", txHash);
                    return txHash;
                })
                .whenComplete((txHash, throwable) -> {
                    if (throwable != null) {
                        log.error("USDC faucet request failed - to: {}, amount: {}",
                                to, amount, throwable);
                    }
                });

        } catch (Exception e) {
            log.error("Error requesting USDC from faucet", e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "USDC 테스트 토큰 요청 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }

    /**
     * Owner 전용 USDC 민팅 (금액 제한 없음)
     *
     * @param credentials 트랜잭션 서명용 자격증명 (owner 계정)
     * @param to 받을 주소
     * @param amount 민팅할 금액
     * @return 트랜잭션 해시
     */
    public CompletableFuture<String> mint(
            Credentials credentials,
            String to,
            BigInteger amount) {

        log.info("Minting USDC (owner) - to: {}, amount: {}", to, amount);

        try {
            // 함수 파라미터 정의
            List<Type> inputParameters = Arrays.asList(
                new Address(to),
                new Uint256(amount)
            );

            // 함수 출력 파라미터 정의 (void 함수)
            List<TypeReference<?>> outputParameters = Collections.emptyList();

            // 함수 정의
            Function mintFunction = new Function(
                "mint",
                inputParameters,
                outputParameters
            );

            return executeTransaction(credentials, mintFunction)
                .thenApply(receipt -> {
                    String txHash = receipt.getTransactionHash();
                    log.info("USDC minting successful. Transaction hash: {}", txHash);
                    return txHash;
                })
                .whenComplete((txHash, throwable) -> {
                    if (throwable != null) {
                        log.error("USDC minting failed - to: {}, amount: {}",
                                to, amount, throwable);
                    }
                });

        } catch (Exception e) {
            log.error("Error minting USDC", e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "USDC 민팅 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
    }
    
    /**
     * USDC 전송을 실행합니다
     *
     * @param credentials 트랜잭션 서명용 자격증명
     * @param to 받을 주소
     * @param amount 전송할 금액
     * @return 트랜잭션 해시
     */
    public CompletableFuture<String> transfer(
            Credentials credentials,
            String to,
            BigInteger amount) {

        log.info("Transferring USDC - to: {}, amount: {}", to, amount);

        try {
            // 함수 파라미터 정의
            List<Type> inputParameters = Arrays.asList(
                new Address(to),
                new Uint256(amount)
            );

            // 함수 출력 파라미터 정의
            List<TypeReference<?>> outputParameters = Collections.singletonList(
                new TypeReference<Bool>() {}
            );

            // 함수 정의
            Function transferFunction = new Function(
                "transfer",
                inputParameters,
                outputParameters
            );

            return executeTransaction(credentials, transferFunction)
                .thenApply(receipt -> {
                    String txHash = receipt.getTransactionHash();
                    log.info("USDC transfer successful. Transaction hash: {}", txHash);
                    return txHash;
                })
                .whenComplete((txHash, throwable) -> {
                    if (throwable != null) {
                        log.error("USDC transfer failed - to: {}, amount: {}", to, amount, throwable);
                    }
                });

        } catch (Exception e) {
            log.error("Error transferring USDC", e);
            return CompletableFuture.failedFuture(new BlockchainException(
                "USDC 전송 중 오류가 발생했습니다: " + e.getMessage(),
                BlockchainException.BlockchainErrorType.GENERAL,
                e
            ));
        }
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
                log.debug("Executing USDC transaction with encoded function: {}", encodedFunction);
                
                // RawTransactionManager 사용 (private key 서명)
                RawTransactionManager transactionManager = new RawTransactionManager(web3j, credentials);
                
                // 동적 가스 가격 및 한도 계산
                BigInteger currentGasPrice = BlockchainUtil.getCurrentGasPrice(web3j);
                BigInteger estimatedGasLimit = BlockchainUtil.estimateGas(web3j, 
                    credentials.getAddress(), contractAddress, encodedFunction);
                
                log.info("USDC 트랜잭션 가스 설정 - 가격: {} gwei, 한도: {}", 
                    org.web3j.utils.Convert.fromWei(currentGasPrice.toString(), org.web3j.utils.Convert.Unit.GWEI), 
                    estimatedGasLimit);
                
                // 트랜잭션 전송
                org.web3j.protocol.core.methods.response.EthSendTransaction response = 
                    transactionManager.sendTransaction(
                        currentGasPrice,
                        estimatedGasLimit,
                        contractAddress,
                        encodedFunction,
                        BigInteger.ZERO // value (ERC-20 토큰이므로 0)
                    );
                
                if (response.hasError()) {
                    throw new BlockchainException(
                        "USDC 트랜잭션 전송 실패: " + response.getError().getMessage(),
                        BlockchainException.BlockchainErrorType.TRANSACTION_FAILED
                    );
                }
                
                String transactionHash = response.getTransactionHash();
                log.info("USDC transaction sent successfully: {}", transactionHash);
                
                // 트랜잭션 수신 대기 (최대 60초)
                TransactionReceipt receipt = waitForTransactionReceipt(transactionHash, 60);
                
                // 수신 상태 확인
                if (receipt == null || !"0x1".equals(receipt.getStatus())) {
                    throw new BlockchainException(
                        "USDC 트랜잭션 실행 실패 - Hash: " + transactionHash + ", Status: " + 
                        (receipt != null ? receipt.getStatus() : "null"),
                        transactionHash,
                        BlockchainException.BlockchainErrorType.TRANSACTION_FAILED
                    );
                }
                
                log.info("USDC transaction executed successfully: {}", transactionHash);
                return receipt;
                
            } catch (BlockchainException e) {
                throw e;
            } catch (Exception e) {
                log.error("USDC transaction execution error", e);
                throw new BlockchainException(
                    "USDC 트랜잭션 실행 중 오류가 발생했습니다: " + e.getMessage(),
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
                    log.debug("Waiting for USDC transaction receipt... Attempt: {}/{}", attempts, maxAttempts);
                }
            }
            
            log.warn("USDC transaction receipt timeout after {} seconds for hash: {}", timeoutSeconds, transactionHash);
            return null;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("USDC transaction receipt polling interrupted", e);
            return null;
        } catch (Exception e) {
            log.error("Error while waiting for USDC transaction receipt", e);
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
                log.debug("Executing USDC call for function: {}", function.getName());
                
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
                        "USDC 함수 호출 실패: " + ethCall.getError().getMessage(),
                        BlockchainException.BlockchainErrorType.CONTRACT_ERROR
                    );
                }
                
                String result = ethCall.getValue();
                log.debug("USDC call result: {}", result);
                
                // 결과 디코딩
                List<Type> decodedResult = org.web3j.abi.FunctionReturnDecoder.decode(
                    result, function.getOutputParameters());
                
                log.debug("USDC decoded result size: {}", decodedResult.size());
                return decodedResult;
                
            } catch (BlockchainException e) {
                throw e;
            } catch (Exception e) {
                log.error("USDC function call error", e);
                throw new BlockchainException(
                    "USDC 함수 호출 중 오류가 발생했습니다: " + e.getMessage(),
                    BlockchainException.BlockchainErrorType.CONTRACT_ERROR,
                    e
                );
            }
        });
    }
}