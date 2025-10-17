require('dotenv').config();

async function checkEnvironment() {
    console.log("🔍 환경 설정 검증 중...\n");
    
    // 1. 환경 변수 확인
    console.log("=== 환경 변수 확인 ===");
    const requiredVars = ['SEPOLIA_URL', 'PRIVATE_KEY'];
    const optionalVars = ['ALCHEMY_API_KEY', 'ETHERSCAN_API_KEY'];
    let envOK = true;
    
    requiredVars.forEach(varName => {
        if (process.env[varName]) {
            console.log(`✅ ${varName}: 설정됨`);
        } else {
            console.log(`❌ ${varName}: 누락됨`);
            envOK = false;
        }
    });
    
    console.log("\n=== 선택적 환경 변수 확인 ===");
    optionalVars.forEach(varName => {
        if (process.env[varName]) {
            console.log(`✅ ${varName}: 설정됨`);
        } else {
            console.log(`⚠️ ${varName}: 설정되지 않음 (선택사항)`);
        }
    });
    
    // Alchemy URL 형식 확인
    if (process.env.SEPOLIA_URL && process.env.SEPOLIA_URL.includes('alchemy.com')) {
        console.log("✅ Alchemy RPC URL 형식 확인됨");
    } else if (process.env.SEPOLIA_URL) {
        console.log("⚠️ 다른 RPC 서비스 사용 중 (Alchemy 권장)");
    }
    
    if (!envOK) {
        console.log("\n⚠️ 필수 환경 변수를 .env 파일에 설정해주세요");
        console.log("💡 Alchemy API 키 발급: https://www.alchemy.com/");
        return false;
    }
    
    // 2. Hardhat 설정 확인
    console.log("\n=== Hardhat 설정 확인 ===");
    try {
        const hre = require("hardhat");
        console.log("✅ Hardhat 설정 로드 성공");
        console.log(`📡 네트워크 설정: ${Object.keys(hre.config.networks).join(', ')}`);
    } catch (error) {
        console.log("❌ Hardhat 설정 오류:", error.message);
        return false;
    }
    
    return true;
}

checkEnvironment().then(success => {
    if (success) {
        console.log("\n🎉 환경 설정이 올바르게 완료되었습니다!");
        console.log("💡 Alchemy를 사용하면 더 안정적인 네트워크 연결이 가능합니다.");
    } else {
        console.log("\n❌ 환경 설정에 문제가 있습니다.");
    }
}); 