package com.hanachain.hanachainbackend.dto.campaignmanager;

import com.hanachain.hanachainbackend.entity.CampaignManager;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignManagerUpdateRequest {

    private CampaignManager.ManagerRole role;

    private CampaignManager.ManagerStatus status;

    @Size(max = 500, message = "메모는 500자 이하로 입력해주세요")
    private String notes;
}
