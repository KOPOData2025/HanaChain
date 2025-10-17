/**
 * HanaChainCampaign 컨트랙트 배포 스크립트
 * 이미 배포된 MockUSDC 주소를 사용합니다.
 */

const { ethers } = require("hardhat");

async function main() {
    console.log("🚀 HanaChainCampaign 컨트랙트 배포를 시작합니다...\n");
    
    // 이미 배포된 MockUSDC 주소
    const mockUSDCAddress = "0xE6497C56aF2D2746c2e9C0f079C9eaA9895b3baE";
    
    // 배포자 계정 정보
    const [deployer] = await ethers.getSigners();
    const deployerAddress = await deployer.getAddress();
    const balance = await ethers.provider.getBalance(deployerAddress);
    
    console.log("📊 배포 정보:");
    console.log(`배포자 주소: ${deployerAddress}`);
    console.log(`배포자 잔고: ${ethers.formatEther(balance)} ETH`);
    console.log(`MockUSDC 주소: ${mockUSDCAddress}`);
    console.log(`네트워크: ${hre.network.name}`);
    console.log("─".repeat(60));
    
    try {
        // HanaChainCampaign 컨트랙트 배포
        console.log("2️⃣ HanaChainCampaign 컨트랙트 배포 중...");
        const HanaChainCampaign = await ethers.getContractFactory("HanaChainCampaign");
        const campaign = await HanaChainCampaign.deploy(mockUSDCAddress);
        await campaign.waitForDeployment();

        const campaignAddress = await campaign.getAddress();
        console.log(`✅ HanaChainCampaign 배포 완료: ${campaignAddress}`);

        // Campaign 정보 확인
        const usdcToken = await campaign.usdcToken();
        const platformFee = await campaign.platformFee();
        const owner = await campaign.owner();
        
        console.log(`   USDC 토큰: ${usdcToken}`);
        console.log(`   플랫폼 수수료: ${Number(platformFee) / 100}%`);
        console.log(`   소유자: ${owner}`);
        console.log();
        
        // 배포 완료 정보
        console.log("🎉 HanaChainCampaign 배포 완료!");
        console.log("─".repeat(60));
        console.log("📋 컨트랙트 주소:");
        console.log(`MockUSDC: ${mockUSDCAddress}`);
        console.log(`HanaChainCampaign: ${campaignAddress}`);
        console.log();
        
        // 검증 명령어 안내
        if (hre.network.name === "sepolia") {
            console.log("🔍 Etherscan 검증 명령어:");
            console.log(`npx hardhat verify --network sepolia ${campaignAddress} ${mockUSDCAddress}`);
            console.log();
        }
        
        return {
            mockUSDC: mockUSDCAddress,
            campaign: campaignAddress
        };
        
    } catch (error) {
        console.error("❌ 배포 실패:", error);
        process.exit(1);
    }
}

// 스크립트 실행
if (require.main === module) {
    main()
        .then(() => process.exit(0))
        .catch((error) => {
            console.error("❌ 배포 스크립트 실행 실패:", error);
            process.exit(1);
        });
}

module.exports = main;