package com.hanachain.hanachainbackend.controller.api;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.wallet.*;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.UserWallet;
import com.hanachain.hanachainbackend.security.SecurityUtils;
import com.hanachain.hanachainbackend.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 지갑 관리 API 컨트롤러
 * 사용자의 지갑 생성, 연결, 관리 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet", description = "지갑 관리 API")
public class WalletController {
    
    private final WalletService walletService;
    
    // =========================== 지갑 조회 ===========================
    
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "사용자 지갑 목록 조회", description = "현재 로그인한 사용자의 모든 지갑을 조회합니다")
    public ResponseEntity<ApiResponse<List<WalletResponse>>> getUserWallets() {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        List<UserWallet> wallets = walletService.getUserWallets(currentUser);
        List<WalletResponse> walletDtos = wallets.stream()
                .map(WalletResponse::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(walletDtos));
    }
    
    @GetMapping("/primary")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "주 지갑 조회", description = "현재 로그인한 사용자의 주 지갑을 조회합니다")
    public ResponseEntity<ApiResponse<WalletResponse>> getPrimaryWallet() {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        return walletService.getPrimaryWallet(currentUser)
                .map(wallet -> ResponseEntity.ok(ApiResponse.success(WalletResponse.from(wallet))))
                .orElse(ResponseEntity.ok(ApiResponse.success("주 지갑이 설정되지 않았습니다")));
    }
    
    @GetMapping("/{walletId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "특정 지갑 조회", description = "지갑 ID로 특정 지갑 정보를 조회합니다")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @Parameter(description = "지갑 ID") @PathVariable Long walletId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        // 권한 확인 및 지갑 조회는 서비스에서 처리
        return walletService.getUserWallets(currentUser).stream()
                .filter(wallet -> wallet.getId().equals(walletId))
                .findFirst()
                .map(wallet -> ResponseEntity.ok(ApiResponse.success(WalletResponse.from(wallet))))
                .orElse(ResponseEntity.notFound().build());
    }
    
    // =========================== 지갑 생성 및 연결 ===========================
    
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "새 지갑 생성", description = "새로운 Ethereum 지갑을 생성하고 암호화하여 저장합니다")
    public ResponseEntity<ApiResponse<WalletResponse>> createWallet(
            @Valid @RequestBody WalletCreateRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        UserWallet createdWallet = walletService.createWallet(
                currentUser, 
                request.getPassword(), 
                request.getIsPrimary()
        );
        
        WalletResponse response = WalletResponse.from(createdWallet);
        return ResponseEntity.ok(ApiResponse.success("지갑이 성공적으로 생성되었습니다", response));
    }
    
    @PostMapping("/connect")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "외부 지갑 연결", description = "MetaMask 등 외부 지갑을 계정에 연결합니다")
    public ResponseEntity<ApiResponse<WalletResponse>> connectWallet(
            @Valid @RequestBody WalletConnectRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        UserWallet connectedWallet = walletService.connectExternalWallet(
                currentUser,
                request.getWalletAddress(),
                request.getWalletType(),
                request.getIsPrimary()
        );
        
        WalletResponse response = WalletResponse.from(connectedWallet);
        return ResponseEntity.ok(ApiResponse.success("지갑이 성공적으로 연결되었습니다", response));
    }
    
    // =========================== 지갑 관리 ===========================
    
    @PostMapping("/{walletId}/verify")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "지갑 검증", description = "지갑을 검증 완료 상태로 변경합니다")
    public ResponseEntity<ApiResponse<String>> verifyWallet(
            @Parameter(description = "지갑 ID") @PathVariable Long walletId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        walletService.verifyWallet(walletId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("지갑이 성공적으로 검증되었습니다"));
    }
    
    @PostMapping("/{walletId}/set-primary")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "주 지갑 설정", description = "특정 지갑을 주 지갑으로 설정합니다")
    public ResponseEntity<ApiResponse<String>> setPrimaryWallet(
            @Parameter(description = "지갑 ID") @PathVariable Long walletId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        walletService.setPrimaryWallet(walletId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("주 지갑이 성공적으로 설정되었습니다"));
    }
    
    @DeleteMapping("/{walletId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "지갑 삭제", description = "지갑을 계정에서 제거합니다")
    public ResponseEntity<ApiResponse<String>> deleteWallet(
            @Parameter(description = "지갑 ID") @PathVariable Long walletId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        walletService.deleteWallet(walletId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("지갑이 성공적으로 삭제되었습니다"));
    }
    
    // =========================== 잔액 조회 ===========================
    
    @GetMapping("/{walletId}/balance")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "지갑 잔액 조회", description = "지갑의 ETH 및 USDC 잔액을 조회합니다")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getWalletBalance(
            @Parameter(description = "지갑 ID") @PathVariable Long walletId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        // 지갑 소유권 확인 후 잔액 조회
        return walletService.getUserWallets(currentUser).stream()
                .filter(wallet -> wallet.getId().equals(walletId))
                .findFirst()
                .map(wallet -> {
                    WalletService.WalletBalanceInfo balanceInfo = walletService.updateWalletBalance(wallet);
                    WalletBalanceResponse response = WalletBalanceResponse.from(balanceInfo);
                    return ResponseEntity.ok(ApiResponse.success(response));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/primary/balance")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "주 지갑 잔액 조회", description = "주 지갑의 ETH 및 USDC 잔액을 조회합니다")
    public ResponseEntity<ApiResponse<WalletBalanceResponse>> getPrimaryWalletBalance() {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        return walletService.getPrimaryWallet(currentUser)
                .map(wallet -> {
                    WalletService.WalletBalanceInfo balanceInfo = walletService.updateWalletBalance(wallet);
                    WalletBalanceResponse response = WalletBalanceResponse.from(balanceInfo);
                    return ResponseEntity.ok(ApiResponse.success(response));
                })
                .orElse(ResponseEntity.ok(ApiResponse.success("주 지갑이 설정되지 않았습니다")));
    }
    
    // =========================== 지갑 검증 및 유틸리티 ===========================
    
    @GetMapping("/validate/{address}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "지갑 주소 유효성 검증", description = "지갑 주소가 유효한 Ethereum 주소인지 확인합니다")
    public ResponseEntity<ApiResponse<Boolean>> validateWalletAddress(
            @Parameter(description = "검증할 지갑 주소") @PathVariable String address) {
        boolean isValid = walletService.isValidWalletAddress(address);
        return ResponseEntity.ok(ApiResponse.success(isValid));
    }
    
    @GetMapping("/check/{address}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "지갑 주소 등록 여부 확인", description = "지갑 주소가 이미 등록되어 있는지 확인합니다")
    public ResponseEntity<ApiResponse<Boolean>> checkWalletRegistration(
            @Parameter(description = "확인할 지갑 주소") @PathVariable String address) {
        boolean isRegistered = walletService.isWalletAddressRegistered(address);
        return ResponseEntity.ok(ApiResponse.success(isRegistered));
    }
    
    // =========================== 트랜잭션 서명 ===========================
    
    @PostMapping("/sign")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "트랜잭션 서명", description = "내부 생성 지갑으로 트랜잭션에 서명합니다")
    public ResponseEntity<ApiResponse<String>> signTransaction(
            @Valid @RequestBody WalletSignRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다"));
        
        // 지갑 소유권 확인 후 서명
        return walletService.getUserWallets(currentUser).stream()
                .filter(wallet -> wallet.getId().equals(request.getWalletId()))
                .findFirst()
                .map(wallet -> {
                    String signedData = walletService.signTransaction(
                            wallet, 
                            request.getPassword(), 
                            request.getTransactionData()
                    );
                    return ResponseEntity.ok(ApiResponse.success("트랜잭션이 성공적으로 서명되었습니다", signedData));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}