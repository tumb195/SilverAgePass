package com.learning.silveragepass.service;

import com.learning.silveragepass.config.ApiKeyProperties;
import com.learning.silveragepass.dto.EncryptedKeyDTO;
import com.learning.silveragepass.service.impl.KeyEncryptionServiceImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for KeyEncryptionService
 * 
 * **Feature: api-key-security-enhancement**
 * **Validates: Requirements 1.6, 1.7, 1.8**
 */
public class KeyEncryptionServicePropertyTest {

    // Test secret key for encryption
    private static final String TEST_SECRET_KEY = "test-secret-key-for-property-testing-32bytes";
    
    // Create a test instance of the encryption service
    private KeyEncryptionService createTestService() {
        ApiKeyProperties properties = new ApiKeyProperties();
        ApiKeyProperties.EncryptionConfig encryptionConfig = new ApiKeyProperties.EncryptionConfig();
        encryptionConfig.setSecretKey(TEST_SECRET_KEY);
        encryptionConfig.setTimestampToleranceMs(300000L);
        properties.setEncryption(encryptionConfig);
        
        ApiKeyProperties.AmapConfig amapConfig = new ApiKeyProperties.AmapConfig();
        amapConfig.setKeyExpirationSeconds(300L);
        properties.setAmap(amapConfig);
        
        return new KeyEncryptionServiceImpl(properties);
    }

    // ==================== Property 3: Encryption round-trip ====================
    
    /**
     * Property 3: Encryption round-trip
     * 
     * *For any* valid API key string and timestamp, encrypting then decrypting
     * SHALL produce the original key.
     * 
     * **Feature: api-key-security-enhancement, Property 3: Encryption round-trip**
     * **Validates: Requirements 1.6, 1.7**
     */
    @Property(tries = 100)
    void encryptThenDecryptShouldReturnOriginalKey(
            @ForAll @AlphaChars @StringLength(min = 10, max = 50) String originalKey,
            @ForAll @LongRange(min = 1000000000000L, max = 2000000000000L) long timestamp) {
        
        KeyEncryptionService service = createTestService();
        
        // Encrypt
        EncryptedKeyDTO encrypted = service.encryptWithTimestamp(originalKey, timestamp);
        
        // Decrypt
        String decrypted = service.decrypt(encrypted.getEncryptedKey(), timestamp);
        
        // Verify round-trip
        assertThat(decrypted).isEqualTo(originalKey);
    }
    
    /**
     * Property 3 continued: Round-trip with special characters
     * 
     * **Feature: api-key-security-enhancement, Property 3: Encryption round-trip**
     * **Validates: Requirements 1.6, 1.7**
     */
    @Property(tries = 50)
    void encryptThenDecryptShouldWorkWithSpecialCharacters(
            @ForAll("apiKeyWithSpecialChars") String originalKey,
            @ForAll @LongRange(min = 1000000000000L, max = 2000000000000L) long timestamp) {
        
        KeyEncryptionService service = createTestService();
        
        // Encrypt
        EncryptedKeyDTO encrypted = service.encryptWithTimestamp(originalKey, timestamp);
        
        // Decrypt
        String decrypted = service.decrypt(encrypted.getEncryptedKey(), timestamp);
        
        // Verify round-trip
        assertThat(decrypted).isEqualTo(originalKey);
    }
    
    /**
     * Property 3 continued: Same key encrypted with different timestamps
     * should produce different ciphertexts
     * 
     * **Feature: api-key-security-enhancement, Property 3: Encryption round-trip**
     * **Validates: Requirements 1.6, 1.7**
     */
    @Property(tries = 50)
    void sameKeyWithDifferentTimestampsShouldProduceDifferentCiphertexts(
            @ForAll @AlphaChars @StringLength(min = 10, max = 50) String originalKey,
            @ForAll @LongRange(min = 1000000000000L, max = 1500000000000L) long timestamp1,
            @ForAll @LongRange(min = 1500000000001L, max = 2000000000000L) long timestamp2) {
        
        KeyEncryptionService service = createTestService();
        
        // Encrypt with different timestamps
        EncryptedKeyDTO encrypted1 = service.encryptWithTimestamp(originalKey, timestamp1);
        EncryptedKeyDTO encrypted2 = service.encryptWithTimestamp(originalKey, timestamp2);
        
        // Ciphertexts should be different
        assertThat(encrypted1.getEncryptedKey()).isNotEqualTo(encrypted2.getEncryptedKey());
        
        // But both should decrypt to the same original key
        String decrypted1 = service.decrypt(encrypted1.getEncryptedKey(), timestamp1);
        String decrypted2 = service.decrypt(encrypted2.getEncryptedKey(), timestamp2);
        
        assertThat(decrypted1).isEqualTo(originalKey);
        assertThat(decrypted2).isEqualTo(originalKey);
    }

    // ==================== Property 4: Response structure completeness ====================
    
    /**
     * Property 4: Response structure completeness
     * 
     * *For any* encryption operation, the response SHALL contain all required fields:
     * encryptedKey, timestamp, expiresIn, and algorithm.
     * 
     * **Feature: api-key-security-enhancement, Property 4: Response structure completeness**
     * **Validates: Requirements 1.8**
     */
    @Property(tries = 100)
    void encryptedResponseShouldContainAllRequiredFields(
            @ForAll @AlphaChars @StringLength(min = 10, max = 50) String originalKey,
            @ForAll @LongRange(min = 1000000000000L, max = 2000000000000L) long timestamp) {
        
        KeyEncryptionService service = createTestService();
        
        EncryptedKeyDTO response = service.encryptWithTimestamp(originalKey, timestamp);
        
        // Verify all required fields are present
        assertThat(response.getEncryptedKey()).isNotNull().isNotEmpty();
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getExpiresIn()).isGreaterThan(0);
        assertThat(response.getAlgorithm()).isEqualTo("AES-256-CBC");
    }
    
    /**
     * Property 4 continued: Encrypted key should be Base64 encoded
     * 
     * **Feature: api-key-security-enhancement, Property 4: Response structure completeness**
     * **Validates: Requirements 1.8**
     */
    @Property(tries = 100)
    void encryptedKeyShouldBeValidBase64(
            @ForAll @AlphaChars @StringLength(min = 10, max = 50) String originalKey,
            @ForAll @LongRange(min = 1000000000000L, max = 2000000000000L) long timestamp) {
        
        KeyEncryptionService service = createTestService();
        
        EncryptedKeyDTO response = service.encryptWithTimestamp(originalKey, timestamp);
        
        // Verify encrypted key is valid Base64
        String encryptedKey = response.getEncryptedKey();
        assertThat(encryptedKey).matches("^[A-Za-z0-9+/=]+$");
        
        // Should be decodable
        byte[] decoded = java.util.Base64.getDecoder().decode(encryptedKey);
        assertThat(decoded).isNotEmpty();
    }
    
    /**
     * Property 4 continued: ExpiresIn should match configuration
     * 
     * **Feature: api-key-security-enhancement, Property 4: Response structure completeness**
     * **Validates: Requirements 1.8**
     */
    @Property(tries = 50)
    void expiresInShouldMatchConfiguration(
            @ForAll @AlphaChars @StringLength(min = 10, max = 50) String originalKey,
            @ForAll @LongRange(min = 1000000000000L, max = 2000000000000L) long timestamp) {
        
        KeyEncryptionService service = createTestService();
        
        EncryptedKeyDTO response = service.encryptWithTimestamp(originalKey, timestamp);
        
        // ExpiresIn should be 300 (configured value)
        assertThat(response.getExpiresIn()).isEqualTo(300L);
    }

    // ==================== Providers ====================
    
    @Provide
    Arbitrary<String> apiKeyWithSpecialChars() {
        return Arbitraries.strings()
                .withChars('a', 'z', 'A', 'Z', '0', '9', '-', '_', '.')
                .ofMinLength(10)
                .ofMaxLength(50);
    }
}
