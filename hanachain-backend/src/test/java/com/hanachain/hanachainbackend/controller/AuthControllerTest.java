package com.hanachain.hanachainbackend.controller;

import com.hanachain.hanachainbackend.controller.api.AuthController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanachain.hanachainbackend.dto.auth.LoginRequest;
import com.hanachain.hanachainbackend.dto.auth.LoginResponse;
import com.hanachain.hanachainbackend.dto.auth.RegisterRequest;
import com.hanachain.hanachainbackend.dto.user.UserProfileResponse;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.service.UserService;
import com.hanachain.hanachainbackend.config.WebMvcTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(WebMvcTestConfiguration.class)
@ActiveProfiles({"test", "webmvc-test"})
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private UserService userService;
    
    @Test
    @WithMockUser
    void testRegister() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setConfirmPassword("Password123!");
        request.setName("Test User");
        request.setTermsAccepted(true);
        request.setPrivacyAccepted(true);
        request.setVerificationCode("123456");
        
        User mockUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(User.Role.USER)
                .emailVerified(true)
                .enabled(true)
                .build();
        
        when(userService.registerUser(any(RegisterRequest.class))).thenReturn(mockUser);
        
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }
    
    @Test
    @WithMockUser
    void testLogin() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        
        UserProfileResponse userProfile = UserProfileResponse.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(User.Role.USER)
                .build();
        
        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(userProfile)
                .build();
        
        when(userService.loginUser(any(LoginRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }
}
