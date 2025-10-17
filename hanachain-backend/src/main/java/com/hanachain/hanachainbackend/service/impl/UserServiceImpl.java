package com.hanachain.hanachainbackend.service.impl;

import com.hanachain.hanachainbackend.dto.auth.LoginRequest;
import com.hanachain.hanachainbackend.dto.auth.LoginResponse;
import com.hanachain.hanachainbackend.dto.auth.RegisterRequest;
import com.hanachain.hanachainbackend.dto.user.UserProfileResponse;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.VerificationSession;
import com.hanachain.hanachainbackend.entity.UserWallet;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.security.JwtTokenProvider;
import com.hanachain.hanachainbackend.service.UserService;
import com.hanachain.hanachainbackend.service.VerificationService;
import com.hanachain.hanachainbackend.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final VerificationService verificationService;
    private final WalletService walletService;

    @Override
    public User registerUser(RegisterRequest request) {
        // 비밀번호 일치 확인
        if (!request.isPasswordMatching()) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 이메일 중복 확인
        if (existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        // 이메일 인증 확인
        boolean isVerified = verificationService.verifyCode(
                request.getEmail(), 
                request.getVerificationCode(), 
                VerificationSession.VerificationType.EMAIL_REGISTRATION
        );

        if (!isVerified) {
            throw new RuntimeException("이메일 인증이 완료되지 않았습니다.");
        }

        // 사용자 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .role(User.Role.USER)
                .emailVerified(true) // 이메일 인증 완료
                .termsAccepted(request.getTermsAccepted())
                .privacyAccepted(request.getPrivacyAccepted())
                .marketingAccepted(request.getMarketingAccepted())
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // ✨ E-Wallet 자동 생성
        try {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("🔐 E-Wallet 자동 생성 시작 - userId: {}", savedUser.getId());

            UserWallet primaryWallet = walletService.createWallet(
                savedUser,
                request.getPassword(),  // 사용자 비밀번호로 개인키 암호화
                true  // 주 지갑으로 설정
            );

            log.info("✅ E-Wallet 자동 생성 완료!");
            log.info("   userId: {}", savedUser.getId());
            log.info("   walletAddress: {}", primaryWallet.getWalletAddress());
            log.info("   walletType: {}", primaryWallet.getWalletType());
            log.info("   isPrimary: {}", primaryWallet.getIsPrimary());
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            // 지갑 생성 실패 시에도 회원가입은 완료 (추후 수동 생성 가능)
            log.error("❌ E-Wallet 자동 생성 실패 - userId: {}", savedUser.getId(), e);
            log.warn("⚠️  회원가입은 완료되었으나 지갑 생성에 실패했습니다. 사용자는 마이페이지에서 수동으로 지갑을 생성할 수 있습니다.");
        }

        // 인증 세션 정리
        verificationService.deleteVerificationSessions(
                request.getEmail(),
                VerificationSession.VerificationType.EMAIL_REGISTRATION
        );

        log.info("User registered successfully: {}", savedUser.getEmail());
        return savedUser;
    }

    @Override
    public LoginResponse loginUser(LoginRequest request) {
        try {
            // 인증 처리
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // 토큰 생성
            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(request.getEmail());

            // 사용자 정보 조회
            User user = findByEmail(request.getEmail());
            UserProfileResponse userProfile = UserProfileResponse.fromEntity(user);

            log.info("User logged in successfully: {}", request.getEmail());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(3600) // 1시간
                    .user(userProfile)
                    .build();

        } catch (AuthenticationException e) {
            log.warn("Login failed for email: {}", request.getEmail());
            throw new RuntimeException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // 토큰 유효성 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 사용자 정보 추출
        String email = tokenProvider.getUsernameFromToken(refreshToken);
        User user = findByEmail(email);

        // 새 토큰 발급
        String newAccessToken = tokenProvider.generateAccessToken(email);
        String newRefreshToken = tokenProvider.generateRefreshToken(email);

        UserProfileResponse userProfile = UserProfileResponse.fromEntity(user);

        log.info("Token refreshed for user: {}", email);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(3600) // 1시간
                .user(userProfile)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = findById(userId);
        return UserProfileResponse.fromEntity(user);
    }

    @Override
    public UserProfileResponse updateUserProfile(Long userId, UserProfileResponse request) {
        User user = findById(userId);

        // 업데이트 가능한 필드만 변경
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getMarketingAccepted() != null) {
            user.setMarketingAccepted(request.getMarketingAccepted());
        }

        User updatedUser = userRepository.save(user);
        log.info("User profile updated: {}", updatedUser.getEmail());

        return UserProfileResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return userRepository.existsByName(name);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> searchUsers(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Search keyword cannot be empty");
        }
        
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 50))); // 최대 50개로 제한
        List<User> users = userRepository.searchByKeyword(keyword.trim(), pageable);
        
        log.info("Found {} users for keyword: '{}'", users.size(), keyword);
        
        return users.stream()
                .map(UserProfileResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getUserList(String keyword, Pageable pageable) {
        log.debug("Getting user list with keyword: '{}' and pageable: {}", keyword, pageable);
        
        Page<User> users;
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 키워드가 있는 경우 검색
            users = userRepository.searchByKeywordPage(keyword.trim(), pageable);
        } else {
            // 키워드가 없는 경우 전체 일반 유저 조회 (USER 역할만)
            users = userRepository.findByRole(User.Role.USER, pageable);
        }
        
        log.debug("Found {} users", users.getTotalElements());
        
        return users.map(UserProfileResponse::fromEntity);
    }
}
