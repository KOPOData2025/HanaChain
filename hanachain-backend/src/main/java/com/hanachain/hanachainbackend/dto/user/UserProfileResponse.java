package com.hanachain.hanachainbackend.dto.user;

import com.hanachain.hanachainbackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    
    private Long id;
    private String email;
    private String name;
    private String phoneNumber;
    private User.Role role;
    private Boolean emailVerified;
    private Boolean termsAccepted;
    private Boolean privacyAccepted;
    private Boolean marketingAccepted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .termsAccepted(user.getTermsAccepted())
                .privacyAccepted(user.getPrivacyAccepted())
                .marketingAccepted(user.getMarketingAccepted())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
