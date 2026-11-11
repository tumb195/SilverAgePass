package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.exception.EncryptionException;
import com.learning.silveragepass.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密服务实现
 * 使用 AES-256-GCM 算法进行加密，提供认证加密（AEAD）
 * GCM 模式提供了更好的安全性和性能，无需单独的 MAC
 */
@Service
public class EncryptionServiceImpl implements EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionServiceImpl.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // 128 bits authentication tag

    @Value("${app.encryption.secret-key:SilverAgePassMessageEncryptionKey32}")
    private String secretKey;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            throw new EncryptionException("Plain text cannot be null or empty");
        }

        try {
            byte[] secretKeyBytes = getSecretKeyBytes();
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, KEY_ALGORITHM);
            
            // Generate random IV for each encryption
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted data: [IV][encrypted data]
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);
            
            String result = Base64.getEncoder().encodeToString(byteBuffer.array());
            
            logger.debug("Successfully encrypted text of length: {}", plainText.length());
            
            return result;
        } catch (Exception e) {
            logger.error("Failed to encrypt text: {}", e.getMessage());
            throw new EncryptionException("Failed to encrypt text", e);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            throw new EncryptionException("Encrypted text cannot be null or empty");
        }

        try {
            byte[] secretKeyBytes = getSecretKeyBytes();
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, KEY_ALGORITHM);

            // Decode Base64 and extract IV and encrypted data
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            
            if (decodedBytes.length < GCM_IV_LENGTH) {
                throw new EncryptionException("Invalid encrypted text format: too short");
            }
            
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);
            
            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            
            // Extract encrypted data
            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);
            
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            String result = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            logger.debug("Successfully decrypted text");
            
            return result;
        } catch (Exception e) {
            logger.error("Failed to decrypt text: {}", e.getMessage());
            throw new EncryptionException("Failed to decrypt text", e);
        }
    }

    @Override
    public boolean validateEncryptedText(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return false;
        }

        try {
            // Try to decrypt - if successful, the text is valid
            decrypt(encryptedText);
            return true;
        } catch (Exception e) {
            logger.debug("Encrypted text validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the secret key bytes, ensuring it's exactly 32 bytes for AES-256
     */
    private byte[] getSecretKeyBytes() {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new EncryptionException("Encryption secret key is not configured");
        }

        try {
            // Use SHA-256 to ensure we always get exactly 32 bytes
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new EncryptionException("Failed to generate secret key bytes", e);
        }
    }
}
