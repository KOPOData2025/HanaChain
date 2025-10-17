package com.hanachain.hanachainbackend.util;

import com.hanachain.hanachainbackend.exception.WalletCreationException;
import com.hanachain.hanachainbackend.exception.WalletDecryptionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("WalletEncryptionUtil 테스트")
class WalletEncryptionUtilTest {
    
    private static final String TEST_PRIVATE_KEY = "0x1234567890123456789012345678901234567890123456789012345678901234";
    private static final String TEST_PASSWORD = "TestPassword123!";
    
    @Test
    @DisplayName("개인키 암호화 및 복호화 성공")
    void encryptAndDecrypt_Success() {
        // When
        String encrypted = WalletEncryptionUtil.encrypt(TEST_PRIVATE_KEY, TEST_PASSWORD);
        String decrypted = WalletEncryptionUtil.decrypt(encrypted, TEST_PASSWORD);
        
        // Then
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(TEST_PRIVATE_KEY);
        assertThat(decrypted).isEqualTo(TEST_PRIVATE_KEY);
    }
    
    @Test
    @DisplayName("잘못된 비밀번호로 복호화 실패")
    void decrypt_WrongPassword_ThrowsException() {
        // Given
        String encrypted = WalletEncryptionUtil.encrypt(TEST_PRIVATE_KEY, TEST_PASSWORD);
        String wrongPassword = "WrongPassword";
        
        // When & Then
        assertThatThrownBy(() -> WalletEncryptionUtil.decrypt(encrypted, wrongPassword))
                .isInstanceOf(WalletDecryptionException.class);
    }
    
    @Test
    @DisplayName("손상된 암호화 데이터 복호화 실패")
    void decrypt_CorruptedData_ThrowsException() {
        // Given
        String corruptedData = "corrupted_base64_data";
        
        // When & Then
        assertThatThrownBy(() -> WalletEncryptionUtil.decrypt(corruptedData, TEST_PASSWORD))
                .isInstanceOf(WalletDecryptionException.class);
    }
    
    @Test
    @DisplayName("비밀번호 유효성 검증 성공")
    void validatePassword_CorrectPassword_ReturnsTrue() {
        // Given
        String encrypted = WalletEncryptionUtil.encrypt(TEST_PRIVATE_KEY, TEST_PASSWORD);
        
        // When
        boolean isValid = WalletEncryptionUtil.validatePassword(encrypted, TEST_PASSWORD);
        
        // Then
        assertThat(isValid).isTrue();
    }
    
    @Test
    @DisplayName("비밀번호 유효성 검증 실패")
    void validatePassword_WrongPassword_ReturnsFalse() {
        // Given
        String encrypted = WalletEncryptionUtil.encrypt(TEST_PRIVATE_KEY, TEST_PASSWORD);
        String wrongPassword = "WrongPassword";
        
        // When
        boolean isValid = WalletEncryptionUtil.validatePassword(encrypted, wrongPassword);
        
        // Then
        assertThat(isValid).isFalse();
    }
    
    @Test
    @DisplayName("비밀번호 강도 테스트 - 강한 비밀번호")
    void getPasswordStrength_StrongPassword_ReturnsHighScore() {
        // Given
        String strongPassword = "StrongP@ssw0rd123!";
        
        // When
        int strength = WalletEncryptionUtil.getPasswordStrength(strongPassword);
        
        // Then
        assertThat(strength).isGreaterThanOrEqualTo(8);
    }
    
    @Test
    @DisplayName("비밀번호 강도 테스트 - 약한 비밀번호")
    void getPasswordStrength_WeakPassword_ReturnsLowScore() {
        // Given
        String weakPassword = "123";
        
        // When
        int strength = WalletEncryptionUtil.getPasswordStrength(weakPassword);
        
        // Then
        assertThat(strength).isLessThan(5);
    }
    
    @Test
    @DisplayName("비밀번호 강도 테스트 - null 또는 빈 비밀번호")
    void getPasswordStrength_NullOrEmpty_ReturnsZero() {
        // When & Then
        assertThat(WalletEncryptionUtil.getPasswordStrength(null)).isEqualTo(0);
        assertThat(WalletEncryptionUtil.getPasswordStrength("")).isEqualTo(0);
    }
    
    @Test
    @DisplayName("암호화 일관성 테스트 - 같은 데이터도 다른 암호화 결과")
    void encrypt_SameData_ProducesDifferentResults() {
        // When
        String encrypted1 = WalletEncryptionUtil.encrypt(TEST_PRIVATE_KEY, TEST_PASSWORD);
        String encrypted2 = WalletEncryptionUtil.encrypt(TEST_PRIVATE_KEY, TEST_PASSWORD);
        
        // Then
        assertThat(encrypted1).isNotEqualTo(encrypted2); // 솔트와 IV가 다르므로 결과가 달라야 함
        
        // 하지만 둘 다 정상적으로 복호화되어야 함
        String decrypted1 = WalletEncryptionUtil.decrypt(encrypted1, TEST_PASSWORD);
        String decrypted2 = WalletEncryptionUtil.decrypt(encrypted2, TEST_PASSWORD);
        
        assertThat(decrypted1).isEqualTo(TEST_PRIVATE_KEY);
        assertThat(decrypted2).isEqualTo(TEST_PRIVATE_KEY);
    }
    
    @Test
    @DisplayName("긴 개인키 암호화 및 복호화")
    void encryptAndDecrypt_LongPrivateKey_Success() {
        // Given
        String longPrivateKey = "0x" + "1234567890abcdef".repeat(4); // 64자리 16진수
        
        // When
        String encrypted = WalletEncryptionUtil.encrypt(longPrivateKey, TEST_PASSWORD);
        String decrypted = WalletEncryptionUtil.decrypt(encrypted, TEST_PASSWORD);
        
        // Then
        assertThat(decrypted).isEqualTo(longPrivateKey);
    }
    
    @Test
    @DisplayName("특수문자가 포함된 비밀번호로 암호화")
    void encryptAndDecrypt_SpecialCharacterPassword_Success() {
        // Given
        String specialPassword = "P@$$w0rd!@#$%^&*()";
        
        // When
        String encrypted = WalletEncryptionUtil.encrypt(TEST_PRIVATE_KEY, specialPassword);
        String decrypted = WalletEncryptionUtil.decrypt(encrypted, specialPassword);
        
        // Then
        assertThat(decrypted).isEqualTo(TEST_PRIVATE_KEY);
    }
}