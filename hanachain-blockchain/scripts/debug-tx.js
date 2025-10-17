const { ethers } = require("hardhat");

async function main() {
    const txHash = "0xc5ebe390fa8a347ea9f19bd6fb78c08f504af6989df11ced7d93db82ce6ef785";
    
    console.log("=".repeat(70));
    console.log("Transaction Debugging");
    console.log("=".repeat(70));
    console.log("");
    
    // Get transaction receipt
    const receipt = await ethers.provider.getTransactionReceipt(txHash);
    
    if (!receipt) {
        console.log("❌ Transaction not found");
        return;
    }
    
    console.log("📋 Transaction Receipt:");
    console.log("   Status:", receipt.status === 1 ? "✅ Success" : "❌ Failed");
    console.log("   Block:", receipt.blockNumber);
    console.log("   Gas Used:", receipt.gasUsed.toString());
    console.log("   Contract:", receipt.to);
    console.log("");
    
    // Get transaction details
    const tx = await ethers.provider.getTransaction(txHash);
    console.log("📝 Transaction Details:");
    console.log("   From:", tx.from);
    console.log("   To:", tx.to);
    console.log("   Value:", ethers.formatEther(tx.value), "ETH");
    console.log("   Gas Limit:", tx.gasLimit.toString());
    console.log("");
    
    // Try to get revert reason
    try {
        await ethers.provider.call(tx, tx.blockNumber);
    } catch (error) {
        console.log("🚨 Revert Reason:");
        console.log("   ", error.message);
        console.log("");
    }
    
    console.log("=".repeat(70));
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
