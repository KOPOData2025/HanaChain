package com.hanachain.hanachainbackend.util;

import com.hanachain.hanachainbackend.exception.WalletCreationException;
import com.hanachain.hanachainbackend.exception.WalletDecryptionException;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;

/**
 * 지갑 개인키 암호화/복호화를 위한 유틸리티 클래스
 * AES-256-GCM 암호화를 사용하여 개인키를 안전하게 보호합니다.
 */
@Slf4j
public class WalletEncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    
    private static final int KEY_LENGTH = 256; // AES-256
    private static final int IV_LENGTH = 12;   // GCM IV length
    private static final int SALT_LENGTH = 16; // Salt length
    private static final int TAG_LENGTH = 16;  // GCM tag length
    private static final int ITERATION_COUNT = 100000; // PBKDF2 iterations
    
    /**
     * 개인키를 AES-256-GCM으로 암호화합니다
     * 
     * @param privateKey 암호화할 개인키 (16진수 문자열)
     * @param password 암호화에 사용할 비밀번호
     * @return 암호화된 데이터 (Base64 인코딩된 문자열)
     * @throws WalletCreationException 암호화 실패 시
     */
    public static String encrypt(String privateKey, String password) {
        try {
            // 솔트 생성
            byte[] salt = generateSalt();
            
            // 비밀번호에서 키 파생
            SecretKey secretKey = deriveKeyFromPassword(password, salt);
            
            // IV 생성
            byte[] iv = generateIV();
            
            // 암호화 수행
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            byte[] privateKeyBytes = privateKey.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedData = cipher.doFinal(privateKeyBytes);
            
            // 솔트 + IV + 암호화된 데이터를 결합
            byte[] result = new byte[SALT_LENGTH + IV_LENGTH + encryptedData.length];
            System.arraycopy(salt, 0, result, 0, SALT_LENGTH);
            System.arraycopy(iv, 0, result, SALT_LENGTH, IV_LENGTH);
            System.arraycopy(encryptedData, 0, result, SALT_LENGTH + IV_LENGTH, encryptedData.length);
            
            // Base64 인코딩하여 반환
            return Base64.getEncoder().encodeToString(result);
            
        } catch (Exception e) {
            log.error("Private key encryption failed", e);
            throw WalletCreationException.encryptionFailed();
        }
    }
    
    /**
     * 암호화된 개인키를 복호화합니다
     * 
     * @param encryptedPrivateKey 암호화된 개인키 (Base64 인코딩된 문자열)
     * @param password 복호화에 사용할 비밀번호
     * @return 복호화된 개인키 (16진수 문자열)
     * @throws WalletDecryptionException 복호화 실패 시
     */
    public static String decrypt(String encryptedPrivateKey, String password) {
        try {
            // Base64 디코딩
            byte[] encryptedData = Base64.getDecoder().decode(encryptedPrivateKey);
            
            if (encryptedData.length < SALT_LENGTH + IV_LENGTH) {
                throw WalletDecryptionException.corruptedData();
            }
            
            // 솔트, IV, 암호화된 데이터 분리
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[encryptedData.length - SALT_LENGTH - IV_LENGTH];
            
            System.arraycopy(encryptedData, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(encryptedData, SALT_LENGTH, iv, 0, IV_LENGTH);
            System.arraycopy(encryptedData, SALT_LENGTH + IV_LENGTH, cipherText, 0, cipherText.length);
            
            // 비밀번호에서 키 파생
            SecretKey secretKey = deriveKeyFromPassword(password, salt);
            
            // 복호화 수행
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            byte[] decryptedData = cipher.doFinal(cipherText);
            
            return new String(decryptedData, StandardCharsets.UTF_8);
            
        } catch (javax.crypto.AEADBadTagException e) {
            log.error("Private key decryption failed - invalid password", e);
            throw WalletDecryptionException.invalidPassword();
        } catch (javax.crypto.BadPaddingException e) {
            log.error("Private key decryption failed - invalid password", e);
            throw WalletDecryptionException.invalidPassword();
        } catch (Exception e) {
            log.error("Private key decryption failed", e);
            throw WalletDecryptionException.decryptionFailed();
        }
    }
    
    /**
     * 비밀번호가 올바른지 검증합니다
     * 
     * @param encryptedPrivateKey 암호화된 개인키
     * @param password 검증할 비밀번호
     * @return 비밀번호 유효성 여부
     */
    public static boolean validatePassword(String encryptedPrivateKey, String password) {
        try {
            decrypt(encryptedPrivateKey, password);
            return true;
        } catch (WalletDecryptionException e) {
            return false;
        }
    }
    
    /**
     * 암호화 강도를 테스트합니다 (개발/테스트용)
     * 
     * @param password 테스트할 비밀번호
     * @return 암호화 강도 점수 (1-10)
     */
    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int score = 0;
        
        // 길이 체크
        if (password.length() >= 8) score += 2;
        if (password.length() >= 12) score += 1;
        if (password.length() >= 16) score += 1;
        
        // 복잡성 체크
        if (password.matches(".*[a-z].*")) score += 1; // 소문자
        if (password.matches(".*[A-Z].*")) score += 1; // 대문자
        if (password.matches(".*[0-9].*")) score += 1; // 숫자
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) score += 2; // 특수문자
        if (!password.matches(".*(.)\\1{2,}.*")) score += 1; // 연속 문자 없음
        
        return Math.min(score, 10);
    }
    
    /**
     * 랜덤 솔트를 생성합니다
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }
    
    /**
     * 랜덤 IV를 생성합니다
     */
    private static byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    
    /**
     * PBKDF2를 사용하여 비밀번호에서 암호화 키를 파생합니다
     */
    private static SecretKey deriveKeyFromPassword(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * 테스트용 암호화 키 생성
     */
    public static SecretKey generateTestKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_LENGTH);
        return keyGenerator.generateKey();
    }
}