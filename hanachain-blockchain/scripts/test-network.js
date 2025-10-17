require('dotenv').config();
const { ethers } = require('hardhat');

async function testNetworkConnection() {
    console.log("🌐 네트워크 연결 테스트 중...\n");
    
    try {
        // Sepolia 네트워크 연결 테스트
        console.log("=== Sepolia 테스트넷 연결 ===");
        const provider = new ethers.JsonRpcProvider(process.env.SEPOLIA_URL);
        
        // 네트워크 정보 확인
        const network = await provider.getNetwork();
        console.log(`✅ 네트워크 연결 성공`);
        console.log(`   이름: ${network.name}`);
        console.log(`   체인 ID: ${network.chainId}`);
        
        // 최신 블록 확인
        const blockNumber = await provider.getBlockNumber();
        console.log(`   최신 블록: ${blockNumber}`);
        
        return true;
    } catch (error) {
        console.log("❌ 네트워크 연결 실패:", error.message);
        return false;
    }
}

testNetworkConnection(); 