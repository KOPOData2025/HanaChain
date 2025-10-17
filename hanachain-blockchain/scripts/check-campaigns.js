require('dotenv').config();
const { ethers } = require('hardhat');
const fs = require('fs');

async function checkCampaigns() {
    console.log("🔍 배포된 캠페인 상태 확인 중...\n");
    
    try {
        // deployment-info.json에서 팩토리 주소 읽기
        if (!fs.existsSync('deployment-info.json')) {
            console.log("❌ deployment-info.json 파일을 찾을 수 없습니다.");
            console.log("💡 먼저 npm run deploy-factory를 실행하세요.");
            return;
        }
        
        const deploymentInfo = JSON.parse(fs.readFileSync('deployment-info.json', 'utf8'));
        const factoryAddress = deploymentInfo.factoryAddress;
        
        console.log("🏭 팩토리 주소:", factoryAddress);
        
        // 팩토리 컨트랙트 연결
        const CampaignFactory = await ethers.getContractFactory("CampaignFactory");
        const factory = CampaignFactory.attach(factoryAddress);
        
        // 기본 정보 조회
        const totalCampaigns = await factory.totalCampaigns();
        console.log("📊 총 캠페인 수:", totalCampaigns.toString());
        
        if (totalCampaigns === 0n) {
            console.log("📭 생성된 캠페인이 없습니다.");
            console.log("💡 npm run create-campaign을 실행하여 캠페인을 생성하세요.");
            return;
        }
        
        // 모든 캠페인 조회
        const allCampaigns = await factory.getAllCampaigns();
        const activeCampaigns = await factory.getActiveCampaigns();
        
        console.log("🟢 활성 캠페인 수:", activeCampaigns.length);
        console.log("🟡 비활성 캠페인 수:", allCampaigns.length - activeCampaigns.length);
        
        console.log("\n" + "=".repeat(80));
        console.log("📋 캠페인 목록");
        console.log("=".repeat(80));
        
        // 각 캠페인 상세 정보 조회
        for (let i = 0; i < allCampaigns.length; i++) {
            const campaignAddress = allCampaigns[i];
            console.log(`\n${i + 1}. 캠페인 주소: ${campaignAddress}`);
            
            try {
                // 팩토리에서 기본 정보 조회
                const factoryInfo = await factory.getCampaignInfo(campaignAddress);
                const isActive = factoryInfo[5];
                
                console.log(`   상태: ${isActive ? '🟢 활성' : '🔴 비활성'}`);
                console.log(`   생성자: ${factoryInfo[1]}`);
                console.log(`   생성일: ${new Date(Number(factoryInfo[4]) * 1000).toLocaleString()}`);
                
                // 캠페인 컨트랙트에서 상세 정보 조회
                const details = await factory.getCampaignDetails(campaignAddress);
                
                console.log(`   제목: ${details[0]}`);
                console.log(`   설명: ${details[1]}`);
                console.log(`   목표 금액: ${ethers.formatEther(details[2])} ETH`);
                console.log(`   현재 금액: ${ethers.formatEther(details[3])} ETH`);
                console.log(`   마감일: ${new Date(Number(details[4]) * 1000).toLocaleString()}`);
                console.log(`   수혜자: ${details[5]}`);
                console.log(`   캠페인 상태: ${getCampaignStatusText(details[6])}`);
                console.log(`   기부자 수: ${details[7].toString()}명`);
                
                // 진행률 계산
                const goalAmount = details[2];
                const currentAmount = details[3];
                let progress = 0;
                if (goalAmount > 0) {
                    progress = (Number(currentAmount) * 100) / Number(goalAmount);
                }
                console.log(`   진행률: ${progress.toFixed(2)}%`);
                
                // 남은 시간 계산
                const deadline = Number(details[4]);
                const now = Math.floor(Date.now() / 1000);
                if (now < deadline) {
                    const remaining = deadline - now;
                    const days = Math.floor(remaining / (24 * 60 * 60));
                    const hours = Math.floor((remaining % (24 * 60 * 60)) / (60 * 60));
                    console.log(`   남은 시간: ${days}일 ${hours}시간`);
                } else {
                    console.log(`   남은 시간: 만료됨`);
                }
                
                // Etherscan 링크
                const network = await ethers.provider.getNetwork();
                if (network.chainId === 11155111n) {
                    console.log(`   🔍 Etherscan: https://sepolia.etherscan.io/address/${campaignAddress}`);
                }
                
                // 기부자 정보 (기부자가 있는 경우)
                if (details[7] > 0) {
                    console.log("   💰 기부자 정보:");
                    await showDonorInfo(campaignAddress);
                }
                
            } catch (error) {
                console.log(`   ❌ 캠페인 정보 조회 실패: ${error.message}`);
            }
        }
        
        // 통계 요약
        console.log("\n" + "=".repeat(80));
        console.log("📈 통계 요약");
        console.log("=".repeat(80));
        
        let totalGoalAmount = 0n;
        let totalCurrentAmount = 0n;
        let totalDonors = 0n;
        let completedCampaigns = 0;
        
        for (const campaignAddress of allCampaigns) {
            try {
                const details = await factory.getCampaignDetails(campaignAddress);
                totalGoalAmount += details[2];
                totalCurrentAmount += details[3];
                totalDonors += details[7];
                
                if (details[6] === 1) { // Completed status
                    completedCampaigns++;
                }
            } catch (error) {
                // 에러 무시하고 계속
            }
        }
        
        console.log(`총 목표 금액: ${ethers.formatEther(totalGoalAmount)} ETH`);
        console.log(`총 모금 금액: ${ethers.formatEther(totalCurrentAmount)} ETH`);
        console.log(`총 기부자 수: ${totalDonors.toString()}명`);
        console.log(`완료된 캠페인: ${completedCampaigns}개`);
        
        if (totalGoalAmount > 0n) {
            const overallProgress = (Number(totalCurrentAmount) * 100) / Number(totalGoalAmount);
            console.log(`전체 진행률: ${overallProgress.toFixed(2)}%`);
        }
        
    } catch (error) {
        console.error("❌ 캠페인 확인 실패:", error.message);
        
        if (error.code === 'CALL_EXCEPTION') {
            console.log("💡 해결책: 팩토리 주소가 올바른지 확인하고, 네트워크 연결을 확인하세요.");
        }
        
        throw error;
    }
}

async function showDonorInfo(campaignAddress) {
    try {
        const DonationCampaign = await ethers.getContractFactory("DonationCampaign");
        const campaign = DonationCampaign.attach(campaignAddress);
        
        const donors = await campaign.getDonors();
        
        for (let i = 0; i < Math.min(donors.length, 5); i++) { // 최대 5명까지만 표시
            const donorAddress = donors[i];
            const donationAmount = await campaign.getDonationAmount(donorAddress);
            console.log(`     ${i + 1}. ${donorAddress}: ${ethers.formatEther(donationAmount)} ETH`);
        }
        
        if (donors.length > 5) {
            console.log(`     ... 및 ${donors.length - 5}명의 추가 기부자`);
        }
        
    } catch (error) {
        console.log(`     ❌ 기부자 정보 조회 실패: ${error.message}`);
    }
}

function getCampaignStatusText(status) {
    const statusTexts = {
        0: '🟢 활성',
        1: '✅ 완료',
        2: '⏰ 만료',
        3: '💸 출금완료'
    };
    return statusTexts[status] || '❓ 알 수 없음';
}

async function checkSpecificCampaign(campaignAddress) {
    console.log(`🔍 캠페인 ${campaignAddress} 상세 정보 확인 중...\n`);
    
    try {
        const DonationCampaign = await ethers.getContractFactory("DonationCampaign");
        const campaign = DonationCampaign.attach(campaignAddress);
        
        // 캠페인 정보 조회
        const info = await campaign.getCampaignInfo();
        
        console.log("=== 캠페인 상세 정보 ===");
        console.log("제목:", info[0]);
        console.log("설명:", info[1]);
        console.log("목표 금액:", ethers.formatEther(info[2]), "ETH");
        console.log("현재 금액:", ethers.formatEther(info[3]), "ETH");
        console.log("마감일:", new Date(Number(info[4]) * 1000).toLocaleString());
        console.log("수혜자:", info[5]);
        console.log("상태:", getCampaignStatusText(info[6]));
        console.log("기부자 수:", info[7].toString(), "명");
        
        // 추가 정보
        const progress = await campaign.getProgressPercentage();
        console.log("진행률:", (Number(progress) / 100).toFixed(2) + "%");
        
        const remainingTime = await campaign.getRemainingTime();
        if (remainingTime > 0) {
            const days = Math.floor(Number(remainingTime) / (24 * 60 * 60));
            const hours = Math.floor((Number(remainingTime) % (24 * 60 * 60)) / (60 * 60));
            console.log("남은 시간:", days + "일 " + hours + "시간");
        } else {
            console.log("남은 시간: 만료됨");
        }
        
        // 기부자 목록
        if (Number(info[7]) > 0) {
            console.log("\n=== 기부자 목록 ===");
            const donors = await campaign.getDonors();
            
            for (let i = 0; i < donors.length; i++) {
                const donorAddress = donors[i];
                const donationAmount = await campaign.getDonationAmount(donorAddress);
                console.log(`${i + 1}. ${donorAddress}: ${ethers.formatEther(donationAmount)} ETH`);
            }
        }
        
        // 컨트랙트 잔액
        const balance = await ethers.provider.getBalance(campaignAddress);
        console.log("\n컨트랙트 잔액:", ethers.formatEther(balance), "ETH");
        
        return true;
        
    } catch (error) {
        console.error("❌ 캠페인 확인 실패:", error.message);
        return false;
    }
}

// 스크립트 직접 실행 시
if (require.main === module) {
    // 명령행 인수로 특정 캠페인 주소가 제공된 경우
    const campaignAddress = process.argv[2];
    
    if (campaignAddress && ethers.isAddress(campaignAddress)) {
        checkSpecificCampaign(campaignAddress)
            .then(() => {
                console.log("\n✅ 캠페인 확인 완료!");
                process.exit(0);
            })
            .catch((error) => {
                console.error("\n💥 캠페인 확인 중 오류 발생:", error);
                process.exit(1);
            });
    } else {
        checkCampaigns()
            .then(() => {
                console.log("\n✅ 모든 캠페인 확인 완료!");
                process.exit(0);
            })
            .catch((error) => {
                console.error("\n💥 캠페인 확인 중 오류 발생:", error);
                process.exit(1);
            });
    }
}

module.exports = { checkCampaigns, checkSpecificCampaign };