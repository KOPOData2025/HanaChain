package com.hanachain.hanachainbackend.dto.campaignmanager;

import com.hanachain.hanachainbackend.entity.CampaignManager;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignManagerResponse {

    private Long id;
    private Long campaignId;
    private String campaignTitle;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userNickname;
    private CampaignManager.ManagerRole role;
    private CampaignManager.ManagerStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime revokedAt;
    private Long assignedByUserId;
    private String assignedByUserName;
    private String notes;

    /**
     * 담당자 권한이 활성 상태인지 확인
     */
    public boolean isActive() {
        return status == CampaignManager.ManagerStatus.ACTIVE && revokedAt == null;
    }

    /**
     * 역할 한국어 이름 반환
     */
    public String getRoleDisplayName() {
        return role != null ? role.getDisplayName() : "";
    }

    /**
     * 상태 한국어 이름 반환
     */
    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "";
    }
}
