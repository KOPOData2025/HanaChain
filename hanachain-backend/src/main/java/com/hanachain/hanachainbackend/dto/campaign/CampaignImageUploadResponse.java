package com.hanachain.hanachainbackend.dto.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 캠페인 이미지 업로드 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignImageUploadResponse {
    
    private String imageUrl;
    private String originalFileName;
    private Long fileSize;
    private String message;
    
    /**
     * 성공 응답 생성
     */
    public static CampaignImageUploadResponse success(String imageUrl, String originalFileName, Long fileSize) {
        return CampaignImageUploadResponse.builder()
                .imageUrl(imageUrl)
                .originalFileName(originalFileName)
                .fileSize(fileSize)
                .message("캠페인 이미지가 성공적으로 업로드되었습니다.")
                .build();
    }
}