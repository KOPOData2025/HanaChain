package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 즐겨찾기 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteResponse {
    
    private Long id;
    private Long campaignId;
    private String campaignTitle;
    private String campaignImage;
    private String campaignDescription;
    private String memo;
    private LocalDateTime createdAt;
    
    // 캠페인 상태 정보
    private String campaignStatus; // ACTIVE, COMPLETED, SUSPENDED 등
    private LocalDateTime campaignEndDate;
}