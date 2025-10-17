package com.hanachain.hanachainbackend.controller.dev;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.service.BlockchainTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 블록체인 연결 테스트 컨트롤러
 * Web3j 연결 상태를 확인할 수 있는 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/blockchain/test")
@RequiredArgsConstructor
@Slf4j
public class BlockchainTestController {

    private final BlockchainTestService blockchainTestService;

    /**
     * Web3j 연결 상태를 테스트합니다.
     * 
     * @return API 응답
     */
    @GetMapping("/connection")
    public ApiResponse<Boolean> testConnection() {
        log.info("Testing blockchain connection...");
        
        boolean connected = blockchainTestService.testConnection();
        
        if (connected) {
            return ApiResponse.success("블록체인 연결이 성공했습니다.", true);
        } else {
            return ApiResponse.error("블록체인 연결에 실패했습니다.");
        }
    }

    /**
     * 네트워크 정보를 조회합니다.
     * 
     * @return 네트워크 정보 API 응답
     */
    @GetMapping("/network-info")
    public ApiResponse<BlockchainTestService.NetworkInfo> getNetworkInfo() {
        log.info("Getting blockchain network info...");
        
        BlockchainTestService.NetworkInfo networkInfo = blockchainTestService.getNetworkInfo();
        
        if (networkInfo.isConnected()) {
            return ApiResponse.success("네트워크 정보를 성공적으로 조회했습니다.", networkInfo);
        } else {
            return ApiResponse.error("네트워크 정보 조회에 실패했습니다.", networkInfo);
        }
    }
}