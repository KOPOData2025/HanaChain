// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

import "./HanaChainCampaign.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";

/**
 * @title HanaChainFactory
 * @dev HanaChain 캠페인을 생성하고 관리하는 팩토리 컨트랙트 (USDC 기반)
 */
contract HanaChainFactory {
    // USDC 토큰 주소
    address public immutable usdcToken;

    // 생성된 캠페인 정보
    struct CampaignInfo {
        address campaignAddress;    // 캠페인 컨트랙트 주소
        string title;              // 캠페인 제목
        address creator;           // 생성자
        uint256 goalAmount;        // 목표 금액 (USDC 최소 단위)
        uint256 deadline;          // 마감일
        uint256 createdAt;         // 생성 시간
        bool isActive;             // 활성 상태
    }

    // 상태 변수
    address[] public deployedCampaigns;                    // 배포된 모든 캠페인 주소
    mapping(address => CampaignInfo) public campaignInfo;  // 캠페인 정보 매핑
    mapping(address => address[]) public creatorCampaigns; // 생성자별 캠페인 목록
    uint256 public totalCampaigns;                         // 총 캠페인 수

    // 이벤트
    event CampaignCreated(
        address indexed campaignAddress,
        address indexed creator,
        string title,
        uint256 goalAmount,
        uint256 deadline,
        uint256 timestamp
    );

    event CampaignDeactivated(address indexed campaignAddress, uint256 timestamp);

    /**
     * @dev 생성자
     * @param _usdcToken USDC 토큰 컨트랙트 주소
     */
    constructor(address _usdcToken) {
        require(_usdcToken != address(0), "Invalid USDC token address");
        usdcToken = _usdcToken;
    }

    /**
     * @dev 새로운 HanaChain 캠페인 생성
     * @param _title 캠페인 제목
     * @param _description 캠페인 설명
     * @param _goalAmount 목표 금액 (USDC 최소 단위, 6 decimals)
     * @param _durationDays 캠페인 기간 (일)
     * @param _beneficiary 수혜자 주소
     */
    function createCampaign(
        string memory _title,
        string memory _description,
        uint256 _goalAmount,
        uint256 _durationDays,
        address payable _beneficiary
    ) public returns (address) {
        require(bytes(_title).length > 0, "Title cannot be empty");
        require(bytes(_description).length > 0, "Description cannot be empty");
        require(_goalAmount > 0, "Goal amount must be greater than 0");
        require(_durationDays > 0 && _durationDays <= 365, "Invalid duration");
        require(_beneficiary != address(0), "Invalid beneficiary address");

        // 새로운 HanaChainCampaign 컨트랙트 배포
        HanaChainCampaign newCampaign = new HanaChainCampaign(usdcToken);

        // 단일 캠페인 생성 (ID는 항상 1이 됨)
        uint256 duration = _durationDays * 1 days;
        newCampaign.createCampaign(
            _beneficiary,
            _goalAmount,
            duration,
            _title,
            _description
        );

        address campaignAddress = address(newCampaign);

        // 캠페인 정보 저장
        campaignInfo[campaignAddress] = CampaignInfo({
            campaignAddress: campaignAddress,
            title: _title,
            creator: msg.sender,
            goalAmount: _goalAmount,
            deadline: block.timestamp + duration,
            createdAt: block.timestamp,
            isActive: true
        });

        // 배열에 추가
        deployedCampaigns.push(campaignAddress);
        creatorCampaigns[msg.sender].push(campaignAddress);
        totalCampaigns++;

        // 이벤트 발생
        emit CampaignCreated(
            campaignAddress,
            msg.sender,
            _title,
            _goalAmount,
            block.timestamp + duration,
            block.timestamp
        );

        return campaignAddress;
    }

    /**
     * @dev 캠페인 비활성화 (생성자만 가능)
     */
    function deactivateCampaign(address _campaignAddress) public {
        require(campaignInfo[_campaignAddress].creator == msg.sender, "Only creator can deactivate");
        require(campaignInfo[_campaignAddress].isActive, "Campaign already inactive");

        campaignInfo[_campaignAddress].isActive = false;
        emit CampaignDeactivated(_campaignAddress, block.timestamp);
    }

    /**
     * @dev 모든 활성 캠페인 조회
     */
    function getActiveCampaigns() public view returns (address[] memory) {
        uint256 activeCount = 0;

        // 활성 캠페인 수 계산
        for (uint256 i = 0; i < deployedCampaigns.length; i++) {
            if (campaignInfo[deployedCampaigns[i]].isActive) {
                activeCount++;
            }
        }

        // 활성 캠페인 주소 배열 생성
        address[] memory activeCampaigns = new address[](activeCount);
        uint256 currentIndex = 0;

        for (uint256 i = 0; i < deployedCampaigns.length; i++) {
            if (campaignInfo[deployedCampaigns[i]].isActive) {
                activeCampaigns[currentIndex] = deployedCampaigns[i];
                currentIndex++;
            }
        }

        return activeCampaigns;
    }

    /**
     * @dev 특정 생성자의 캠페인 목록 조회
     */
    function getCampaignsByCreator(address _creator) public view returns (address[] memory) {
        return creatorCampaigns[_creator];
    }

    /**
     * @dev 캠페인 기본 정보 조회
     */
    function getCampaignInfo(address _campaignAddress) public view returns (
        string memory title,
        address creator,
        uint256 goalAmount,
        uint256 deadline,
        uint256 createdAt,
        bool isActive
    ) {
        CampaignInfo memory info = campaignInfo[_campaignAddress];
        return (
            info.title,
            info.creator,
            info.goalAmount,
            info.deadline,
            info.createdAt,
            info.isActive
        );
    }

    /**
     * @dev 캠페인 상세 정보 조회 (캠페인 컨트랙트에서 직접 조회)
     */
    function getCampaignDetails(address _campaignAddress) public view returns (
        string memory title,
        string memory description,
        uint256 goalAmount,
        uint256 totalRaised,
        uint256 deadline,
        address beneficiary,
        bool finalized,
        uint256 donorCount
    ) {
        HanaChainCampaign campaign = HanaChainCampaign(_campaignAddress);

        // 캠페인 ID는 항상 1 (각 컨트랙트당 하나의 캠페인만 존재)
        HanaChainCampaign.Campaign memory campaignData = campaign.getCampaign(1);
        address[] memory donors = campaign.getCampaignDonors(1);

        title = campaignData.title;
        description = campaignData.description;
        goalAmount = campaignData.goalAmount;
        totalRaised = campaignData.totalRaised;
        deadline = campaignData.deadline;
        beneficiary = campaignData.beneficiary;
        finalized = campaignData.finalized;
        donorCount = donors.length;
    }

    /**
     * @dev 전체 캠페인 수 조회
     */
    function getTotalCampaigns() public view returns (uint256) {
        return totalCampaigns;
    }

    /**
     * @dev 모든 캠페인 주소 조회
     */
    function getAllCampaigns() public view returns (address[] memory) {
        return deployedCampaigns;
    }

    /**
     * @dev 캠페인 주소 유효성 검증
     */
    function isValidCampaign(address _campaignAddress) public view returns (bool) {
        return campaignInfo[_campaignAddress].campaignAddress != address(0);
    }

    /**
     * @dev USDC 토큰 주소 조회
     */
    function getUSDCToken() public view returns (address) {
        return usdcToken;
    }
}
