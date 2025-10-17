require('dotenv').config();
const { ethers } = require('hardhat');

async function verifyCompleteSetup() {
    console.log("🎯 WeAreHana 블록체인 개발환경 종합 검증\n");
    console.log("=" .repeat(50));
    
    let allTestsPassed = true;
    const testResults = [];
    
    // 테스트 1: 패키지 설치 확인
    console.log("\n1️⃣ 패키지 설치 확인");
    try {
        require('@nomicfoundation/hardhat-toolbox');
        require('ethers');
        console.log("✅ 필수 패키지 모두 설치됨");
        testResults.push("✅ 패키지 설치");
    } catch (error) {
        console.log("❌ 패키지 누락:", error.message);
        testResults.push("❌ 패키지 설치");
        allTestsPassed = false;
    }
    
    // 테스트 2: 환경 변수 확인
    console.log("\n2️⃣ 환경 변수 확인");
    if (process.env.SEPOLIA_URL && process.env.PRIVATE_KEY) {
        console.log("✅ 필수 환경 변수 설정됨");
        
        // Alchemy 사용 여부 확인
        if (process.env.SEPOLIA_URL.includes('alchemy.com')) {
            console.log("✅ Alchemy RPC 서비스 사용 중 (권장)");
        } else {
            console.log("⚠️ 다른 RPC 서비스 사용 중 (Alchemy 권장)");
        }
        
        testResults.push("✅ 환경 변수");
    } else {
        console.log("❌ 환경 변수 누락");
        testResults.push("❌ 환경 변수");
        allTestsPassed = false;
    }
    
    // 테스트 3: 네트워크 연결
    console.log("\n3️⃣ Sepolia 네트워크 연결");
    try {
        const provider = new ethers.JsonRpcProvider(process.env.SEPOLIA_URL);
        const network = await provider.getNetwork();
        
        if (network.chainId === 11155111n) {
            console.log("✅ Sepolia 테스트넷 연결 성공");
            testResults.push("✅ 네트워크 연결");
        } else {
            console.log(`❌ 잘못된 네트워크 (Chain ID: ${network.chainId})`);
            testResults.push("❌ 네트워크 연결");
            allTestsPassed = false;
        }
    } catch (error) {
        console.log("❌ 네트워크 연결 실패:", error.message);
        console.log("💡 Alchemy API 키를 확인하거나 다른 RPC URL을 시도해보세요");
        testResults.push("❌ 네트워크 연결");
        allTestsPassed = false;
    }
    
    // 테스트 4: 지갑 및 잔액
    console.log("\n4️⃣ 지갑 및 잔액 확인");
    try {
        const provider = new ethers.JsonRpcProvider(process.env.SEPOLIA_URL);
        const wallet = new ethers.Wallet(process.env.PRIVATE_KEY, provider);
        const balance = await provider.getBalance(wallet.address);
        const balanceInEth = parseFloat(ethers.formatEther(balance));
        
        console.log(`📍 지갑 주소: ${wallet.address}`);
        console.log(`💰 현재 잔액: ${balanceInEth} ETH`);
        
        if (balanceInEth > 0.01) {
            console.log("✅ 테스트 ETH 보유 (개발 가능)");
            testResults.push("✅ 지갑 잔액");
        } else {
            console.log("⚠️ 테스트 ETH 필요 (Faucet 이용)");
            testResults.push("⚠️ 지갑 잔액");
        }
    } catch (error) {
        console.log("❌ 지갑 오류:", error.message);
        testResults.push("❌ 지갑 잔액");
        allTestsPassed = false;
    }
    
    // 테스트 5: Hardhat 설정
    console.log("\n5️⃣ Hardhat 설정 확인");
    try {
        const hre = require("hardhat");
        const networks = Object.keys(hre.config.networks);
        
        if (networks.includes('sepolia')) {
            console.log("✅ Hardhat 네트워크 설정 완료");
            console.log(`📡 설정된 네트워크: ${networks.join(', ')}`);
            testResults.push("✅ Hardhat 설정");
        } else {
            console.log("❌ Sepolia 네트워크 설정 누락");
            testResults.push("❌ Hardhat 설정");
            allTestsPassed = false;
        }
    } catch (error) {
        console.log("❌ Hardhat 설정 오류:", error.message);
        testResults.push("❌ Hardhat 설정");
        allTestsPassed = false;
    }
    
    // 최종 결과
    console.log("\n" + "=".repeat(50));
    console.log("📋 검증 결과 요약");
    console.log("=".repeat(50));
    
    testResults.forEach(result => {
        console.log(`   ${result}`);
    });
    
    if (allTestsPassed) {
        console.log("\n🎉 모든 설정이 완료되었습니다!");
        console.log("✨ 이제 스마트 컨트랙트 개발을 시작할 수 있습니다.");
        console.log("\n📚 다음 단계:");
        console.log("   1. contracts/ 폴더에 스마트 컨트랙트 생성");
        console.log("   2. 기본 컨트랙트 구조 작성");
        console.log("   3. npx hardhat compile로 컴파일 테스트");
    } else {
        console.log("\n⚠️ 일부 설정에 문제가 있습니다.");
        console.log("❌ 표시된 항목들을 먼저 해결해주세요.");
    }
    
    console.log("\n🔗 유용한 링크:");
    console.log("   • Alchemy: https://www.alchemy.com/ (무료 API 키 발급)");
    console.log("   • Sepolia Faucet: https://sepoliafaucet.com/");
    console.log("   • Sepolia Explorer: https://sepolia.etherscan.io/");
    console.log("   • MetaMask 가이드: https://metamask.io/");
}

verifyCompleteSetup().catch(console.error); 