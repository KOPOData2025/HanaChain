package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.auth.LoginRequest;
import com.hanachain.hanachainbackend.dto.auth.LoginResponse;
import com.hanachain.hanachainbackend.dto.auth.RegisterRequest;
import com.hanachain.hanachainbackend.dto.user.UserProfileResponse;
import com.hanachain.hanachainbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    
    /**
     * 사용자 회원가입을 처리합니다.
     */
    User registerUser(RegisterRequest request);
    
    /**
     * 사용자 로그인을 처리합니다.
     */
    LoginResponse loginUser(LoginRequest request);
    
    /**
     * 리프레시 토큰을 사용하여 새 액세스 토큰을 발급합니다.
     */
    LoginResponse refreshToken(String refreshToken);
    
    /**
     * 사용자 프로필을 조회합니다.
     */
    UserProfileResponse getUserProfile(Long userId);
    
    /**
     * 사용자 프로필을 업데이트합니다.
     */
    UserProfileResponse updateUserProfile(Long userId, UserProfileResponse request);
    
    /**
     * 이메일로 사용자를 조회합니다.
     */
    User findByEmail(String email);
    
    /**
     * 사용자 ID로 사용자를 조회합니다.
     */
    User findById(Long id);
    
    /**
     * 이메일 중복을 확인합니다.
     */
    boolean existsByEmail(String email);
    
    /**
     * 이름(닉네임) 중복을 확인합니다.
     */
    boolean existsByName(String name);
    
    /**
     * 키워드로 사용자를 검색합니다. (이름 또는 이메일)
     */
    java.util.List<UserProfileResponse> searchUsers(String keyword, int limit);
    
    /**
     * 일반 유저 목록을 조회합니다 (페이징 지원)
     */
    Page<UserProfileResponse> getUserList(String keyword, Pageable pageable);
}
