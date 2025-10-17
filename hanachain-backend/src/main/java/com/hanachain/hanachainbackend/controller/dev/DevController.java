package com.hanachain.hanachainbackend.controller.dev;

import com.hanachain.hanachainbackend.dto.common.ApiResponse;
import com.hanachain.hanachainbackend.dto.auth.LoginResponse;
import com.hanachain.hanachainbackend.dto.user.UserProfileResponse;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.UserWallet;
import com.hanachain.hanachainbackend.entity.VerificationSession;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.service.EmailService;
import com.hanachain.hanachainbackend.service.WalletService;
import com.hanachain.hanachainbackend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DevController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;
    private final WalletService walletService;
    
    @PostMapping("/create-test-user")
    public ResponseEntity<ApiResponse<String>> createTestUser() {
        try {
            // 테스트 사용자가 이미 존재하는지 확인
            if (userRepository.findByEmail("test@example.com").isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("테스트 사용자가 이미 존재합니다.", "test@example.com"));
            }
            
            User testUser = User.builder()
                    .email("test@example.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .name("테스트 사용자")
                    .nickname("testuser")
                    .role(User.Role.USER)
                    .emailVerified(true)
                    .profileCompleted(true)
                    .termsAccepted(true)
                    .build();
            
            userRepository.save(testUser);
            log.info("Test user created via API: test@example.com / Password123!");
            
            return ResponseEntity.ok(ApiResponse.success("테스트 사용자가 생성되었습니다.", "test@example.com"));
            
        } catch (Exception e) {
            log.error("Failed to create test user", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("테스트 사용자 생성에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/check-users")
    public ResponseEntity<ApiResponse<String>> checkUsers() {
        long userCount = userRepository.count();
        return ResponseEntity.ok(ApiResponse.success("사용자 수: " + userCount, String.valueOf(userCount)));
    }
    
    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse<String>> testEmail(@RequestParam String email) {
        try {
            log.info("Testing email send to: {}", email);
            
            // 간단한 테스트 이메일 발송
            emailService.sendSimpleEmail(
                email, 
                "[위아하나] 이메일 발송 테스트", 
                "이메일 발송 테스트입니다. 이 메시지를 받으셨다면 이메일 설정이 정상적으로 작동하고 있습니다."
            );
            
            log.info("Test email sent successfully to: {}", email);
            return ResponseEntity.ok(ApiResponse.success("테스트 이메일이 성공적으로 발송되었습니다.", email));
            
        } catch (Exception e) {
            log.error("Failed to send test email to: {}", email, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("테스트 이메일 발송에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/test-verification-email")
    public ResponseEntity<ApiResponse<String>> testVerificationEmail(@RequestParam String email) {
        try {
            log.info("Testing verification email send to: {}", email);
            
            // 인증 이메일 발송 테스트
            emailService.sendVerificationEmail(
                email, 
                "123456", 
                VerificationSession.VerificationType.EMAIL_REGISTRATION
            );
            
            log.info("Test verification email sent successfully to: {}", email);
            return ResponseEntity.ok(ApiResponse.success("테스트 인증 이메일이 성공적으로 발송되었습니다.", email));
            
        } catch (Exception e) {
            log.error("Failed to send test verification email to: {}", email, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("테스트 인증 이메일 발송에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/generate-test-token")
    public ResponseEntity<ApiResponse<LoginResponse>> generateTestToken() {
        try {
            // 테스트 사용자 찾기 또는 생성
            User testUser = userRepository.findByEmail("test@example.com")
                    .orElseGet(() -> {
                        User newUser = User.builder()
                                .email("test@example.com")
                                .password(passwordEncoder.encode("Password123!"))
                                .name("테스트 사용자")
                                .nickname("testuser")
                                .role(User.Role.USER)
                                .emailVerified(true)
                                .profileCompleted(true)
                                .termsAccepted(true)
                                .build();
                        return userRepository.save(newUser);
                    });
            
            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(testUser.getEmail());
            String refreshToken = jwtTokenProvider.generateRefreshToken(testUser.getEmail());
            
            // 사용자 정보 DTO 생성
            UserProfileResponse userDto = UserProfileResponse.fromEntity(testUser);
            
            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600) // 1시간
                    .user(userDto) // 사용자 정보 포함
                    .build();
            
            log.info("Test JWT token generated for user: {}", testUser.getEmail());
            log.info("Access Token: {}", accessToken.substring(0, 20) + "...");
            
            return ResponseEntity.ok(ApiResponse.success("테스트 JWT 토큰이 생성되었습니다.", response));
            
        } catch (Exception e) {
            log.error("Failed to generate test token", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("테스트 토큰 생성에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/create-test-user-with-wallet")
    public ResponseEntity<ApiResponse<String>> createTestUserWithWallet() {
        try {
            // 고유한 이메일 생성 (타임스탬프 사용)
            String timestamp = String.valueOf(System.currentTimeMillis());
            String email = "wallet-test-" + timestamp + "@example.com";
            String password = "TestPass123!";

            User testUser = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .name("Wallet Test User")
                    .phoneNumber("010-1234-5678")
                    .role(User.Role.USER)
                    .emailVerified(true)
                    .termsAccepted(true)
                    .privacyAccepted(true)
                    .marketingAccepted(false)
                    .enabled(true)
                    .build();

            User savedUser = userRepository.save(testUser);
            log.info("Test user created: {} / {}", email, password);

            // E-Wallet 생성
            try {
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                log.info("🔐 E-Wallet 생성 시작 - userId: {}", savedUser.getId());

                UserWallet primaryWallet = walletService.createWallet(
                    savedUser,
                    password,  // 사용자 비밀번호로 개인키 암호화
                    true  // 주 지갑으로 설정
                );

                log.info("✅ E-Wallet 생성 완료!");
                log.info("   userId: {}", savedUser.getId());
                log.info("   walletAddress: {}", primaryWallet.getWalletAddress());
                log.info("   walletType: {}", primaryWallet.getWalletType());
                log.info("   isPrimary: {}", primaryWallet.getIsPrimary());
                log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                return ResponseEntity.ok(ApiResponse.success(
                    "테스트 사용자와 지갑이 생성되었습니다. Wallet Address: " + primaryWallet.getWalletAddress(),
                    email
                ));

            } catch (Exception e) {
                log.error("❌ E-Wallet 생성 실패 - userId: {}", savedUser.getId(), e);
                return ResponseEntity.ok(ApiResponse.success(
                    "테스트 사용자가 생성되었으나 지갑 생성에 실패했습니다: " + e.getMessage(),
                    email
                ));
            }

        } catch (Exception e) {
            log.error("Failed to create test user with wallet", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("테스트 사용자 생성에 실패했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/create-admin-user")
    public ResponseEntity<ApiResponse<String>> createAdminUser() {
        try {
            // 관리자 사용자가 이미 존재하는지 확인
            if (userRepository.findByEmail("admin@example.com").isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("관리자 사용자가 이미 존재합니다.", "admin@example.com"));
            }

            User adminUser = User.builder()
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("AdminPassword123!"))
                    .name("관리자 사용자")
                    .nickname("admin")
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .profileCompleted(true)
                    .termsAccepted(true)
                    .build();

            userRepository.save(adminUser);
            log.info("Admin user created via API: admin@example.com / AdminPassword123!");

            return ResponseEntity.ok(ApiResponse.success("관리자 사용자가 생성되었습니다.", "admin@example.com"));

        } catch (Exception e) {
            log.error("Failed to create admin user", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("관리자 사용자 생성에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/generate-admin-token")
    public ResponseEntity<ApiResponse<LoginResponse>> generateAdminToken() {
        try {
            // 관리자 사용자 찾기 또는 생성
            User adminUser = userRepository.findByEmail("admin@example.com")
                    .orElseGet(() -> {
                        User newAdmin = User.builder()
                                .email("admin@example.com")
                                .password(passwordEncoder.encode("AdminPassword123!"))
                                .name("관리자 사용자")
                                .nickname("admin")
                                .role(User.Role.ADMIN)
                                .emailVerified(true)
                                .profileCompleted(true)
                                .termsAccepted(true)
                                .build();
                        return userRepository.save(newAdmin);
                    });
            
            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.generateAccessToken(adminUser.getEmail());
            String refreshToken = jwtTokenProvider.generateRefreshToken(adminUser.getEmail());
            
            // 사용자 정보 DTO 생성
            UserProfileResponse userDto = UserProfileResponse.fromEntity(adminUser);
            
            LoginResponse response = LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600) // 1시간
                    .user(userDto) // 사용자 정보 포함
                    .build();
            
            log.info("Admin JWT token generated for user: {}", adminUser.getEmail());
            log.info("Admin Access Token: {}", accessToken.substring(0, 20) + "...");
            
            return ResponseEntity.ok(ApiResponse.success("관리자 JWT 토큰이 생성되었습니다.", response));
            
        } catch (Exception e) {
            log.error("Failed to generate admin token", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("관리자 토큰 생성에 실패했습니다: " + e.getMessage()));
        }
    }
}
