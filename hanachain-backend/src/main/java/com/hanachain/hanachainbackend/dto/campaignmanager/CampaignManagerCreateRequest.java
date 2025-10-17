package com.hanachain.hanachainbackend.dto.campaignmanager;

import com.hanachain.hanachainbackend.entity.CampaignManager;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignManagerCreateRequest {

    @NotNull(message = "캠페인 ID는 필수입니다")
    private Long campaignId;

    @NotNull(message = "유저 ID는 필수입니다")
    private Long userId;

    @Builder.Default
    private CampaignManager.ManagerRole role = CampaignManager.ManagerRole.MANAGER;

    @Size(max = 500, message = "메모는 500자 이하로 입력해주세요")
    private String notes;
}
