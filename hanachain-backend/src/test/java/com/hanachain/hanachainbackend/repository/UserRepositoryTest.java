package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testFindByEmail() {
        // Given
        User user = User.builder()
                .email("test@example.com")
                .password("password")
                .name("Test User")
                .emailVerified(true)
                .termsAccepted(true)
                .privacyAccepted(true)
                .build();
        
        entityManager.persistAndFlush(user);
        
        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
    }
    
    @Test
    void testExistsByEmail() {
        // Given
        User user = User.builder()
                .email("existing@example.com")
                .password("password")
                .name("Existing User")
                .emailVerified(true)
                .termsAccepted(true)
                .privacyAccepted(true)
                .build();
        
        entityManager.persistAndFlush(user);
        
        // When & Then
        assertThat(userRepository.existsByEmail("existing@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }
    
    @Test
    void testFindByEmailAndEnabled() {
        // Given
        User enabledUser = User.builder()
                .email("enabled@example.com")
                .password("password")
                .name("Enabled User")
                .emailVerified(true)
                .enabled(true)
                .termsAccepted(true)
                .privacyAccepted(true)
                .build();
        
        User disabledUser = User.builder()
                .email("disabled@example.com")
                .password("password")
                .name("Disabled User")
                .emailVerified(true)
                .enabled(false)
                .termsAccepted(true)
                .privacyAccepted(true)
                .build();
        
        entityManager.persistAndFlush(enabledUser);
        entityManager.persistAndFlush(disabledUser);
        
        // When & Then
        assertThat(userRepository.findByEmailAndEnabled("enabled@example.com")).isPresent();
        assertThat(userRepository.findByEmailAndEnabled("disabled@example.com")).isEmpty();
    }
}
