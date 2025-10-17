package com.hanachain.hanachainbackend.dto.wallet;

import com.hanachain.hanachainbackend.service.WalletService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * 지갑 잔액 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceResponse {
    
    /**
     * 지갑 주소
     */
    private String walletAddress;
    
    /**
     * USDC 잔액 (원본 값, 6 decimals)
     */
    private String usdcBalanceRaw;
    
    /**
     * USDC 잔액 (포맷된 값)
     */
    private String usdcBalance;
    
    /**
     * ETH 잔액 (원본 값, wei)
     */
    private String ethBalanceRaw;
    
    /**
     * ETH 잔액 (포맷된 값)
     */
    private String ethBalance;
    
    /**
     * 총 USD 가치 추정 (향후 확장용)
     */
    private String totalUsdValue;
    
    /**
     * WalletBalanceInfo로부터 DTO 생성
     */
    public static WalletBalanceResponse from(WalletService.WalletBalanceInfo balanceInfo) {
        return WalletBalanceResponse.builder()
                .walletAddress(balanceInfo.getWalletAddress())
                .usdcBalanceRaw(balanceInfo.getUsdcBalance().toString())
                .usdcBalance(formatUSDCBalance(balanceInfo.getUsdcBalance()))
                .ethBalanceRaw(balanceInfo.getEthBalance().toString())
                .ethBalance(formatETHBalance(balanceInfo.getEthBalance()))
                .totalUsdValue("0.00") // 향후 실제 계산 로직 구현
                .build();
    }
    
    /**
     * USDC 잔액을 포맷합니다 (6 decimals)
     */
    private static String formatUSDCBalance(BigInteger usdcBalance) {
        if (usdcBalance == null || usdcBalance.equals(BigInteger.ZERO)) {
            return "0.00";
        }
        
        BigDecimal balance = new BigDecimal(usdcBalance)
                .divide(new BigDecimal("1000000"), 6, RoundingMode.DOWN);
        
        return balance.setScale(2, RoundingMode.DOWN).toString();
    }
    
    /**
     * ETH 잔액을 포맷합니다 (18 decimals)
     */
    private static String formatETHBalance(BigInteger ethBalance) {
        if (ethBalance == null || ethBalance.equals(BigInteger.ZERO)) {
            return "0.000000";
        }
        
        BigDecimal ethValue = Convert.fromWei(new BigDecimal(ethBalance), Convert.Unit.ETHER);
        return ethValue.setScale(6, RoundingMode.DOWN).toString();
    }
}