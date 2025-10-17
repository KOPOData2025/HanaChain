const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("MockUSDC", function () {
  let MockUSDC;
  let mockUSDC;
  let owner;
  let addr1;
  let addr2;

  beforeEach(async function () {
    // Get signers
    [owner, addr1, addr2] = await ethers.getSigners();

    // Deploy MockUSDC
    MockUSDC = await ethers.getContractFactory("MockUSDC");
    mockUSDC = await MockUSDC.deploy();
    await mockUSDC.waitForDeployment();
  });

  describe("Deployment", function () {
    it("Should have correct name and symbol", async function () {
      expect(await mockUSDC.name()).to.equal("Mock USD Coin");
      expect(await mockUSDC.symbol()).to.equal("USDC");
    });

    it("Should have 6 decimals (USDC standard)", async function () {
      expect(await mockUSDC.decimals()).to.equal(6);
    });

    it("Should mint initial supply to deployer", async function () {
      const ownerBalance = await mockUSDC.balanceOf(owner.address);
      expect(ownerBalance).to.equal(ethers.parseUnits("1000000", 6));
    });

    it("Should set the right owner", async function () {
      expect(await mockUSDC.owner()).to.equal(owner.address);
    });
  });

  describe("Faucet", function () {
    it("Should allow users to request any amount of USDC (unlimited for testing)", async function () {
      const faucetAmount = ethers.parseUnits("100", 6);

      await mockUSDC.faucet(addr1.address, faucetAmount);
      expect(await mockUSDC.balanceOf(addr1.address)).to.equal(faucetAmount);
    });

    it("Should allow large amounts without restriction (testnet mode)", async function () {
      const largeAmount = ethers.parseUnits("1000000", 6); // 1M USDC

      await mockUSDC.faucet(addr1.address, largeAmount);
      expect(await mockUSDC.balanceOf(addr1.address)).to.equal(largeAmount);
    });

    it("Should allow multiple immediate faucet requests (no cooldown)", async function () {
      const faucetAmount = ethers.parseUnits("100", 6);

      // First request
      await mockUSDC.faucet(addr1.address, faucetAmount);

      // Second immediate request should also succeed
      await mockUSDC.faucet(addr1.address, faucetAmount);

      expect(await mockUSDC.balanceOf(addr1.address)).to.equal(
        ethers.parseUnits("200", 6)
      );
    });

    it("Should reject zero amount faucet request", async function () {
      await expect(
        mockUSDC.faucet(addr1.address, 0)
      ).to.be.revertedWith("MockUSDC: Amount must be greater than 0");
    });

    it("Should allow anyone to mint USDC for testing", async function () {
      const amount1 = ethers.parseUnits("500", 6);
      const amount2 = ethers.parseUnits("750", 6);

      await mockUSDC.faucet(addr1.address, amount1);
      await mockUSDC.faucet(addr2.address, amount2);

      expect(await mockUSDC.balanceOf(addr1.address)).to.equal(amount1);
      expect(await mockUSDC.balanceOf(addr2.address)).to.equal(amount2);
    });
  });

  describe("Owner functions", function () {
    it("Should allow owner to mint additional tokens", async function () {
      const mintAmount = ethers.parseUnits("1000", 6);
      
      await mockUSDC.mint(addr1.address, mintAmount);
      expect(await mockUSDC.balanceOf(addr1.address)).to.equal(mintAmount);
    });

    it("Should prevent non-owners from minting", async function () {
      const mintAmount = ethers.parseUnits("1000", 6);
      
      await expect(
        mockUSDC.connect(addr1).mint(addr2.address, mintAmount)
      ).to.be.revertedWithCustomError(mockUSDC, "OwnableUnauthorizedAccount");
    });
  });

  describe("ERC20 functionality", function () {
    it("Should transfer tokens between accounts", async function () {
      const transferAmount = ethers.parseUnits("100", 6);
      
      // Transfer from owner to addr1
      await mockUSDC.transfer(addr1.address, transferAmount);
      expect(await mockUSDC.balanceOf(addr1.address)).to.equal(transferAmount);
      
      // Transfer from addr1 to addr2
      await mockUSDC.connect(addr1).transfer(addr2.address, transferAmount);
      expect(await mockUSDC.balanceOf(addr1.address)).to.equal(0);
      expect(await mockUSDC.balanceOf(addr2.address)).to.equal(transferAmount);
    });

    it("Should approve and transferFrom", async function () {
      const approveAmount = ethers.parseUnits("100", 6);
      
      // Owner approves addr1 to spend
      await mockUSDC.approve(addr1.address, approveAmount);
      
      // addr1 transfers from owner to addr2
      await mockUSDC.connect(addr1).transferFrom(
        owner.address,
        addr2.address,
        approveAmount
      );
      
      expect(await mockUSDC.balanceOf(addr2.address)).to.equal(approveAmount);
    });
  });

  describe("Faucet cooldown tracking (utility function only)", function () {
    it("Should return 0 cooldown for all addresses (cooldown not enforced)", async function () {
      // Even for address that never used faucet
      let cooldown = await mockUSDC.getFaucetCooldown(addr1.address);
      expect(cooldown).to.equal(0);

      // Even after using faucet (no cooldown enforcement in testnet mode)
      const faucetAmount = ethers.parseUnits("100", 6);
      await mockUSDC.faucet(addr1.address, faucetAmount);

      cooldown = await mockUSDC.getFaucetCooldown(addr1.address);
      expect(cooldown).to.equal(0);
    });

    it("Should allow checking cooldown utility function exists", async function () {
      // Verify the function exists and is callable
      const cooldown = await mockUSDC.getFaucetCooldown(addr2.address);
      expect(cooldown).to.be.a('bigint');
    });
  });
});