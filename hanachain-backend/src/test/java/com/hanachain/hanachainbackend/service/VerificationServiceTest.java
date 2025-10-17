package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.entity.VerificationSession;
import com.hanachain.hanachainbackend.repository.VerificationSessionRepository;
import com.hanachain.hanachainbackend.service.impl.VerificationServiceImpl;
import com.hanachain.hanachainbackend.util.VerificationCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class VerificationServiceTest {
    
    @Mock
    private VerificationSessionRepository verificationSessionRepository;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private VerificationCodeGenerator codeGenerator;
    
    @InjectMocks
    private VerificationServiceImpl verificationService;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verificationService, "verificationExpirationMs", 300000L);
    }
    
    @Test
    void testCreateAndSendVerificationCode() {
        // Given
        String email = "test@example.com";
        VerificationSession.VerificationType type = VerificationSession.VerificationType.EMAIL_REGISTRATION;
        String code = "123456";
        
        when(codeGenerator.generateCode()).thenReturn(code);
        when(verificationSessionRepository.save(any(VerificationSession.class)))
                .thenReturn(new VerificationSession());
        
        // When
        verificationService.createAndSendVerificationCode(email, type);
        
        // Then
        verify(verificationSessionRepository).deleteByEmailAndType(email, type);
        verify(verificationSessionRepository).save(any(VerificationSession.class));
        verify(emailService).sendVerificationEmail(email, code, type);
    }
    
    @Test
    void testVerifyCodeSuccess() {
        // Given
        String email = "test@example.com";
        String code = "123456";
        VerificationSession.VerificationType type = VerificationSession.VerificationType.EMAIL_REGISTRATION;
        
        VerificationSession session = VerificationSession.builder()
                .email(email)
                .verificationCode(code)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .verified(false)
                .build();
        
        when(verificationSessionRepository.findLatestUnverifiedSession(
                anyString(), any(VerificationSession.VerificationType.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(session));
        
        // When
        boolean result = verificationService.verifyCode(email, code, type);
        
        // Then
        assertThat(result).isTrue();
        verify(verificationSessionRepository, times(2)).save(session);
    }
    
    @Test
    void testVerifyCodeInvalidCode() {
        // Given
        String email = "test@example.com";
        String code = "123456";
        String wrongCode = "654321";
        VerificationSession.VerificationType type = VerificationSession.VerificationType.EMAIL_REGISTRATION;
        
        VerificationSession session = VerificationSession.builder()
                .email(email)
                .verificationCode(code)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .verified(false)
                .build();
        
        when(verificationSessionRepository.findLatestUnverifiedSession(
                anyString(), any(VerificationSession.VerificationType.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(session));
        
        // When
        boolean result = verificationService.verifyCode(email, wrongCode, type);
        
        // Then
        assertThat(result).isFalse();
        verify(verificationSessionRepository).save(session);
    }
    
    @Test
    void testVerifyCodeExpired() {
        // Given
        String email = "test@example.com";
        String code = "123456";
        VerificationSession.VerificationType type = VerificationSession.VerificationType.EMAIL_REGISTRATION;
        
        VerificationSession session = VerificationSession.builder()
                .email(email)
                .verificationCode(code)
                .type(type)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // 만료됨
                .attemptCount(0)
                .verified(false)
                .build();
        
        when(verificationSessionRepository.findLatestUnverifiedSession(
                anyString(), any(VerificationSession.VerificationType.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(session));
        
        // When
        boolean result = verificationService.verifyCode(email, code, type);
        
        // Then
        assertThat(result).isFalse();
        verify(verificationSessionRepository).save(session);
    }
}
