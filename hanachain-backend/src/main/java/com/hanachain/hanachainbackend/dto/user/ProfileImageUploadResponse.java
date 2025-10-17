package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프로필 이미지 업로드 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileImageUploadResponse {
    
    private String imageUrl;
    private String originalFileName;
    private Long fileSize;
    private String message;
    
    /**
     * 성공 응답 생성
     */
    public static ProfileImageUploadResponse success(String imageUrl, String originalFileName, Long fileSize) {
        return ProfileImageUploadResponse.builder()
                .imageUrl(imageUrl)
                .originalFileName(originalFileName)
                .fileSize(fileSize)
                .message("프로필 이미지가 성공적으로 업로드되었습니다.")
                .build();
    }
}