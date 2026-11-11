package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.config.ApiKeyProperties;
import com.learning.silveragepass.dto.EncryptedKeyDTO;
import com.learning.silveragepass.exception.EncryptionException;
import com.learning.silveragepass.service.KeyEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Key 加密服务实现
 * 使用 AES-256-CBC 算法进行加密，IV 基于时间戳生成
 */
@Service
public class KeyEncryptionServiceImpl implements KeyEncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(KeyEncryptionServiceImpl.class);
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int IV_LENGTH = 16;

    private final ApiKeyProperties apiKeyProperties;

    public KeyEncryptionServiceImpl(ApiKeyProperties apiKeyProperties) {
        this.apiKeyProperties = apiKeyProperties;
    }

    @Override
    public EncryptedKeyDTO encryptWithTimestamp(String plainKey, long timestamp) {
        if (plainKey == null || plainKey.isEmpty()) {
            throw new EncryptionException("Plain key cannot be null or empty");
        }

        try {
            byte[] secretKeyBytes = getSecretKeyBytes();
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, KEY_ALGORITHM);
            
            // Generate IV from timestamp
            byte[] iv = generateIvFromTimestamp(timestamp);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(plainKey.getBytes(StandardCharsets.UTF_8));
            String encryptedKey = Base64.getEncoder().encodeToString(encryptedBytes);

            Long expiresIn = apiKeyProperties.getAmap().getKeyExpirationSeconds();

            logger.debug("Successfully encrypted key with timestamp: {}", timestamp);

            return new EncryptedKeyDTO(encryptedKey, timestamp, expiresIn, "AES-256-CBC");
        } catch (Exception e) {
            logger.error("Failed to encrypt key: {}", e.getMessage());
            throw new EncryptionException("Failed to encrypt key", e);
        }
    }


    @Override
    public String decrypt(String encryptedKey, long timestamp) {
        if (encryptedKey == null || encryptedKey.isEmpty()) {
            throw new EncryptionException("Encrypted key cannot be null or empty");
        }

        try {
            byte[] secretKeyBytes = getSecretKeyBytes();
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, KEY_ALGORITHM);

            // Generate IV from timestamp (same as encryption)
            byte[] iv = generateIvFromTimestamp(timestamp);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedKey);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            logger.debug("Successfully decrypted key with timestamp: {}", timestamp);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Failed to decrypt key: {}", e.getMessage());
            throw new EncryptionException("Failed to decrypt key", e);
        }
    }

    @Override
    public boolean validateEncryptedKey(String encryptedKey, long timestamp) {
        if (encryptedKey == null || encryptedKey.isEmpty()) {
            return false;
        }

        try {
            // Check if timestamp is within tolerance
            long currentTime = System.currentTimeMillis();
            long tolerance = apiKeyProperties.getEncryption().getTimestampToleranceMs();
            
            if (Math.abs(currentTime - timestamp) > tolerance) {
                logger.warn("Timestamp {} is outside tolerance window", timestamp);
                return false;
            }

            // Try to decrypt - if successful, the key is valid
            decrypt(encryptedKey, timestamp);
            return true;
        } catch (Exception e) {
            logger.warn("Key validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the secret key bytes, ensuring it's exactly 32 bytes for AES-256
     */
    private byte[] getSecretKeyBytes() {
        String secretKey = apiKeyProperties.getEncryption().getSecretKey();
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

    /**
     * Generate a 16-byte IV from timestamp
     * Uses SHA-256 hash of timestamp to ensure consistent IV generation
     */
    private byte[] generateIvFromTimestamp(long timestamp) {
        try {
            // Convert timestamp to bytes
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(timestamp);
            byte[] timestampBytes = buffer.array();

            // Hash the timestamp to get consistent 16 bytes
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(timestampBytes);

            // Take first 16 bytes for IV
            return Arrays.copyOf(hash, IV_LENGTH);
        } catch (Exception e) {
            throw new EncryptionException("Failed to generate IV from timestamp", e);
        }
    }
}
