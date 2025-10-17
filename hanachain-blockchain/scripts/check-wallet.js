require('dotenv').config();
const { ethers } = require('hardhat');

async function checkWallet() {
    console.log("👛 지갑 상태 확인 중...\n");
    
    try {
        // 지갑 생성
        const provider = new ethers.JsonRpcProvider(process.env.SEPOLIA_URL);
        const wallet = new ethers.Wallet(process.env.PRIVATE_KEY, provider);
        
        console.log("=== 지갑 정보 ===");
        console.log(`📍 주소: ${wallet.address}`);
        
        // 잔액 확인
        const balance = await provider.getBalance(wallet.address);
        const balanceInEth = ethers.formatEther(balance);
        console.log(`💰 잔액: ${balanceInEth} ETH`);
        
        // 잔액 상태 판단
        if (parseFloat(balanceInEth) > 0.1) {
            console.log("✅ 충분한 테스트 ETH 보유 (배포 가능)");
        } else if (parseFloat(balanceInEth) > 0.01) {
            console.log("⚠️ 적은 양의 테스트 ETH (기본 테스트 가능)");
        } else {
            console.log("❌ 테스트 ETH 부족 (Faucet에서 받아야 함)");
            console.log("🔗 Sepolia Faucet: https://sepoliafaucet.com/");
        }
        
        // 네트워크 상태
        const gasPrice = await provider.getFeeData();
        console.log(`⛽ 현재 가스 가격: ${ethers.formatUnits(gasPrice.gasPrice, 'gwei')} gwei`);
        
        return parseFloat(balanceInEth) > 0;
        
    } catch (error) {
        console.log("❌ 지갑 확인 실패:", error.message);
        if (error.message.includes('invalid private key')) {
            console.log("💡 해결책: MetaMask에서 개인키를 다시 복사해서 .env 파일에 설정하세요");
        }
        return false;
    }
}

checkWallet(); 