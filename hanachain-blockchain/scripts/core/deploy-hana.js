/**
 * HanaChain 컨트랙트 배포 스크립트
 * MockUSDC와 HanaChainCampaign 컨트랙트를 Sepolia 테스트넷에 배포합니다.
 */

const { ethers } = require("hardhat");

async function main() {
    console.log("🚀 HanaChain 컨트랙트 배포를 시작합니다...\n");
    
    // 배포자 계정 정보
    const [deployer] = await ethers.getSigners();
    const deployerAddress = await deployer.getAddress();
    const balance = await ethers.provider.getBalance(deployerAddress);
    
    console.log("📊 배포 정보:");
    console.log(`배포자 주소: ${deployerAddress}`);
    console.log(`배포자 잔고: ${ethers.formatEther(balance)} ETH`);
    console.log(`네트워크: ${hre.network.name}`);
    console.log("─".repeat(60));
    
    // 배포자 잔고 확인 (0.01 ETH 이상 필요)
    const minimumBalance = ethers.parseEther("0.01");
    if (balance < minimumBalance) {
        throw new Error(`❌ 배포자 잔고가 부족합니다. 최소 0.01 ETH가 필요합니다. 현재: ${ethers.formatEther(balance)} ETH`);
    }
    
    try {
        // 1. MockUSDC 컨트랙트 배포
        console.log("1️⃣ MockUSDC 컨트랙트 배포 중...");
        const MockUSDC = await ethers.getContractFactory("MockUSDC");
        const mockUSDC = await MockUSDC.deploy();
        await mockUSDC.waitForDeployment();
        
        const mockUSDCAddress = await mockUSDC.getAddress();
        console.log(`✅ MockUSDC 배포 완료: ${mockUSDCAddress}`);
        
        // MockUSDC 정보 확인
        const name = await mockUSDC.name();
        const symbol = await mockUSDC.symbol();
        const decimals = await mockUSDC.decimals();
        const totalSupply = await mockUSDC.totalSupply();
        
        console.log(`   이름: ${name}`);
        console.log(`   심볼: ${symbol}`);
        console.log(`   소수점: ${decimals}`);
        console.log(`   총 공급량: ${ethers.formatUnits(totalSupply, decimals)} ${symbol}`);
        console.log();
        
        // 2. HanaChainCampaign 컨트랙트 배포
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
        
        // 3. 배포 완료 정보
        console.log("🎉 배포 완료!");
        console.log("─".repeat(60));
        console.log("📋 배포된 컨트랙트 주소:");
        console.log(`MockUSDC: ${mockUSDCAddress}`);
        console.log(`HanaChainCampaign: ${campaignAddress}`);
        console.log();
        
        // 4. 검증 명령어 안내
        if (hre.network.name === "sepolia") {
            console.log("🔍 Etherscan 검증 명령어:");
            console.log(`npx hardhat verify --network sepolia ${mockUSDCAddress}`);
            console.log(`npx hardhat verify --network sepolia ${campaignAddress} ${mockUSDCAddress}`);
            console.log();
        }
        
        // 5. 환경변수 업데이트 안내
        console.log("⚙️ 환경변수 업데이트:");
        console.log(`MOCK_USDC_ADDRESS=${mockUSDCAddress}`);
        console.log(`HANA_CAMPAIGN_ADDRESS=${campaignAddress}`);
        console.log();
        
        // 6. 배포 정보 파일 저장
        const fs = require('fs');
        const path = require('path');
        
        const deploymentInfo = {
            network: hre.network.name,
            chainId: (await ethers.provider.getNetwork()).chainId.toString(),
            contracts: {
                mockUSDC: mockUSDCAddress,
                hanaChainCampaign: campaignAddress
            },
            deployer: deployerAddress,
            timestamp: Date.now(),
            blockNumber: await ethers.provider.getBlockNumber()
        };
        
        // deployment-hana.json 파일로 저장
        const deploymentPath = path.join(__dirname, '..', 'deployment-hana.json');
        fs.writeFileSync(deploymentPath, JSON.stringify(deploymentInfo, null, 2));
        console.log(`📁 배포 정보가 ${deploymentPath}에 저장되었습니다.`);
        
        // Spring Boot 연동을 위한 application.properties 형식 생성
        const springConfigPath = path.join(__dirname, '..', 'spring-blockchain-config.properties');
        const springConfig = `# HanaChain 블록체인 설정
# 생성 시간: ${new Date().toISOString()}
blockchain.network=${hre.network.name}
blockchain.chainId=${deploymentInfo.chainId}
blockchain.contracts.mockUsdc=${mockUSDCAddress}
blockchain.contracts.hanaChainCampaign=${campaignAddress}
blockchain.deployer=${deployerAddress}
`;
        
        fs.writeFileSync(springConfigPath, springConfig);
        console.log(`📁 Spring Boot 설정이 ${springConfigPath}에 저장되었습니다.`);
        console.log();
        
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