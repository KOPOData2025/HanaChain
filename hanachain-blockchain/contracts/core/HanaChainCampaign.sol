// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

/**
 * @title HanaChainCampaign
 * @dev USDC 통합을 갖춘 HanaChain 플랫폼용 캠페인 관리 컨트랙트
 */
contract HanaChainCampaign is Ownable, ReentrancyGuard {
    // USDC 토큰 인터페이스
    IERC20 public immutable usdcToken;

    // 캠페인 구조체
    struct Campaign {
        uint256 id;
        address payable beneficiary;
        uint256 goalAmount;
        uint256 totalRaised;
        uint256 deadline;
        bool finalized;
        bool exists;
        string title;
        string description;
    }
    
    // 캠페인 기부 추적
    struct DonationInfo {
        mapping(address => uint256) donations;  // 기부자 => 금액
        address[] donors;                       // 고유 기부자 목록
    }

    // 상태 변수
    mapping(uint256 => Campaign) public campaigns;
    mapping(uint256 => DonationInfo) private campaignDonations;
    uint256[] public campaignIds;
    uint256 public nextCampaignId = 1;

    // 플랫폼 수수료 (베이시스 포인트 단위, 예: 250 = 2.5%)
    uint256 public platformFee = 250;
    uint256 public constant MAX_PLATFORM_FEE = 1000; // 10%
    address public platformFeeRecipient;

    // 이벤트
    event CampaignCreated(
        uint256 indexed campaignId,
        address indexed beneficiary,
        uint256 goalAmount,
        uint256 deadline,
        string title
    );
    
    event DonationMade(
        uint256 indexed campaignId,
        address indexed donor,
        uint256 amount
    );
    
    event CampaignFinalized(
        uint256 indexed campaignId,
        uint256 totalRaised,
        uint256 platformFeeAmount,
        uint256 beneficiaryAmount
    );
    
    event CampaignCancelled(
        uint256 indexed campaignId,
        uint256 totalRefunded
    );
    
    event PlatformFeeUpdated(uint256 oldFee, uint256 newFee);
    event PlatformFeeRecipientUpdated(address oldRecipient, address newRecipient);
    
    // 수정자
    modifier campaignExists(uint256 _campaignId) {
        require(campaigns[_campaignId].exists, "Campaign does not exist");
        _;
    }
    
    modifier campaignActive(uint256 _campaignId) {
        require(campaigns[_campaignId].exists, "Campaign does not exist");
        require(!campaigns[_campaignId].finalized, "Campaign already finalized");
        require(block.timestamp <= campaigns[_campaignId].deadline, "Campaign deadline passed");
        _;
    }
    
    /**
     * @dev 생성자
     * @param _usdcToken USDC 토큰 컨트랙트 주소
     */
    constructor(address _usdcToken) Ownable(msg.sender) {
        require(_usdcToken != address(0), "Invalid USDC token address");
        usdcToken = IERC20(_usdcToken);
        platformFeeRecipient = msg.sender;
    }
    
    /**
     * @dev 새로운 캠페인 생성
     * @param _beneficiary 캠페인 성공 시 자금을 받을 주소
     * @param _goalAmount 모금 목표 금액 (USDC 최소 단위)
     * @param _duration 캠페인 기간 (초 단위)
     * @param _title 캠페인 제목
     * @param _description 캠페인 설명
     * @return campaignId 생성된 캠페인의 ID
     */
    function createCampaign(
        address payable _beneficiary,
        uint256 _goalAmount,
        uint256 _duration,
        string memory _title,
        string memory _description
    ) external returns (uint256 campaignId) {
        require(_beneficiary != address(0), "Invalid beneficiary address");
        require(_goalAmount > 0, "Goal amount must be greater than 0");
        require(_duration > 0 && _duration <= 365 days, "Invalid duration");
        require(bytes(_title).length > 0, "Title cannot be empty");
        
        campaignId = nextCampaignId++;
        uint256 deadline = block.timestamp + _duration;
        
        campaigns[campaignId] = Campaign({
            id: campaignId,
            beneficiary: _beneficiary,
            goalAmount: _goalAmount,
            totalRaised: 0,
            deadline: deadline,
            finalized: false,
            exists: true,
            title: _title,
            description: _description
        });
        
        campaignIds.push(campaignId);
        
        emit CampaignCreated(campaignId, _beneficiary, _goalAmount, deadline, _title);
    }
    
    /**
     * @dev 캠페인에 기부하기
     * @param _campaignId 기부할 캠페인 ID
     * @param _amount 기부할 USDC 금액
     */
    function donate(uint256 _campaignId, uint256 _amount) 
        external 
        nonReentrant 
        campaignActive(_campaignId) 
    {
        require(_amount > 0, "Donation amount must be greater than 0");

        // 기부자로부터 이 컨트랙트로 USDC 전송
        require(
            usdcToken.transferFrom(msg.sender, address(this), _amount),
            "USDC transfer failed"
        );

        Campaign storage campaign = campaigns[_campaignId];
        DonationInfo storage donationInfo = campaignDonations[_campaignId];

        // 신규 기부자 추적
        if (donationInfo.donations[msg.sender] == 0) {
            donationInfo.donors.push(msg.sender);
        }

        // 기부 기록 업데이트
        donationInfo.donations[msg.sender] += _amount;
        campaign.totalRaised += _amount;
        
        emit DonationMade(_campaignId, msg.sender, _amount);
    }
    
    /**
     * @dev 성공한 캠페인을 종료하고 자금 분배
     * @param _campaignId 종료할 캠페인 ID
     */
    function finalizeCampaign(uint256 _campaignId) 
        external 
        nonReentrant 
        campaignExists(_campaignId) 
    {
        Campaign storage campaign = campaigns[_campaignId];
        
        require(!campaign.finalized, "Campaign already finalized");
        require(
            block.timestamp > campaign.deadline || 
            campaign.totalRaised >= campaign.goalAmount,
            "Campaign still active and goal not reached"
        );
        require(campaign.totalRaised > 0, "No funds to distribute");
        
        campaign.finalized = true;
        
        uint256 totalRaised = campaign.totalRaised;
        uint256 feeAmount = (totalRaised * platformFee) / 10000;
        uint256 beneficiaryAmount = totalRaised - feeAmount;

        // 플랫폼 수수료 전송
        if (feeAmount > 0 && platformFeeRecipient != address(0)) {
            require(
                usdcToken.transfer(platformFeeRecipient, feeAmount),
                "Platform fee transfer failed"
            );
        }

        // 남은 자금을 수혜자에게 전송
        require(
            usdcToken.transfer(campaign.beneficiary, beneficiaryAmount),
            "Beneficiary transfer failed"
        );
        
        emit CampaignFinalized(_campaignId, totalRaised, feeAmount, beneficiaryAmount);
    }
    
    /**
     * @dev 실패한 캠페인을 취소하고 환불 활성화
     * @param _campaignId 취소할 캠페인 ID
     */
    function cancelCampaign(uint256 _campaignId) 
        external 
        nonReentrant 
        campaignExists(_campaignId) 
    {
        Campaign storage campaign = campaigns[_campaignId];
        
        require(!campaign.finalized, "Campaign already finalized");
        require(
            msg.sender == campaign.beneficiary || msg.sender == owner(),
            "Only beneficiary or owner can cancel"
        );
        require(
            block.timestamp > campaign.deadline && 
            campaign.totalRaised < campaign.goalAmount,
            "Campaign still active or goal reached"
        );
        
        campaign.finalized = true;

        // 모든 기부자에 대한 환불 처리
        DonationInfo storage donationInfo = campaignDonations[_campaignId];
        uint256 totalRefunded = 0;
        
        for (uint256 i = 0; i < donationInfo.donors.length; i++) {
            address donor = donationInfo.donors[i];
            uint256 amount = donationInfo.donations[donor];
            
            if (amount > 0) {
                donationInfo.donations[donor] = 0;
                require(
                    usdcToken.transfer(donor, amount),
                    "Refund transfer failed"
                );
                totalRefunded += amount;
            }
        }
        
        campaign.totalRaised = 0;
        
        emit CampaignCancelled(_campaignId, totalRefunded);
    }
    
    /**
     * @dev 캠페인 상세 정보 조회
     * @param _campaignId 캠페인 ID
     */
    function getCampaign(uint256 _campaignId) 
        external 
        view 
        campaignExists(_campaignId) 
        returns (Campaign memory) 
    {
        return campaigns[_campaignId];
    }
    
    /**
     * @dev 캠페인에서 특정 기부자의 기부 금액 조회
     * @param _campaignId 캠페인 ID
     * @param _donor 기부자 주소
     */
    function getDonationAmount(uint256 _campaignId, address _donor) 
        external 
        view 
        campaignExists(_campaignId) 
        returns (uint256) 
    {
        return campaignDonations[_campaignId].donations[_donor];
    }
    
    /**
     * @dev 캠페인의 기부자 목록 조회
     * @param _campaignId 캠페인 ID
     */
    function getCampaignDonors(uint256 _campaignId) 
        external 
        view 
        campaignExists(_campaignId) 
        returns (address[] memory) 
    {
        return campaignDonations[_campaignId].donors;
    }
    
    /**
     * @dev 모든 캠페인 ID 조회
     */
    function getAllCampaignIds() external view returns (uint256[] memory) {
        return campaignIds;
    }
    
    /**
     * @dev 캠페인 종료 가능 여부 확인
     * @param _campaignId 캠페인 ID
     */
    function canFinalize(uint256 _campaignId) 
        external 
        view 
        campaignExists(_campaignId) 
        returns (bool) 
    {
        Campaign memory campaign = campaigns[_campaignId];
        return !campaign.finalized && 
               (block.timestamp > campaign.deadline || campaign.totalRaised >= campaign.goalAmount) &&
               campaign.totalRaised > 0;
    }
    
    /**
     * @dev 플랫폼 수수료 업데이트 (소유자 전용)
     * @param _newFee 베이시스 포인트 단위의 새로운 플랫폼 수수료
     */
    function setPlatformFee(uint256 _newFee) external onlyOwner {
        require(_newFee <= MAX_PLATFORM_FEE, "Fee exceeds maximum");
        uint256 oldFee = platformFee;
        platformFee = _newFee;
        emit PlatformFeeUpdated(oldFee, _newFee);
    }
    
    /**
     * @dev 플랫폼 수수료 수취인 업데이트 (소유자 전용)
     * @param _newRecipient 새로운 수수료 수취인 주소
     */
    function setPlatformFeeRecipient(address _newRecipient) external onlyOwner {
        require(_newRecipient != address(0), "Invalid recipient address");
        address oldRecipient = platformFeeRecipient;
        platformFeeRecipient = _newRecipient;
        emit PlatformFeeRecipientUpdated(oldRecipient, _newRecipient);
    }
}