package com.hanachain.hanachainbackend.util;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.utils.Convert;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;

/**
 * 블록체인 관련 유틸리티 클래스
 */
@Slf4j
public class BlockchainUtil {
    
    /**
     * Private Key로부터 계정 주소를 반환합니다
     */
    public static String getAddressFromPrivateKey(String privateKey) {
        try {
            Credentials credentials = Credentials.create(privateKey);
            return credentials.getAddress();
        } catch (Exception e) {
            log.error("Failed to get address from private key", e);
            return null;
        }
    }
    
    /**
     * 계정의 ETH 잔액을 조회합니다
     */
    public static BigInteger getBalance(Web3j web3j, String address) {
        try {
            return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                       .send()
                       .getBalance();
        } catch (Exception e) {
            log.error("Failed to get balance for address: {}", address, e);
            return BigInteger.ZERO;
        }
    }
    
    /**
     * Wei를 Ether로 변환합니다
     */
    public static String weiToEther(BigInteger wei) {
        if (wei == null) {
            log.warn("Wei value is null, returning 0");
            return "0";
        }
        return Convert.fromWei(wei.toString(), Convert.Unit.ETHER).toString();
    }
    
    /**
     * 네트워크 정보를 조회합니다
     */
    public static void logNetworkInfo(Web3j web3j) {
        try {
            // 네트워크 연결 확인
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            log.info("클라이언트 버전: {}", clientVersion);
            
            // 체인 ID 확인
            BigInteger chainId = web3j.ethChainId().send().getChainId();
            log.info("체인 ID: {} ({})", chainId, getNetworkName(chainId));
            
            // 최신 블록 번호 확인
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            log.info("최신 블록 번호: {}", blockNumber);
            
        } catch (Exception e) {
            log.error("네트워크 정보 조회 실패", e);
        }
    }
    
    /**
     * 체인 ID로부터 네트워크 이름을 반환합니다
     */
    private static String getNetworkName(BigInteger chainId) {
        switch (chainId.intValue()) {
            case 1: return "Ethereum Mainnet";
            case 11155111: return "Sepolia Testnet";
            case 1337: return "Local/Hardhat";
            case 31337: return "Hardhat Network";
            default: return "Unknown Network";
        }
    }
    
    /**
     * 계정 정보를 로그로 출력합니다
     */
    public static void logAccountInfo(Web3j web3j, Credentials credentials) {
        String address = credentials.getAddress();
        BigInteger balance = getBalance(web3j, address);
        String etherBalance = weiToEther(balance);
        
        log.info("=== 블록체인 계정 정보 ===");
        log.info("주소: {}", address);
        log.info("잔액: {} wei ({} ETH)", balance, etherBalance);
        log.info("========================");
    }
    
    /**
     * 가스 사용량을 추정합니다
     */
    public static BigInteger estimateGas(Web3j web3j, String from, String to, String data) {
        try {
            Transaction transaction = Transaction.createFunctionCallTransaction(
                from, null, null, null, to, data
            );
            
            BigInteger estimatedGas = web3j.ethEstimateGas(transaction)
                .send()
                .getAmountUsed();
            
            // 20% 여유분 추가
            BigInteger gasWithBuffer = estimatedGas.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));
            
            log.info("가스 추정 결과 - 예상: {}, 여유분 포함: {}", estimatedGas, gasWithBuffer);
            return gasWithBuffer;
            
        } catch (Exception e) {
            log.error("가스 추정 실패, 기본값 사용", e);
            return BigInteger.valueOf(800000); // 기본값
        }
    }
    
    /**
     * 현재 네트워크의 가스 가격을 조회합니다
     */
    public static BigInteger getCurrentGasPrice(Web3j web3j) {
        try {
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            log.info("현재 네트워크 가스 가격: {} wei", gasPrice);
            return gasPrice;
        } catch (Exception e) {
            log.error("가스 가격 조회 실패, 기본값 사용", e);
            return Convert.toWei("20", Convert.Unit.GWEI).toBigInteger(); // 20 gwei 기본값
        }
    }
    
    /**
     * 종합 블록체인 정보를 로그로 출력합니다
     */
    public static void logBlockchainInfo(Web3j web3j, Credentials credentials) {
        log.info("=== 블록체인 연결 정보 ===");
        logNetworkInfo(web3j);
        log.info("=== 블록체인 계정 정보 ===");
        String address = credentials.getAddress();
        BigInteger balance = getBalance(web3j, address);
        String etherBalance = weiToEther(balance);
        log.info("주소: {}", address);
        log.info("잔액: {} wei ({} ETH)", balance, etherBalance);
        
        // 현재 가스 가격 정보 추가
        BigInteger currentGasPrice = getCurrentGasPrice(web3j);
        String gasPriceGwei = Convert.fromWei(currentGasPrice.toString(), Convert.Unit.GWEI).toString();
        log.info("현재 가스 가격: {} gwei", gasPriceGwei);
        
        log.info("========================");
    }
}