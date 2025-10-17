const { ethers } = require("hardhat");

async function main() {
    const contractAddress = "0xc4e6726cd8083ed88b0e521e9a8ef3a8701fbb32";
    const campaignId = 1;
    
    console.log("=".repeat(70));
    console.log("Campaign Existence Check");
    console.log("=".repeat(70));
    console.log("Contract Address:", contractAddress);
    console.log("Campaign ID:", campaignId);
    console.log("");
    
    // HanaChainCampaign ABI (getCampaignDetails function)
    const abi = [
        "function getCampaignDetails(uint256 _campaignId) external view returns (bool exists, address beneficiary, uint256 goalAmount, uint256 deadline, uint256 totalRaised, bool finalized, string memory title, string memory description)"
    ];
    
    const provider = ethers.provider;
    const contract = new ethers.Contract(contractAddress, abi, provider);
    
    try {
        const details = await contract.getCampaignDetails(campaignId);
        
        console.log("✅ Campaign Details:");
        console.log("   Exists:", details.exists);
        console.log("   Beneficiary:", details.beneficiary);
        console.log("   Goal Amount:", ethers.formatUnits(details.goalAmount, 6), "USDC");
        console.log("   Total Raised:", ethers.formatUnits(details.totalRaised, 6), "USDC");
        console.log("   Deadline:", new Date(Number(details.deadline) * 1000).toLocaleString());
        console.log("   Finalized:", details.finalized);
        console.log("   Title:", details.title);
        console.log("");
        
        if (!details.exists) {
            console.log("❌ PROBLEM: Campaign does not exist!");
            console.log("   The donate() transaction will fail with 'Campaign does not exist'");
        } else if (details.finalized) {
            console.log("❌ PROBLEM: Campaign is already finalized!");
            console.log("   The donate() transaction will fail with 'Campaign already finalized'");
        } else if (Number(details.deadline) < Date.now() / 1000) {
            console.log("❌ PROBLEM: Campaign deadline has passed!");
            console.log("   The donate() transaction will fail with 'Campaign deadline passed'");
        } else {
            console.log("✅ Campaign is active and ready to receive donations");
        }
        
    } catch (error) {
        console.log("❌ Error querying campaign:");
        console.log("   ", error.message);
        
        if (error.message.includes("revert")) {
            console.log("");
            console.log("⚠️  This likely means:");
            console.log("   1. The contract does not exist at this address, OR");
            console.log("   2. The contract is not a HanaChainCampaign contract, OR");
            console.log("   3. Campaign ID 1 does not exist in this contract");
        }
    }
    
    console.log("=".repeat(70));
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
