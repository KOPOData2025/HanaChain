package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 대시보드 종합 정보 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    
    private ProfileResponse profile;                      // 프로필 정보
    private DonationStatsResponse donationStats;          // 기부 통계
    private List<DonationHistoryResponse> recentDonations; // 최근 기부 내역
    private Long favoriteCampaignsCount;                   // 관심 캠페인 수
}