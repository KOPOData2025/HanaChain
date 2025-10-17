package com.hanachain.hanachainbackend.security;

import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("integration-test")
@Transactional
@DisplayName("Security Integration Test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User regularUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        regularUser = User.builder()
                .email("user@test.com")
                .name("Regular User")
                .password(passwordEncoder.encode("password"))
                .role(User.Role.USER)
                .emailVerified(true)
                .build();
        userRepository.save(regularUser);

        adminUser = User.builder()
                .email("admin@test.com")
                .name("Admin User")
                .password(passwordEncoder.encode("password"))
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .build();
        userRepository.save(adminUser);
    }

    @Test
    @DisplayName("공개 엔드포인트는 인증 없이 접근 가능하다")
    void testPublicEndpointsAccessible() throws Exception {
        // Auth endpoints
        mockMvc.perform(post("/auth/register"))
                .andExpect(status().isBadRequest()); // 바디가 없어서 400

        mockMvc.perform(post("/auth/login"))
                .andExpect(status().isBadRequest()); // 바디가 없어서 400

        // Public comment endpoints
        mockMvc.perform(get("/comments/public/campaign/1"))
                .andExpect(status().isOk());

        // Health check
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        // Actuator health
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 엔드포인트는 USER 역할이 필요하다")
    @WithMockUser(roles = "USER")
    void testUserEndpointsRequireUserRole() throws Exception {
        mockMvc.perform(get("/users/profile")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/campaigns")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/donations")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/expenses")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/comments")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 엔드포인트는 ADMIN 역할이 필요하다")
    @WithMockUser(roles = "ADMIN")
    void testAdminEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isNotFound()); // 컨트롤러 미구현으로 404

        mockMvc.perform(get("/expenses/pending")
                        .with(user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/comments/reported")
                        .with(user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/info")
                        .with(user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isNotFound()); // actuator 엔드포인트 미활성화
    }

    @Test
    @DisplayName("일반 사용자는 관리자 엔드포인트에 접근할 수 없다")
    @WithMockUser(roles = "USER")
    void testUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/expenses/1/approve")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/comments/1/hide")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/expenses/pending")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/comments/reported")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 보호된 엔드포인트에 접근할 수 없다")
    void testUnauthenticatedCannotAccessProtectedEndpoints() throws Exception {
        // User endpoints
        mockMvc.perform(get("/users/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/campaigns"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/campaigns"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/donations"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/donations"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/expenses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/expenses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/comments"))
                .andExpect(status().isUnauthorized());

        // Admin endpoints
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/expenses/1/approve"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/comments/1/hide"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS 설정이 올바르게 적용된다")
    void testCorsConfiguration() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}