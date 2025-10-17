package com.hanachain.hanachainbackend.dto.campaign;

import com.hanachain.hanachainbackend.entity.BlockchainStatus;
import com.hanachain.hanachainbackend.entity.Campaign;
import com.hanachain.hanachainbackend.entity.CampaignStory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignDetailResponse {
    
    private Long id;
    private String title;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private Integer donorCount;
    private String imageUrl;
    private Campaign.CampaignStatus status;
    private Campaign.CampaignCategory category;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;  // 관리자용 삭제 여부 확인
    private CreatorInfo creator;
    private String organizer;
    private BigDecimal progressPercentage;
    private boolean isActive;
    private boolean isCompleted;
    private List<StoryInfo> stories;
    
    // 블록체인 관련 필드
    private BlockchainStatus blockchainStatus;
    private String blockchainContractAddress;  // HanaChainCampaign 컨트랙트 주소
    private BigInteger blockchainCampaignId;    // 컨트랙트 내부의 캠페인 ID
    private String blockchainTransactionHash;
    private String beneficiaryAddress;
    private String blockchainErrorMessage;
    private LocalDateTime blockchainProcessedAt;
    
    public static CampaignDetailResponse fromEntity(Campaign campaign) {
        return CampaignDetailResponse.builder()
                .id(campaign.getId())
                .title(campaign.getTitle())
                .description(campaign.getDescription())
                .targetAmount(campaign.getTargetAmount())
                .currentAmount(campaign.getCurrentAmount())
                .donorCount(campaign.getDonorCount())
                .imageUrl(campaign.getImageUrl())
                .status(campaign.getStatus())
                .category(campaign.getCategory())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .deletedAt(campaign.getDeletedAt())
                .creator(CreatorInfo.fromUser(campaign.getUser()))
                .organizer(campaign.getOrganizer())
                .progressPercentage(campaign.getProgressPercentage())
                .isActive(campaign.isActive())
                .isCompleted(campaign.isCompleted())
                .stories(campaign.getStories() != null ?
                        campaign.getStories().stream()
                                .filter(CampaignStory::getPublished)
                                .map(StoryInfo::fromEntity)
                                .collect(Collectors.toList()) : List.of())
                .blockchainStatus(campaign.getBlockchainStatus())
                .blockchainContractAddress(campaign.getBlockchainContractAddress())
                .blockchainCampaignId(campaign.getBlockchainCampaignId())
                .blockchainTransactionHash(campaign.getBlockchainTransactionHash())
                .beneficiaryAddress(campaign.getBeneficiaryAddress())
                .blockchainErrorMessage(campaign.getBlockchainErrorMessage())
                .blockchainProcessedAt(campaign.getBlockchainProcessedAt())
                .build();
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatorInfo {
        private Long id;
        private String name;
        private String email;
        
        public static CreatorInfo fromUser(com.hanachain.hanachainbackend.entity.User user) {
            return CreatorInfo.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .build();
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoryInfo {
        private Long id;
        private String title;
        private String content;
        private String imageUrl;
        private CampaignStory.StoryType type;
        private LocalDateTime createdAt;
        
        public static StoryInfo fromEntity(CampaignStory story) {
            return StoryInfo.builder()
                    .id(story.getId())
                    .title(story.getTitle())
                    .content(story.getContent())
                    .imageUrl(story.getImageUrl())
                    .type(story.getType())
                    .createdAt(story.getCreatedAt())
                    .build();
        }
    }
}
