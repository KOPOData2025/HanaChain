const { ethers } = require("hardhat");

async function main() {
    const factoryEvent = "CampaignCreated(address,address,string,uint256,uint256,uint256)";
    const campaignEvent = "CampaignCreated(uint256,address,uint256,uint256,string)";

    const factoryHash = ethers.keccak256(ethers.toUtf8Bytes(factoryEvent));
    const campaignHash = ethers.keccak256(ethers.toUtf8Bytes(campaignEvent));

    console.log("=".repeat(70));
    console.log("Event Signature Hash Calculation");
    console.log("=".repeat(70));
    console.log("");
    console.log("1. Factory CampaignCreated:");
    console.log("   Signature:", factoryEvent);
    console.log("   Hash:     ", factoryHash);
    console.log("");
    console.log("2. HanaChainCampaign CampaignCreated:");
    console.log("   Signature:", campaignEvent);
    console.log("   Hash:     ", campaignHash);
    console.log("");
    console.log("3. 실제 로그에서 발견된 시그니처:");
    console.log("             ", "0x2e64da7e06e88ccd84d69eccb21119ca81e40e5648611e89edfe888c8fa62597");
    console.log("");
    console.log("Factory 일치:", factoryHash === "0x2e64da7e06e88ccd84d69eccb21119ca81e40e5648611e89edfe888c8fa62597");
    console.log("Campaign 일치:", campaignHash === "0x2e64da7e06e88ccd84d69eccb21119ca81e40e5648611e89edfe888c8fa62597");
    console.log("=".repeat(70));
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
