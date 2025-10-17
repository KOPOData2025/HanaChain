package com.hanachain.hanachainbackend.dto.auth;

import com.hanachain.hanachainbackend.dto.user.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private Integer expiresIn;
    private UserProfileResponse user;
}
