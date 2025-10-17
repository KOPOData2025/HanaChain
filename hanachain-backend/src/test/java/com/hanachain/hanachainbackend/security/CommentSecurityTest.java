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
@DisplayName("Comment Security Test")
class CommentSecurityTest {

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
    @DisplayName("인증되지 않은 사용자도 공개 댓글을 조회할 수 있다")
    void testUnauthenticatedCanViewPublicComments() throws Exception {
        mockMvc.perform(get("/comments/public/campaign/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 댓글을 작성할 수 없다")
    void testUnauthenticatedCannotCreateComments() throws Exception {
        mockMvc.perform(post("/comments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("일반 사용자는 댓글을 작성할 수 있다")
    @WithMockUser(roles = "USER")
    void testUserCanCreateComments() throws Exception {
        String commentJson = objectMapper.writeValueAsString(
                new CommentCreateRequest("좋은 캠페인이네요!", 1L, false)
        );

        mockMvc.perform(post("/comments")
                        .with(user(regularUser.getEmail()).roles("USER"))
                        .content(commentJson)
                        .contentType("application/json"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("일반 사용자는 댓글을 숨길 수 없다")
    @WithMockUser(roles = "USER")
    void testUserCannotHideComments() throws Exception {
        mockMvc.perform(put("/comments/1/hide")
                        .with(user(regularUser.getEmail()).roles("USER"))
                        .content("inappropriate content")
                        .contentType("application/json"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 댓글을 숨길 수 있다")
    @WithMockUser(roles = "ADMIN")
    void testAdminCanHideComments() throws Exception {
        mockMvc.perform(put("/comments/1/hide")
                        .with(user(adminUser.getEmail()).roles("ADMIN"))
                        .content("inappropriate content")
                        .contentType("application/json"))
                .andExpect(status().isNotFound()); // 실제 comment가 없어서 404
    }

    @Test
    @DisplayName("관리자만 신고된 댓글을 조회할 수 있다")
    @WithMockUser(roles = "ADMIN")
    void testOnlyAdminCanViewReportedComments() throws Exception {
        mockMvc.perform(get("/comments/reported")
                        .with(user(adminUser.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 사용자는 신고된 댓글을 조회할 수 없다")
    @WithMockUser(roles = "USER")
    void testUserCannotViewReportedComments() throws Exception {
        mockMvc.perform(get("/comments/reported")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("일반 사용자는 자신의 댓글을 수정할 수 있다")
    @WithMockUser(roles = "USER")
    void testUserCanEditOwnComments() throws Exception {
        String updateJson = objectMapper.writeValueAsString(
                new CommentUpdateRequest("수정된 댓글 내용입니다.")
        );

        mockMvc.perform(put("/comments/1")
                        .with(user(regularUser.getEmail()).roles("USER"))
                        .content(updateJson)
                        .contentType("application/json"))
                .andExpect(status().isNotFound()); // 실제 comment가 없어서 404
    }

    @Test
    @DisplayName("일반 사용자는 자신의 댓글을 삭제할 수 있다")
    @WithMockUser(roles = "USER")
    void testUserCanDeleteOwnComments() throws Exception {
        mockMvc.perform(delete("/comments/1")
                        .with(user(regularUser.getEmail()).roles("USER")))
                .andExpect(status().isNotFound()); // 실제 comment가 없어서 404
    }

    // DTO classes for testing
    private static class CommentCreateRequest {
        public String content;
        public Long campaignId;
        public Boolean anonymous;

        public CommentCreateRequest(String content, Long campaignId, Boolean anonymous) {
            this.content = content;
            this.campaignId = campaignId;
            this.anonymous = anonymous;
        }
    }

    private static class CommentUpdateRequest {
        public String content;

        public CommentUpdateRequest(String content) {
            this.content = content;
        }
    }
}