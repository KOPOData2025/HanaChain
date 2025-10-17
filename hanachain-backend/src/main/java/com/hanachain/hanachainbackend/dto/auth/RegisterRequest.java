package com.hanachain.hanachainbackend.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$", 
             message = "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다.")
    private String password;
    
    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String confirmPassword;
    
    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;
    
    @Pattern(regexp = "^01[0-9]-?[0-9]{4}-?[0-9]{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    private String phoneNumber;
    
    @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
    private Boolean termsAccepted;
    
    @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
    private Boolean privacyAccepted;
    
    private Boolean marketingAccepted = false;
    
    @NotBlank(message = "인증 코드는 필수입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
    private String verificationCode;
    
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
