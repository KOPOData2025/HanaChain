package com.hanachain.hanachainbackend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
class JwtTokenProviderTest {
    
    private JwtTokenProvider jwtTokenProvider;
    
    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "test-secret-key-for-jwt-token-generation-2024-very-long-key-for-testing-purposes-to-meet-minimum-requirements-for-hs512-algorithm-security-validation-requirements-testing");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 60000);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpirationMs", 120000);
    }
    
    @Test
    void testGenerateAccessToken() {
        // Given
        String username = "test@example.com";
        
        // When
        String token = jwtTokenProvider.generateAccessToken(username);
        
        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }
    
    @Test
    void testGetUsernameFromToken() {
        // Given
        String username = "test@example.com";
        String token = jwtTokenProvider.generateAccessToken(username);
        
        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        
        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }
    
    @Test
    void testValidateToken() {
        // Given
        String username = "test@example.com";
        String token = jwtTokenProvider.generateAccessToken(username);
        
        // When & Then
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.validateToken("invalid-token")).isFalse();
    }
    
    @Test
    void testGenerateRefreshToken() {
        // Given
        String username = "test@example.com";
        
        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);
        
        // Then
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
    }
}
