// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title MockUSDC
 * @dev Sepolia 테스트넷에서 테스트용 Mock USDC 토큰
 * USDC 특화 소수점(6자리)을 가진 ERC20 구현 및 테스트용 faucet 함수 포함
 */
contract MockUSDC is ERC20, Ownable {
    // USDC 특화 상수
    string public constant NAME = "Mock USD Coin";
    string public constant SYMBOL = "USDC";
    uint8 public constant DECIMALS = 6;

    // Faucet 금액: 요청당 100 USDC
    uint256 public constant FAUCET_AMOUNT = 100 * 10**DECIMALS;

    // 스팸 방지를 위한 주소별 마지막 faucet 요청 시간 추적
    mapping(address => uint256) public lastFaucetTime;
    uint256 public constant FAUCET_COOLDOWN = 1 days;
    
    /**
     * @dev 배포자에게 초기 공급량을 발행하는 생성자
     * 초기 공급량: 1,000,000 USDC
     */
    constructor() ERC20(NAME, SYMBOL) Ownable(msg.sender) {
        // 배포자에게 100만 USDC 발행
        _mint(msg.sender, 1000000 * 10**DECIMALS);
    }
    
    /**
     * @dev 6을 반환하도록 decimals 오버라이드 (USDC 표준)
     */
    function decimals() public pure override returns (uint8) {
        return DECIMALS;
    }
    
    /**
     * @dev 테스트용 Faucet 함수 - 누구나 테스트 USDC 요청 가능
     * 테스트 목적의 무제한 발행 (쿨다운 및 제한 제거)
     */
    function faucet(address to, uint256 amount) external {
        require(amount > 0, "MockUSDC: Amount must be greater than 0");
        // 테스트용 무제한 발행 - 쿨다운 및 금액 제한 제거
        _mint(to, amount);
    }
    
    /**
     * @dev 필요 시 추가 공급량을 위한 소유자 전용 발행 함수
     */
    function mint(address to, uint256 amount) external onlyOwner {
        _mint(to, amount);
    }
    
    /**
     * @dev 주소의 faucet 쿨다운 남은 시간 조회
     */
    function getFaucetCooldown(address user) external view returns (uint256) {
        if (lastFaucetTime[user] == 0) {
            return 0;
        }
        
        uint256 nextAvailable = lastFaucetTime[user] + FAUCET_COOLDOWN;
        if (block.timestamp >= nextAvailable) {
            return 0;
        }
        
        return nextAvailable - block.timestamp;
    }
}