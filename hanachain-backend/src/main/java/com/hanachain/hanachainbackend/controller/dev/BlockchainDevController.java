package com.hanachain.hanachainbackend.controller.dev;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.util.BlockchainUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * 개발용 블록체인 디버깅 컨트롤러
 */
@RestController
@RequestMapping("/dev/blockchain")
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class BlockchainDevController {
    
    private final Web3j web3j;
    
    @Value("${blockchain.wallet.private-key:}")
    private String privateKey;
    
    /**
     * 현재 계정 정보 조회
     */
    @GetMapping("/account-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccountInfo() {
        try {
            if (privateKey == null || privateKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("BLOCKCHAIN_PRIVATE_KEY가 설정되지 않았습니다."));
            }
            
            Credentials credentials = Credentials.create(privateKey);
            String address = credentials.getAddress();
            BigInteger balance = BlockchainUtil.getBalance(web3j, address);
            String etherBalance = BlockchainUtil.weiToEther(balance);
            
            // 네트워크 정보
            BigInteger chainId = web3j.ethChainId().send().getChainId();
            String clientVersion = web3j.web3ClientVersion().send().getWeb3ClientVersion();
            BigInteger blockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            
            Map<String, Object> result = new HashMap<>();
            result.put("address", address);
            result.put("balanceWei", balance.toString());
            result.put("balanceEther", etherBalance);
            result.put("chainId", chainId.toString());
            result.put("networkName", getNetworkName(chainId));
            result.put("clientVersion", clientVersion);
            result.put("latestBlock", blockNumber.toString());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("계정 정보 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("계정 정보 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 외부 API로 잔액 확인 (Etherscan API)
     */
    @GetMapping("/external-balance/{address}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExternalBalance(@PathVariable String address) {
        try {
            // Sepolia Etherscan API 사용
            String apiUrl = "https://api-sepolia.etherscan.io/api?module=account&action=balance&address=" + address + "&tag=latest";
            
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            Map<String, Object> result = new HashMap<>();
            result.put("address", address);
            result.put("etherscanResponse", response);
            result.put("note", "Etherscan API를 통한 실제 Sepolia 잔액 확인");
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            log.error("외부 API 잔액 조회 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("외부 API 잔액 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 현재 사용 중인 계정의 외부 잔액 확인
     */
    @GetMapping("/check-balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkCurrentAccountBalance() {
        try {
            if (privateKey == null || privateKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("BLOCKCHAIN_PRIVATE_KEY가 설정되지 않았습니다."));
            }
            
            Credentials credentials = Credentials.create(privateKey);
            String address = credentials.getAddress();
            
            return getExternalBalance(address);
            
        } catch (Exception e) {
            log.error("현재 계정 잔액 확인 실패", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("현재 계정 잔액 확인 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    private String getNetworkName(BigInteger chainId) {
        switch (chainId.intValue()) {
            case 1: return "Ethereum Mainnet";
            case 11155111: return "Sepolia Testnet";
            case 1337: return "Local/Hardhat";
            case 31337: return "Hardhat Network";
            default: return "Unknown Network";
        }
    }
}