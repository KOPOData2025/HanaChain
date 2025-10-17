const { expect } = require("chai");
const { ethers } = require("hardhat");
const { time } = require("@nomicfoundation/hardhat-network-helpers");

describe("HanaChainCampaign", function () {
  let MockUSDC;
  let mockUSDC;
  let HanaChainCampaign;
  let hanaChainCampaign;
  let owner;
  let beneficiary;
  let donor1;
  let donor2;
  let platformFeeRecipient;

  const INITIAL_USDC_SUPPLY = ethers.parseUnits("1000000", 6); // 1M USDC
  const CAMPAIGN_GOAL = ethers.parseUnits("10000", 6); // 10K USDC
  const DONATION_AMOUNT = ethers.parseUnits("100", 6); // 100 USDC
  const CAMPAIGN_DURATION = 30 * 24 * 60 * 60; // 30 days

  beforeEach(async function () {
    [owner, beneficiary, donor1, donor2, platformFeeRecipient] = await ethers.getSigners();

    // Deploy MockUSDC
    MockUSDC = await ethers.getContractFactory("MockUSDC");
    mockUSDC = await MockUSDC.deploy();
    await mockUSDC.waitForDeployment();

    // Deploy HanaChainCampaign
    HanaChainCampaign = await ethers.getContractFactory("HanaChainCampaign");
    hanaChainCampaign = await HanaChainCampaign.deploy(await mockUSDC.getAddress());
    await hanaChainCampaign.waitForDeployment();

    // Setup USDC for testing
    // Transfer USDC to donors
    await mockUSDC.transfer(donor1.address, ethers.parseUnits("5000", 6));
    await mockUSDC.transfer(donor2.address, ethers.parseUnits("5000", 6));

    // Approve campaign contract to spend USDC
    await mockUSDC.connect(donor1).approve(await hanaChainCampaign.getAddress(), ethers.parseUnits("5000", 6));
    await mockUSDC.connect(donor2).approve(await hanaChainCampaign.getAddress(), ethers.parseUnits("5000", 6));
  });

  describe("Deployment", function () {
    it("Should set the correct USDC token address", async function () {
      expect(await hanaChainCampaign.usdcToken()).to.equal(await mockUSDC.getAddress());
    });

    it("Should set the correct owner", async function () {
      expect(await hanaChainCampaign.owner()).to.equal(owner.address);
    });

    it("Should set platform fee recipient to owner", async function () {
      expect(await hanaChainCampaign.platformFeeRecipient()).to.equal(owner.address);
    });

    it("Should set default platform fee to 250 basis points (2.5%)", async function () {
      expect(await hanaChainCampaign.platformFee()).to.equal(250);
    });

    it("Should start with nextCampaignId as 1", async function () {
      expect(await hanaChainCampaign.nextCampaignId()).to.equal(1);
    });
  });

  describe("Campaign Creation", function () {
    it("Should create a campaign successfully", async function () {
      const tx = await hanaChainCampaign.createCampaign(
        beneficiary.address,
        CAMPAIGN_GOAL,
        CAMPAIGN_DURATION,
        "Test Campaign",
        "This is a test campaign"
      );

      const receipt = await tx.wait();
      const event = receipt.logs.find(log => {
        try {
          return hanaChainCampaign.interface.parseLog(log).name === 'CampaignCreated';
        } catch {
          return false;
        }
      });
      
      expect(event).to.not.be.undefined;
      const parsedEvent = hanaChainCampaign.interface.parseLog(event);
      expect(parsedEvent.args.campaignId).to.equal(1);
      expect(parsedEvent.args.beneficiary).to.equal(beneficiary.address);
      expect(parsedEvent.args.goalAmount).to.equal(CAMPAIGN_GOAL);
    });

    it("Should increment campaign ID for multiple campaigns", async function () {
      await hanaChainCampaign.createCampaign(
        beneficiary.address,
        CAMPAIGN_GOAL,
        CAMPAIGN_DURATION,
        "Campaign 1",
        "First campaign"
      );

      await hanaChainCampaign.createCampaign(
        beneficiary.address,
        CAMPAIGN_GOAL,
        CAMPAIGN_DURATION,
        "Campaign 2",
        "Second campaign"
      );

      expect(await hanaChainCampaign.nextCampaignId()).to.equal(3);
    });

    it("Should reject campaign with invalid beneficiary", async function () {
      await expect(
        hanaChainCampaign.createCampaign(
          ethers.ZeroAddress,
          CAMPAIGN_GOAL,
          CAMPAIGN_DURATION,
          "Test Campaign",
          "Description"
        )
      ).to.be.revertedWith("Invalid beneficiary address");
    });

    it("Should reject campaign with zero goal amount", async function () {
      await expect(
        hanaChainCampaign.createCampaign(
          beneficiary.address,
          0,
          CAMPAIGN_DURATION,
          "Test Campaign",
          "Description"
        )
      ).to.be.revertedWith("Goal amount must be greater than 0");
    });

    it("Should reject campaign with empty title", async function () {
      await expect(
        hanaChainCampaign.createCampaign(
          beneficiary.address,
          CAMPAIGN_GOAL,
          CAMPAIGN_DURATION,
          "",
          "Description"
        )
      ).to.be.revertedWith("Title cannot be empty");
    });

    it("Should reject campaign with invalid duration", async function () {
      await expect(
        hanaChainCampaign.createCampaign(
          beneficiary.address,
          CAMPAIGN_GOAL,
          0,
          "Test Campaign",
          "Description"
        )
      ).to.be.revertedWith("Invalid duration");
    });
  });

  describe("Campaign Information", function () {
    let campaignId;

    beforeEach(async function () {
      const tx = await hanaChainCampaign.createCampaign(
        beneficiary.address,
        CAMPAIGN_GOAL,
        CAMPAIGN_DURATION,
        "Test Campaign",
        "This is a test campaign"
      );
      const receipt = await tx.wait();
      const event = receipt.logs.find(log => {
        try {
          return hanaChainCampaign.interface.parseLog(log).name === 'CampaignCreated';
        } catch {
          return false;
        }
      });
      campaignId = hanaChainCampaign.interface.parseLog(event).args.campaignId;
    });

    it("Should return correct campaign information", async function () {
      const campaign = await hanaChainCampaign.getCampaign(campaignId);
      
      expect(campaign.id).to.equal(campaignId);
      expect(campaign.beneficiary).to.equal(beneficiary.address);
      expect(campaign.goalAmount).to.equal(CAMPAIGN_GOAL);
      expect(campaign.totalRaised).to.equal(0);
      expect(campaign.finalized).to.equal(false);
      expect(campaign.exists).to.equal(true);
      expect(campaign.title).to.equal("Test Campaign");
      expect(campaign.description).to.equal("This is a test campaign");
    });

    it("Should return all campaign IDs", async function () {
      // Create another campaign
      await hanaChainCampaign.createCampaign(
        beneficiary.address,
        CAMPAIGN_GOAL,
        CAMPAIGN_DURATION,
        "Campaign 2",
        "Second campaign"
      );

      const campaignIds = await hanaChainCampaign.getAllCampaignIds();
      expect(campaignIds.length).to.equal(2);
      expect(campaignIds[0]).to.equal(1);
      expect(campaignIds[1]).to.equal(2);
    });
  });

  describe("Donations", function () {
    let campaignId;

    beforeEach(async function () {
      const tx = await hanaChainCampaign.createCampaign(
        beneficiary.address,
        CAMPAIGN_GOAL,
        CAMPAIGN_DURATION,
        "Test Campaign",
        "This is a test campaign"
      );
      const receipt = await tx.wait();
      const event = receipt.logs.find(log => {
        try {
          return hanaChainCampaign.interface.parseLog(log).name === 'CampaignCreated';
        } catch {
          return false;
        }
      });
      campaignId = hanaChainCampaign.interface.parseLog(event).args.campaignId;
    });

    it("Should accept donations and update campaign total", async function () {
      await hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT);

      const campaign = await hanaChainCampaign.getCampaign(campaignId);
      expect(campaign.totalRaised).to.equal(DONATION_AMOUNT);

      const donationAmount = await hanaChainCampaign.getDonationAmount(campaignId, donor1.address);
      expect(donationAmount).to.equal(DONATION_AMOUNT);
    });

    it("Should emit DonationMade event", async function () {
      await expect(hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT))
        .to.emit(hanaChainCampaign, "DonationMade")
        .withArgs(campaignId, donor1.address, DONATION_AMOUNT);
    });

    it("Should handle multiple donations from same donor", async function () {
      await hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT);
      await hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT);

      const campaign = await hanaChainCampaign.getCampaign(campaignId);
      expect(campaign.totalRaised).to.equal(DONATION_AMOUNT * 2n);

      const donationAmount = await hanaChainCampaign.getDonationAmount(campaignId, donor1.address);
      expect(donationAmount).to.equal(DONATION_AMOUNT * 2n);
    });

    it("Should handle donations from multiple donors", async function () {
      await hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT);
      await hanaChainCampaign.connect(donor2).donate(campaignId, DONATION_AMOUNT * 2n);

      const campaign = await hanaChainCampaign.getCampaign(campaignId);
      expect(campaign.totalRaised).to.equal(DONATION_AMOUNT * 3n);

      const donors = await hanaChainCampaign.getCampaignDonors(campaignId);
      expect(donors.length).to.equal(2);
      expect(donors).to.include(donor1.address);
      expect(donors).to.include(donor2.address);
    });

    it("Should reject donations to non-existent campaigns", async function () {
      await expect(
        hanaChainCampaign.connect(donor1).donate(999, DONATION_AMOUNT)
      ).to.be.revertedWith("Campaign does not exist");
    });

    it("Should reject zero donations", async function () {
      await expect(
        hanaChainCampaign.connect(donor1).donate(campaignId, 0)
      ).to.be.revertedWith("Donation amount must be greater than 0");
    });

    it("Should reject donations after deadline", async function () {
      // Fast forward past deadline
      await time.increase(CAMPAIGN_DURATION + 1);

      await expect(
        hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT)
      ).to.be.revertedWith("Campaign deadline passed");
    });
  });

  describe("Campaign Finalization", function () {
    let campaignId;

    beforeEach(async function () {
      const tx = await hanaChainCampaign.createCampaign(
        beneficiary.address,
        CAMPAIGN_GOAL,
        CAMPAIGN_DURATION,
        "Test Campaign",
        "This is a test campaign"
      );
      const receipt = await tx.wait();
      const event = receipt.logs.find(log => {
        try {
          return hanaChainCampaign.interface.parseLog(log).name === 'CampaignCreated';
        } catch {
          return false;
        }
      });
      campaignId = hanaChainCampaign.interface.parseLog(event).args.campaignId;
    });

    it("Should finalize successful campaign after deadline", async function () {
      // Make donations
      await hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT);
      
      // Fast forward past deadline
      await time.increase(CAMPAIGN_DURATION + 1);

      // Set platform fee recipient
      await hanaChainCampaign.setPlatformFeeRecipient(platformFeeRecipient.address);

      const beneficiaryBalanceBefore = await mockUSDC.balanceOf(beneficiary.address);
      const platformBalanceBefore = await mockUSDC.balanceOf(platformFeeRecipient.address);

      await hanaChainCampaign.finalizeCampaign(campaignId);

      const beneficiaryBalanceAfter = await mockUSDC.balanceOf(beneficiary.address);
      const platformBalanceAfter = await mockUSDC.balanceOf(platformFeeRecipient.address);

      const expectedPlatformFee = DONATION_AMOUNT * 250n / 10000n; // 2.5%
      const expectedBeneficiaryAmount = DONATION_AMOUNT - expectedPlatformFee;

      expect(beneficiaryBalanceAfter - beneficiaryBalanceBefore).to.equal(expectedBeneficiaryAmount);
      expect(platformBalanceAfter - platformBalanceBefore).to.equal(expectedPlatformFee);

      const campaign = await hanaChainCampaign.getCampaign(campaignId);
      expect(campaign.finalized).to.equal(true);
    });

    it("Should finalize campaign when goal is reached before deadline", async function () {
      // Make donations equal to goal from multiple donors
      await hanaChainCampaign.connect(donor1).donate(campaignId, ethers.parseUnits("5000", 6));
      await hanaChainCampaign.connect(donor2).donate(campaignId, ethers.parseUnits("5000", 6));

      const canFinalize = await hanaChainCampaign.canFinalize(campaignId);
      expect(canFinalize).to.equal(true);

      await hanaChainCampaign.finalizeCampaign(campaignId);

      const campaign = await hanaChainCampaign.getCampaign(campaignId);
      expect(campaign.finalized).to.equal(true);
    });

    it("Should emit CampaignFinalized event", async function () {
      await hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT);
      await time.increase(CAMPAIGN_DURATION + 1);

      const expectedPlatformFee = DONATION_AMOUNT * 250n / 10000n;
      const expectedBeneficiaryAmount = DONATION_AMOUNT - expectedPlatformFee;

      await expect(hanaChainCampaign.finalizeCampaign(campaignId))
        .to.emit(hanaChainCampaign, "CampaignFinalized")
        .withArgs(campaignId, DONATION_AMOUNT, expectedPlatformFee, expectedBeneficiaryAmount);
    });

    it("Should reject finalization of already finalized campaign", async function () {
      await hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT);
      await time.increase(CAMPAIGN_DURATION + 1);
      
      await hanaChainCampaign.finalizeCampaign(campaignId);

      await expect(
        hanaChainCampaign.finalizeCampaign(campaignId)
      ).to.be.revertedWith("Campaign already finalized");
    });

    it("Should reject finalization of campaign with no funds", async function () {
      await time.increase(CAMPAIGN_DURATION + 1);

      await expect(
        hanaChainCampaign.finalizeCampaign(campaignId)
      ).to.be.revertedWith("No funds to distribute");
    });
  });

  describe("Campaign Cancellation and Refunds", function () {
    let campaignId;

    beforeEach(async function () {
      const tx = await hanaChainCampaign.createCampaign(
        beneficiary.address,
        CAMPAIGN_GOAL,
        CAMPAIGN_DURATION,
        "Test Campaign",
        "This is a test campaign"
      );
      const receipt = await tx.wait();
      const event = receipt.logs.find(log => {
        try {
          return hanaChainCampaign.interface.parseLog(log).name === 'CampaignCreated';
        } catch {
          return false;
        }
      });
      campaignId = hanaChainCampaign.interface.parseLog(event).args.campaignId;
    });

    it("Should allow beneficiary to cancel failed campaign and process refunds", async function () {
      const halfGoal = CAMPAIGN_GOAL / 2n;
      await hanaChainCampaign.connect(donor1).donate(campaignId, halfGoal);
      
      // Fast forward past deadline
      await time.increase(CAMPAIGN_DURATION + 1);

      const donor1BalanceBefore = await mockUSDC.balanceOf(donor1.address);

      await expect(hanaChainCampaign.connect(beneficiary).cancelCampaign(campaignId))
        .to.emit(hanaChainCampaign, "CampaignCancelled")
        .withArgs(campaignId, halfGoal);

      const donor1BalanceAfter = await mockUSDC.balanceOf(donor1.address);
      expect(donor1BalanceAfter - donor1BalanceBefore).to.equal(halfGoal);

      const campaign = await hanaChainCampaign.getCampaign(campaignId);
      expect(campaign.finalized).to.equal(true);
      expect(campaign.totalRaised).to.equal(0);
    });

    it("Should allow owner to cancel failed campaign", async function () {
      await hanaChainCampaign.connect(donor1).donate(campaignId, CAMPAIGN_GOAL / 2n);
      await time.increase(CAMPAIGN_DURATION + 1);

      await hanaChainCampaign.connect(owner).cancelCampaign(campaignId);

      const campaign = await hanaChainCampaign.getCampaign(campaignId);
      expect(campaign.finalized).to.equal(true);
    });

    it("Should reject cancellation by unauthorized user", async function () {
      await hanaChainCampaign.connect(donor1).donate(campaignId, CAMPAIGN_GOAL / 2n);
      await time.increase(CAMPAIGN_DURATION + 1);

      await expect(
        hanaChainCampaign.connect(donor2).cancelCampaign(campaignId)
      ).to.be.revertedWith("Only beneficiary or owner can cancel");
    });

    it("Should reject cancellation of successful campaign", async function () {
      // Make donations equal to goal from multiple donors
      await hanaChainCampaign.connect(donor1).donate(campaignId, ethers.parseUnits("5000", 6));
      await hanaChainCampaign.connect(donor2).donate(campaignId, ethers.parseUnits("5000", 6));
      await time.increase(CAMPAIGN_DURATION + 1);

      await expect(
        hanaChainCampaign.connect(beneficiary).cancelCampaign(campaignId)
      ).to.be.revertedWith("Campaign still active or goal reached");
    });
  });

  describe("Platform Fee Management", function () {
    it("Should allow owner to update platform fee", async function () {
      const newFee = 500; // 5%
      
      await expect(hanaChainCampaign.setPlatformFee(newFee))
        .to.emit(hanaChainCampaign, "PlatformFeeUpdated")
        .withArgs(250, newFee);

      expect(await hanaChainCampaign.platformFee()).to.equal(newFee);
    });

    it("Should reject platform fee exceeding maximum", async function () {
      const excessiveFee = 1001; // > 10%
      
      await expect(
        hanaChainCampaign.setPlatformFee(excessiveFee)
      ).to.be.revertedWith("Fee exceeds maximum");
    });

    it("Should allow owner to update platform fee recipient", async function () {
      await expect(hanaChainCampaign.setPlatformFeeRecipient(platformFeeRecipient.address))
        .to.emit(hanaChainCampaign, "PlatformFeeRecipientUpdated")
        .withArgs(owner.address, platformFeeRecipient.address);

      expect(await hanaChainCampaign.platformFeeRecipient()).to.equal(platformFeeRecipient.address);
    });

    it("Should reject invalid platform fee recipient", async function () {
      await expect(
        hanaChainCampaign.setPlatformFeeRecipient(ethers.ZeroAddress)
      ).to.be.revertedWith("Invalid recipient address");
    });

    it("Should reject fee updates from non-owner", async function () {
      await expect(
        hanaChainCampaign.connect(donor1).setPlatformFee(500)
      ).to.be.revertedWithCustomError(hanaChainCampaign, "OwnableUnauthorizedAccount");
    });
  });

  describe("Edge Cases and Security", function () {
    let campaignId;

    beforeEach(async function () {
      const tx = await hanaChainCampaign.createCampaign(
        beneficiary.address,
        CAMPAIGN_GOAL,
        CAMPAIGN_DURATION,
        "Test Campaign",
        "This is a test campaign"
      );
      const receipt = await tx.wait();
      const event = receipt.logs.find(log => {
        try {
          return hanaChainCampaign.interface.parseLog(log).name === 'CampaignCreated';
        } catch {
          return false;
        }
      });
      campaignId = hanaChainCampaign.interface.parseLog(event).args.campaignId;
    });

    it("Should handle donations with insufficient USDC allowance", async function () {
      // Reset allowance
      await mockUSDC.connect(donor1).approve(await hanaChainCampaign.getAddress(), 0);

      await expect(
        hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT)
      ).to.be.revertedWithCustomError(mockUSDC, "ERC20InsufficientAllowance");
    });

    it("Should handle donations with insufficient USDC balance", async function () {
      // First approve a large amount
      const excessiveAmount = ethers.parseUnits("10000", 6);
      await mockUSDC.connect(donor1).approve(await hanaChainCampaign.getAddress(), excessiveAmount);
      
      // Try to donate more than balance (donor1 has 5000 USDC)
      await expect(
        hanaChainCampaign.connect(donor1).donate(campaignId, excessiveAmount)
      ).to.be.revertedWithCustomError(mockUSDC, "ERC20InsufficientBalance");
    });

    it("Should return correct canFinalize status", async function () {
      // Before deadline, no goal reached
      let canFinalize = await hanaChainCampaign.canFinalize(campaignId);
      expect(canFinalize).to.equal(false);

      // Make donation but don't reach goal
      await hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT);
      canFinalize = await hanaChainCampaign.canFinalize(campaignId);
      expect(canFinalize).to.equal(false);

      // Fast forward past deadline
      await time.increase(CAMPAIGN_DURATION + 1);
      canFinalize = await hanaChainCampaign.canFinalize(campaignId);
      expect(canFinalize).to.equal(true);
    });

    it("Should handle zero platform fee correctly", async function () {
      await hanaChainCampaign.setPlatformFee(0);
      await hanaChainCampaign.connect(donor1).donate(campaignId, DONATION_AMOUNT);
      await time.increase(CAMPAIGN_DURATION + 1);

      const beneficiaryBalanceBefore = await mockUSDC.balanceOf(beneficiary.address);
      await hanaChainCampaign.finalizeCampaign(campaignId);
      const beneficiaryBalanceAfter = await mockUSDC.balanceOf(beneficiary.address);

      expect(beneficiaryBalanceAfter - beneficiaryBalanceBefore).to.equal(DONATION_AMOUNT);
    });
  });
});