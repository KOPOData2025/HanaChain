const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("HanaChainFactory", function () {
    let HanaChainFactory, MockUSDC;
    let factory, usdc;
    let owner, beneficiary, creator1, creator2, others;

    const GOAL_AMOUNT = ethers.parseUnits("1000", 6); // 1000 USDC (6 decimals)
    const DURATION_DAYS = 30;
    const TITLE = "Test HanaChain Campaign";
    const DESCRIPTION = "This is a test USDC-based campaign";

    beforeEach(async function () {
        [owner, beneficiary, creator1, creator2, ...others] = await ethers.getSigners();

        // MockUSDC 배포
        MockUSDC = await ethers.getContractFactory("MockUSDC");
        usdc = await MockUSDC.deploy();
        await usdc.waitForDeployment();

        // HanaChainFactory 배포
        HanaChainFactory = await ethers.getContractFactory("HanaChainFactory");
        factory = await HanaChainFactory.deploy(await usdc.getAddress());
        await factory.waitForDeployment();
    });

    describe("배포", function () {
        it("초기 상태가 올바르게 설정되어야 함", async function () {
            expect(await factory.totalCampaigns()).to.equal(0);
            expect(await factory.getUSDCToken()).to.equal(await usdc.getAddress());

            const allCampaigns = await factory.getAllCampaigns();
            expect(allCampaigns.length).to.equal(0);

            const activeCampaigns = await factory.getActiveCampaigns();
            expect(activeCampaigns.length).to.equal(0);
        });

        it("잘못된 USDC 주소로 배포 시 실패해야 함", async function () {
            await expect(
                HanaChainFactory.deploy(ethers.ZeroAddress)
            ).to.be.revertedWith("Invalid USDC token address");
        });
    });

    describe("캠페인 생성", function () {
        it("새로운 USDC 기반 캠페인을 성공적으로 생성해야 함", async function () {
            await expect(
                factory.connect(creator1).createCampaign(
                    TITLE,
                    DESCRIPTION,
                    GOAL_AMOUNT,
                    DURATION_DAYS,
                    beneficiary.address
                )
            ).to.emit(factory, "CampaignCreated");

            expect(await factory.totalCampaigns()).to.equal(1);

            const allCampaigns = await factory.getAllCampaigns();
            expect(allCampaigns.length).to.equal(1);

            const campaignAddress = allCampaigns[0];
            expect(await factory.isValidCampaign(campaignAddress)).to.be.true;
        });

        it("생성된 캠페인의 정보가 올바르게 저장되어야 함", async function () {
            const tx = await factory.connect(creator1).createCampaign(
                TITLE,
                DESCRIPTION,
                GOAL_AMOUNT,
                DURATION_DAYS,
                beneficiary.address
            );

            const receipt = await tx.wait();
            const campaignCreatedEvent = receipt.logs.find(
                log => log.fragment && log.fragment.name === 'CampaignCreated'
            );

            const campaignAddress = campaignCreatedEvent.args[0];

            // 팩토리에서 정보 조회
            const factoryInfo = await factory.getCampaignInfo(campaignAddress);
            expect(factoryInfo[0]).to.equal(TITLE); // title
            expect(factoryInfo[1]).to.equal(creator1.address); // creator
            expect(factoryInfo[2]).to.equal(GOAL_AMOUNT); // goalAmount
            expect(factoryInfo[5]).to.equal(true); // isActive

            // 실제 캠페인 컨트랙트에서 정보 조회
            const campaignDetails = await factory.getCampaignDetails(campaignAddress);
            expect(campaignDetails[0]).to.equal(TITLE);
            expect(campaignDetails[1]).to.equal(DESCRIPTION);
            expect(campaignDetails[2]).to.equal(GOAL_AMOUNT);
            expect(campaignDetails[5]).to.equal(beneficiary.address);
        });

        it("생성자별 캠페인 목록이 올바르게 관리되어야 함", async function () {
            // creator1이 2개 캠페인 생성
            await factory.connect(creator1).createCampaign(
                TITLE + " 1",
                DESCRIPTION,
                GOAL_AMOUNT,
                DURATION_DAYS,
                beneficiary.address
            );

            await factory.connect(creator1).createCampaign(
                TITLE + " 2",
                DESCRIPTION,
                GOAL_AMOUNT,
                DURATION_DAYS,
                beneficiary.address
            );

            // creator2가 1개 캠페인 생성
            await factory.connect(creator2).createCampaign(
                TITLE + " 3",
                DESCRIPTION,
                GOAL_AMOUNT,
                DURATION_DAYS,
                beneficiary.address
            );

            const creator1Campaigns = await factory.getCampaignsByCreator(creator1.address);
            const creator2Campaigns = await factory.getCampaignsByCreator(creator2.address);

            expect(creator1Campaigns.length).to.equal(2);
            expect(creator2Campaigns.length).to.equal(1);
            expect(await factory.totalCampaigns()).to.equal(3);
        });

        it("잘못된 매개변수로 생성 시 실패해야 함", async function () {
            // 빈 제목
            await expect(
                factory.createCampaign("", DESCRIPTION, GOAL_AMOUNT, DURATION_DAYS, beneficiary.address)
            ).to.be.revertedWith("Title cannot be empty");

            // 빈 설명
            await expect(
                factory.createCampaign(TITLE, "", GOAL_AMOUNT, DURATION_DAYS, beneficiary.address)
            ).to.be.revertedWith("Description cannot be empty");

            // 목표 금액 0
            await expect(
                factory.createCampaign(TITLE, DESCRIPTION, 0, DURATION_DAYS, beneficiary.address)
            ).to.be.revertedWith("Goal amount must be greater than 0");

            // 기간 0
            await expect(
                factory.createCampaign(TITLE, DESCRIPTION, GOAL_AMOUNT, 0, beneficiary.address)
            ).to.be.revertedWith("Invalid duration");

            // 기간 초과 (365일 초과)
            await expect(
                factory.createCampaign(TITLE, DESCRIPTION, GOAL_AMOUNT, 400, beneficiary.address)
            ).to.be.revertedWith("Invalid duration");

            // 잘못된 수혜자 주소
            await expect(
                factory.createCampaign(TITLE, DESCRIPTION, GOAL_AMOUNT, DURATION_DAYS, ethers.ZeroAddress)
            ).to.be.revertedWith("Invalid beneficiary address");
        });
    });

    describe("캠페인 관리", function () {
        let campaignAddress;

        beforeEach(async function () {
            const tx = await factory.connect(creator1).createCampaign(
                TITLE,
                DESCRIPTION,
                GOAL_AMOUNT,
                DURATION_DAYS,
                beneficiary.address
            );

            const receipt = await tx.wait();
            const campaignCreatedEvent = receipt.logs.find(
                log => log.fragment && log.fragment.name === 'CampaignCreated'
            );

            campaignAddress = campaignCreatedEvent.args[0];
        });

        it("생성자가 캠페인을 비활성화할 수 있어야 함", async function () {
            await expect(factory.connect(creator1).deactivateCampaign(campaignAddress))
                .to.emit(factory, "CampaignDeactivated")
                .withArgs(campaignAddress, await ethers.provider.getBlock('latest').then(b => b.timestamp + 1));

            const info = await factory.getCampaignInfo(campaignAddress);
            expect(info[5]).to.equal(false); // isActive
        });

        it("생성자가 아닌 계정은 캠페인을 비활성화할 수 없어야 함", async function () {
            await expect(factory.connect(creator2).deactivateCampaign(campaignAddress))
                .to.be.revertedWith("Only creator can deactivate");
        });

        it("이미 비활성화된 캠페인은 다시 비활성화할 수 없어야 함", async function () {
            await factory.connect(creator1).deactivateCampaign(campaignAddress);

            await expect(factory.connect(creator1).deactivateCampaign(campaignAddress))
                .to.be.revertedWith("Campaign already inactive");
        });

        it("활성 캠페인 목록이 올바르게 필터링되어야 함", async function () {
            // 추가 캠페인 생성
            await factory.connect(creator1).createCampaign(
                TITLE + " 2",
                DESCRIPTION,
                GOAL_AMOUNT,
                DURATION_DAYS,
                beneficiary.address
            );

            await factory.connect(creator2).createCampaign(
                TITLE + " 3",
                DESCRIPTION,
                GOAL_AMOUNT,
                DURATION_DAYS,
                beneficiary.address
            );

            // 전체 캠페인 3개
            expect(await factory.totalCampaigns()).to.equal(3);

            let activeCampaigns = await factory.getActiveCampaigns();
            expect(activeCampaigns.length).to.equal(3);

            // 하나 비활성화
            await factory.connect(creator1).deactivateCampaign(campaignAddress);

            activeCampaigns = await factory.getActiveCampaigns();
            expect(activeCampaigns.length).to.equal(2);
            expect(activeCampaigns).to.not.include(campaignAddress);
        });
    });

    describe("정보 조회", function () {
        let campaignAddress1, campaignAddress2;

        beforeEach(async function () {
            // 두 개의 캠페인 생성
            let tx = await factory.connect(creator1).createCampaign(
                TITLE + " 1",
                DESCRIPTION + " 1",
                ethers.parseUnits("500", 6), // 500 USDC
                20,
                beneficiary.address
            );

            let receipt = await tx.wait();
            let event = receipt.logs.find(log => log.fragment && log.fragment.name === 'CampaignCreated');
            campaignAddress1 = event.args[0];

            tx = await factory.connect(creator2).createCampaign(
                TITLE + " 2",
                DESCRIPTION + " 2",
                ethers.parseUnits("1500", 6), // 1500 USDC
                40,
                beneficiary.address
            );

            receipt = await tx.wait();
            event = receipt.logs.find(log => log.fragment && log.fragment.name === 'CampaignCreated');
            campaignAddress2 = event.args[0];
        });

        it("모든 캠페인 목록을 올바르게 반환해야 함", async function () {
            const allCampaigns = await factory.getAllCampaigns();
            expect(allCampaigns.length).to.equal(2);
            expect(allCampaigns).to.include(campaignAddress1);
            expect(allCampaigns).to.include(campaignAddress2);
        });

        it("캠페인 상세 정보를 올바르게 반환해야 함", async function () {
            const details1 = await factory.getCampaignDetails(campaignAddress1);
            expect(details1[0]).to.equal(TITLE + " 1");
            expect(details1[1]).to.equal(DESCRIPTION + " 1");
            expect(details1[2]).to.equal(ethers.parseUnits("500", 6));

            const details2 = await factory.getCampaignDetails(campaignAddress2);
            expect(details2[0]).to.equal(TITLE + " 2");
            expect(details2[1]).to.equal(DESCRIPTION + " 2");
            expect(details2[2]).to.equal(ethers.parseUnits("1500", 6));
        });

        it("유효하지 않은 캠페인 주소에 대해 false를 반환해야 함", async function () {
            const randomAddress = ethers.Wallet.createRandom().address;
            expect(await factory.isValidCampaign(randomAddress)).to.be.false;
        });

        it("생성자별 캠페인 목록을 올바르게 반환해야 함", async function () {
            const creator1Campaigns = await factory.getCampaignsByCreator(creator1.address);
            const creator2Campaigns = await factory.getCampaignsByCreator(creator2.address);

            expect(creator1Campaigns.length).to.equal(1);
            expect(creator1Campaigns[0]).to.equal(campaignAddress1);

            expect(creator2Campaigns.length).to.equal(1);
            expect(creator2Campaigns[0]).to.equal(campaignAddress2);
        });

        it("USDC 토큰 주소를 올바르게 반환해야 함", async function () {
            const usdcAddress = await factory.getUSDCToken();
            expect(usdcAddress).to.equal(await usdc.getAddress());
        });
    });

    describe("USDC 기반 캠페인 통합 테스트", function () {
        let campaignAddress;
        let donor1, donor2;

        beforeEach(async function () {
            [, , , , donor1, donor2] = await ethers.getSigners();

            // 캠페인 생성
            const tx = await factory.connect(creator1).createCampaign(
                TITLE,
                DESCRIPTION,
                GOAL_AMOUNT,
                DURATION_DAYS,
                beneficiary.address
            );

            const receipt = await tx.wait();
            const event = receipt.logs.find(log => log.fragment && log.fragment.name === 'CampaignCreated');
            campaignAddress = event.args[0];

            // 기부자들에게 USDC 발급
            await usdc.faucet(donor1.address, ethers.parseUnits("1000", 6));
            await usdc.faucet(donor2.address, ethers.parseUnits("1000", 6));
        });

        it("생성된 캠페인이 USDC 기반으로 작동해야 함", async function () {
            const HanaChainCampaign = await ethers.getContractFactory("HanaChainCampaign");
            const campaign = HanaChainCampaign.attach(campaignAddress);

            // USDC 토큰 주소 확인
            expect(await campaign.usdcToken()).to.equal(await usdc.getAddress());

            // 기부 테스트
            const donationAmount = ethers.parseUnits("100", 6); // 100 USDC

            // USDC 승인
            await usdc.connect(donor1).approve(campaignAddress, donationAmount);

            // 기부 실행 (캠페인 ID는 항상 1)
            await campaign.connect(donor1).donate(1, donationAmount);

            // 기부 확인
            const donorAmount = await campaign.getDonationAmount(1, donor1.address);
            expect(donorAmount).to.equal(donationAmount);

            // 캠페인 상세 정보에서 총 모금액 확인
            const details = await factory.getCampaignDetails(campaignAddress);
            expect(details[3]).to.equal(donationAmount); // totalRaised
        });
    });
});
