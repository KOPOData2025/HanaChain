/**
 * MockUSDC 컨트랙트 단독 배포 스크립트
 * Sepolia 테스트넷에 MockUSDC 컨트랙트만 배포합니다.
 */

const { ethers } = require("hardhat");

async function main() {
    console.log("🚀 MockUSDC 컨트랙트 배포를 시작합니다...\n");

    // 배포자 계정 정보
    const [deployer] = await ethers.getSigners();
    const deployerAddress = await deployer.getAddress();
    const balance = await ethers.provider.getBalance(deployerAddress);

    console.log("📊 배포 정보:");
    console.log(`배포자 주소: ${deployerAddress}`);
    console.log(`배포자 잔고: ${ethers.formatEther(balance)} ETH`);
    console.log(`네트워크: ${hre.network.name}`);
    console.log("─".repeat(60));

    // 배포자 잔고 확인 (0.005 ETH 이상 필요)
    const minimumBalance = ethers.parseEther("0.005");
    if (balance < minimumBalance) {
        throw new Error(`❌ 배포자 잔고가 부족합니다. 최소 0.005 ETH가 필요합니다. 현재: ${ethers.formatEther(balance)} ETH`);
    }

    try {
        // MockUSDC 컨트랙트 배포
        console.log("💰 MockUSDC 컨트랙트 배포 중...");
        const MockUSDC = await ethers.getContractFactory("MockUSDC");
        const mockUSDC = await MockUSDC.deploy();
        await mockUSDC.waitForDeployment();

        const mockUSDCAddress = await mockUSDC.getAddress();
        console.log(`✅ MockUSDC 배포 완료: ${mockUSDCAddress}`);
        console.log();

        // MockUSDC 정보 확인
        const name = await mockUSDC.name();
        const symbol = await mockUSDC.symbol();
        const decimals = await mockUSDC.decimals();
        const totalSupply = await mockUSDC.totalSupply();

        console.log("📋 MockUSDC 컨트랙트 정보:");
        console.log(`   이름: ${name}`);
        console.log(`   심볼: ${symbol}`);
        console.log(`   소수점: ${decimals}`);
        console.log(`   총 공급량: ${ethers.formatUnits(totalSupply, decimals)} ${symbol}`);
        console.log(`   배포자 잔고: ${ethers.formatUnits(await mockUSDC.balanceOf(deployerAddress), decimals)} ${symbol}`);
        console.log();

        // 배포 완료 정보
        console.log("🎉 배포 완료!");
        console.log("─".repeat(60));
        console.log(`MockUSDC 주소: ${mockUSDCAddress}`);
        console.log();

        // Etherscan 검증 명령어 안내
        if (hre.network.name === "sepolia") {
            console.log("🔍 Etherscan 검증 명령어:");
            console.log(`npx hardhat verify --network sepolia ${mockUSDCAddress}`);
            console.log();
        }

        // 환경변수 업데이트 안내
        console.log("⚙️ 환경변수 업데이트:");
        console.log(`MOCK_USDC_ADDRESS=${mockUSDCAddress}`);
        console.log();

        console.log("📝 Spring Boot application.properties 업데이트:");
        console.log(`blockchain.contracts.usdc-address=${mockUSDCAddress}`);
        console.log();

        // 배포 정보 파일 저장
        const fs = require('fs');
        const path = require('path');

        const deploymentInfo = {
            network: hre.network.name,
            chainId: (await ethers.provider.getNetwork()).chainId.toString(),
            contract: {
                name: "MockUSDC",
                address: mockUSDCAddress,
                symbol: symbol,
                decimals: Number(decimals),
                totalSupply: ethers.formatUnits(totalSupply, decimals)
            },
            deployer: deployerAddress,
            timestamp: new Date().toISOString(),
            blockNumber: await ethers.provider.getBlockNumber()
        };

        // deployment-mock-usdc.json 파일로 저장
        const deploymentPath = path.join(__dirname, '..', 'deployment-mock-usdc.json');
        fs.writeFileSync(deploymentPath, JSON.stringify(deploymentInfo, null, 2));
        console.log(`📁 배포 정보가 ${deploymentPath}에 저장되었습니다.`);
        console.log();

        return {
            address: mockUSDCAddress,
            name: name,
            symbol: symbol,
            decimals: Number(decimals)
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
