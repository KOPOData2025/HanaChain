package com.hanachain.hanachainbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프로필 정보 응답 DTO
 * 프론트엔드의 ProfileData 인터페이스와 매핑
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    
    private String nickname;
    private String email;
    private String profileImage;
    private Boolean profileCompleted;
    
    /**
     * User 엔티티로부터 ProfileResponse 생성
     */
    public static ProfileResponse from(com.hanachain.hanachainbackend.entity.User user) {
        return ProfileResponse.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .profileCompleted(user.getProfileCompleted())
                .build();
    }
}