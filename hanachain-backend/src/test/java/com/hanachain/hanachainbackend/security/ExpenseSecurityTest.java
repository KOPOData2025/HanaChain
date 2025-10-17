package com.hanachain.hanachainbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
@DisplayName("Expense Security Test")
class ExpenseSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("인증되지 않은 사용자는 지출 API에 접근할 수 없다")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/expenses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("일반 사용자는 지출 목록을 조회할 수 있다")
    @WithMockUser(roles = "USER")
    void testUserCanViewExpenses() throws Exception {
        mockMvc.perform(get("/expenses")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 사용자는 지출 승인을 할 수 없다")
    @WithMockUser(roles = "USER")
    void testUserCannotApproveExpenses() throws Exception {
        mockMvc.perform(put("/expenses/1/approve")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 지출을 승인할 수 있다")
    @WithMockUser(roles = "ADMIN")
    void testAdminCanApproveExpenses() throws Exception {
        mockMvc.perform(put("/expenses/1/approve")
                        .with(user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isNotFound()); // 실제 expense가 없어서 404
    }

    @Test
    @DisplayName("관리자는 지출을 거부할 수 있다")
    @WithMockUser(roles = "ADMIN")
    void testAdminCanRejectExpenses() throws Exception {
        mockMvc.perform(put("/expenses/1/reject")
                        .with(user(adminUser.getEmail()).roles("ADMIN"))
                        .content(objectMapper.writeValueAsString("rejection reason"))
                        .contentType("application/json"))
                .andExpect(status().isNotFound()); // 실제 expense가 없어서 404
    }

    @Test
    @DisplayName("관리자만 승인 대기 지출을 조회할 수 있다")
    @WithMockUser(roles = "ADMIN")
    void testOnlyAdminCanViewPendingExpenses() throws Exception {
        mockMvc.perform(get("/expenses/pending")
                        .with(user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 사용자는 승인 대기 지출을 조회할 수 없다")
    @WithMockUser(roles = "USER")
    void testUserCannotViewPendingExpenses() throws Exception {
        mockMvc.perform(get("/expenses/pending")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());
    }
}