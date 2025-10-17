package com.hanachain.hanachainbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 블록체인 연결 테스트 서비스
 *
 * <p>Web3j 연결 상태를 확인하고 기본적인 블록체인 정보를 조회합니다.</p>
 *
 * <p><strong>Architecture Note:</strong> 이 서비스는 개발/테스트 전용이며,
 * 의도적으로 인터페이스 없이 구현되었습니다. 프로덕션 코드에서 직접 사용되지 않으며,
 * 주로 블록체인 연결 상태 확인 및 개발 환경 검증 목적으로 사용됩니다.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * // 연결 테스트
 * boolean isConnected = blockchainTestService.testConnection();
 *
 * // 네트워크 정보 조회
 * NetworkInfo info = blockchainTestService.getNetworkInfo();
 * log.info("Network: {}, Block: {}", info.getNetworkName(), info.getBlockNumber());
 * </pre>
 *
 * @author HanaChain Backend Team
 * @version 1.0
 * @see org.web3j.protocol.Web3j
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainTestService {

    private final Web3j web3j;

    @Value("${blockchain.network.name}")
    private String networkName;

    /**
     * Web3j 연결 상태를 테스트합니다.
     *
     * <p>클라이언트 버전, 현재 블록 번호, 가스 가격을 순차적으로 조회하여
     * 블록체인 네트워크 연결 상태를 확인합니다.</p>
     *
     * @return 연결 성공 시 {@code true}, 실패 시 {@code false}
     */
    public boolean testConnection() {
        try {
            log.info("Testing Web3j connection to {} network...", networkName);
            
            // 클라이언트 버전 조회
            Web3ClientVersion clientVersion = web3j.web3ClientVersion().send();
            if (clientVersion.hasError()) {
                log.error("Failed to get client version: {}", clientVersion.getError().getMessage());
                return false;
            }
            
            log.info("Connected to client: {}", clientVersion.getWeb3ClientVersion());
            
            // 현재 블록 번호 조회
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            if (blockNumber.hasError()) {
                log.error("Failed to get block number: {}", blockNumber.getError().getMessage());
                return false;
            }
            
            log.info("Current block number: {}", blockNumber.getBlockNumber());
            
            // 현재 가스 가격 조회
            EthGasPrice gasPrice = web3j.ethGasPrice().send();
            if (gasPrice.hasError()) {
                log.error("Failed to get gas price: {}", gasPrice.getError().getMessage());
                return false;
            }
            
            log.info("Current gas price: {} wei", gasPrice.getGasPrice());
            
            return true;
            
        } catch (IOException e) {
            log.error("Connection test failed", e);
            return false;
        }
    }

    /**
     * 네트워크 정보를 조회합니다.
     *
     * <p>블록체인 네트워크의 현재 상태 정보(네트워크 이름, 클라이언트 버전,
     * 블록 번호, 가스 가격, 연결 상태)를 포함하는 객체를 반환합니다.</p>
     *
     * <p>연결 실패 시에도 기본값이 설정된 {@link NetworkInfo} 객체를 반환합니다.</p>
     *
     * @return 네트워크 정보 객체 (연결 실패 시에도 null이 아닌 객체 반환)
     */
    public NetworkInfo getNetworkInfo() {
        try {
            Web3ClientVersion clientVersion = web3j.web3ClientVersion().send();
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            EthGasPrice gasPrice = web3j.ethGasPrice().send();
            
            return NetworkInfo.builder()
                    .networkName(networkName)
                    .clientVersion(clientVersion.hasError() ? "Error" : clientVersion.getWeb3ClientVersion())
                    .blockNumber(blockNumber.hasError() ? BigInteger.ZERO : blockNumber.getBlockNumber())
                    .gasPrice(gasPrice.hasError() ? BigInteger.ZERO : gasPrice.getGasPrice())
                    .connected(!clientVersion.hasError() && !blockNumber.hasError())
                    .build();
                    
        } catch (IOException e) {
            log.error("Failed to get network info", e);
            return NetworkInfo.builder()
                    .networkName(networkName)
                    .clientVersion("Connection Failed")
                    .blockNumber(BigInteger.ZERO)
                    .gasPrice(BigInteger.ZERO)
                    .connected(false)
                    .build();
        }
    }

    /**
     * 네트워크 정보를 담는 데이터 클래스
     *
     * <p>블록체인 네트워크의 현재 상태를 표현하는 불변 객체입니다.
     * Builder 패턴을 사용하여 인스턴스를 생성합니다.</p>
     *
     * @see NetworkInfoBuilder
     */
    public static class NetworkInfo {
        private String networkName;
        private String clientVersion;
        private BigInteger blockNumber;
        private BigInteger gasPrice;
        private boolean connected;

        // Builder pattern을 위한 정적 메서드
        public static NetworkInfoBuilder builder() {
            return new NetworkInfoBuilder();
        }

        // Getter
        public String getNetworkName() { return networkName; }
        public String getClientVersion() { return clientVersion; }
        public BigInteger getBlockNumber() { return blockNumber; }
        public BigInteger getGasPrice() { return gasPrice; }
        public boolean isConnected() { return connected; }

        // Builder 클래스
        public static class NetworkInfoBuilder {
            private NetworkInfo networkInfo = new NetworkInfo();

            public NetworkInfoBuilder networkName(String networkName) {
                networkInfo.networkName = networkName;
                return this;
            }

            public NetworkInfoBuilder clientVersion(String clientVersion) {
                networkInfo.clientVersion = clientVersion;
                return this;
            }

            public NetworkInfoBuilder blockNumber(BigInteger blockNumber) {
                networkInfo.blockNumber = blockNumber;
                return this;
            }

            public NetworkInfoBuilder gasPrice(BigInteger gasPrice) {
                networkInfo.gasPrice = gasPrice;
                return this;
            }

            public NetworkInfoBuilder connected(boolean connected) {
                networkInfo.connected = connected;
                return this;
            }

            public NetworkInfo build() {
                return networkInfo;
            }
        }
    }
}