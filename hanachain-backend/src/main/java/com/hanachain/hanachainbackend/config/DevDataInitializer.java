package com.hanachain.hanachainbackend.config;

import com.hanachain.hanachainbackend.entity.Notice;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.repository.NoticeRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements ApplicationRunner {
    
    private final UserRepository userRepository;
    private final NoticeRepository noticeRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeTestUsers();
        initializeNotices();
    }
    
    private void initializeTestUsers() {
        // 테스트 사용자가 이미 존재하는지 확인
        if (userRepository.findByEmail("test@example.com").isEmpty()) {
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
            log.info("Test user created: test@example.com / Password123!");
        }
        
        // 관리자 사용자
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            User adminUser = User.builder()
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("Admin123!"))
                    .name("관리자")
                    .nickname("admin")
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .profileCompleted(true)
                    .termsAccepted(true)
                    .build();
            
            userRepository.save(adminUser);
            log.info("Admin user created: admin@example.com / Admin123!");
        }
    }
    
    private void initializeNotices() {
        // 공지사항 개수 확인
        long noticeCount = noticeRepository.count();
        
        if (noticeCount == 0) {
            // 중요 공지사항
            Notice notice1 = Notice.builder()
                    .title("하나체인 플랫폼 정식 오픈!")
                    .content("하나체인 기부 플랫폼이 정식으로 오픈했습니다. 블록체인 기술을 활용한 투명한 기부를 경험해보세요.")
                    .isImportant(true)
                    .viewCount(0)
                    .build();
            
            Notice notice2 = Notice.builder()
                    .title("신규 캠페인 등록 안내")
                    .content("단체 관리자 분들께서는 새로운 캠페인을 등록하실 수 있습니다. 자세한 사항은 고객센터로 문의해주세요.")
                    .isImportant(false)
                    .viewCount(0)
                    .build();
            
            Notice notice3 = Notice.builder()
                    .title("개인정보 처리방침 업데이트")
                    .content("개인정보 처리방침이 2025년 10월 1일자로 업데이트 되었습니다.")
                    .isImportant(false)
                    .viewCount(0)
                    .build();
            
            noticeRepository.save(notice1);
            noticeRepository.save(notice2);
            noticeRepository.save(notice3);
            
            log.info("Sample notices created: 3 notices");
        } else {
            log.info("Notices already exist: {} notices", noticeCount);
        }
    }
}
