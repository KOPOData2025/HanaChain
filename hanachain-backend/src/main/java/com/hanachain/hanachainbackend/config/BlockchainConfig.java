package com.hanachain.hanachainbackend.config;

import com.hanachain.hanachainbackend.service.blockchain.MockUSDCWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Web3j 블록체인 연결 설정
 * Sepolia 테스트넷과의 연결을 구성합니다.
 */
@Configuration
@Slf4j
public class BlockchainConfig {

    @Value("${blockchain.network.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.network.chain-id}")
    private Long chainId;

    @Value("${blockchain.network.name}")
    private String networkName;

    @Value("${blockchain.contracts.usdc-address}")
    private String usdcContractAddress;

    @Value("${blockchain.contracts.campaign-address}")
    private String campaignContractAddress;

    @Value("${blockchain.timeout.connection:60000}")
    private Long connectionTimeout;

    @Value("${blockchain.timeout.read:120000}")
    private Long readTimeout;

    @Value("${blockchain.timeout.write:60000}")
    private Long writeTimeout;

    /**
     * Web3j 인스턴스를 생성합니다.
     * HTTP 서비스를 통해 블록체인 네트워크에 연결합니다.
     */
    @Bean
    public Web3j web3j() {
        log.info("Configuring Web3j connection to {} network", networkName);
        log.info("RPC URL: {}", rpcUrl);
        log.info("Chain ID: {}", chainId);
        log.info("USDC Contract: {}", usdcContractAddress);
        log.info("Campaign Contract: {}", campaignContractAddress);
        log.info("Connection Timeout: {}ms", connectionTimeout);
        log.info("Read Timeout: {}ms", readTimeout);
        log.info("Write Timeout: {}ms", writeTimeout);
        
        // OkHttpClient 커스터마이징으로 타임아웃 설정
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true);
        
        HttpService httpService = new HttpService(rpcUrl, clientBuilder.build());
        return Web3j.build(httpService);
    }

    /**
     * 체인 ID를 반환합니다.
     */
    @Bean
    public Long chainId() {
        return chainId;
    }

    /**
     * USDC 컨트랙트 주소를 반환합니다.
     */
    @Bean("usdcContractAddress")
    public String usdcContractAddress() {
        return usdcContractAddress;
    }

    /**
     * 캠페인 컨트랙트 주소를 반환합니다.
     */
    @Bean("campaignContractAddress")
    public String campaignContractAddress() {
        return campaignContractAddress;
    }

    /**
     * 네트워크 이름을 반환합니다.
     */
    @Bean("networkName")
    public String networkName() {
        return networkName;
    }

    /**
     * MockUSDCWrapper 빈을 생성합니다.
     * USDC 컨트랙트와의 상호작용을 담당합니다.
     */
    @Bean
    public MockUSDCWrapper mockUSDCWrapper(
            Web3j web3j,
            @Qualifier("usdcContractAddress") String contractAddress) {
        log.info("Configuring MockUSDCWrapper with contract address: {}", contractAddress);
        return new MockUSDCWrapper(web3j, contractAddress);
    }
}