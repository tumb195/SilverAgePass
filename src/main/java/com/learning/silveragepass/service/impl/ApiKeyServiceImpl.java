package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.config.ApiKeyProperties;
import com.learning.silveragepass.dto.EncryptedKeyDTO;
import com.learning.silveragepass.dto.KeyRequestStatsDTO;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.exception.RateLimitExceededException;
import com.learning.silveragepass.service.ApiKeyService;
import com.learning.silveragepass.service.KeyAuditService;
import com.learning.silveragepass.service.KeyEncryptionService;
import com.learning.silveragepass.service.KeyRequestRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * API Key 管理服务实现
 * 集成加密服务、频率限制器和审计日志
 */
@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyServiceImpl.class);

    public static final String KEY_TYPE_AMAP = "amap";
    public static final String KEY_TYPE_QWEATHER = "qweather";

    private final ApiKeyProperties apiKeyProperties;
    private final KeyEncryptionService keyEncryptionService;
    private final KeyRequestRateLimiter rateLimiter;
    private final KeyAuditService auditService;

    public ApiKeyServiceImpl(
            ApiKeyProperties apiKeyProperties,
            KeyEncryptionService keyEncryptionService,
            KeyRequestRateLimiter rateLimiter,
            KeyAuditService auditService) {
        this.apiKeyProperties = apiKeyProperties;
        this.keyEncryptionService = keyEncryptionService;
        this.rateLimiter = rateLimiter;
        this.auditService = auditService;
    }

    @Override
    public EncryptedKeyDTO getEncryptedKey(String keyType, Long userId, String clientIp) {
        return getEncryptedKey(keyType, userId, clientIp, null);
    }
    
    @Override
    public EncryptedKeyDTO getEncryptedKey(String keyType, Long userId, String clientIp, String buildType) {
        logger.debug("Getting encrypted key for type: {}, userId: {}, buildType: {}", keyType, userId, buildType);

        // Validate key type
        String plainKey = getPlainKey(keyType, buildType);
        if (plainKey == null || plainKey.isEmpty()) {
            String reason = "Key not configured for type: " + keyType + ", buildType: " + buildType;
            auditService.logFailedRequest(userId, keyType, clientIp, reason);
            throw new BusinessException("KEY_003", "API Key 未配置");
        }

        // Check rate limit
        if (!checkRateLimit(userId, keyType)) {
            long blockRemaining = rateLimiter.getBlockRemainingSeconds(userId, keyType);
            String reason = "Rate limit exceeded";
            auditService.logFailedRequest(userId, keyType, clientIp, reason);
            logger.warn("Rate limit exceeded for userId: {}, keyType: {}", userId, keyType);
            throw new RateLimitExceededException(blockRemaining);
        }

        // Record the request
        rateLimiter.recordRequest(userId, keyType);

        // Encrypt the key with current timestamp
        long timestamp = System.currentTimeMillis();
        EncryptedKeyDTO encryptedKey = keyEncryptionService.encryptWithTimestamp(plainKey, timestamp);

        // Log successful request
        auditService.logSuccessfulRequest(userId, keyType, clientIp);

        logger.info("Successfully provided encrypted key for userId: {}, keyType: {}, buildType: {}", 
                    userId, keyType, buildType != null ? buildType : "default");
        return encryptedKey;
    }

    @Override
    public boolean checkRateLimit(Long userId, String keyType) {
        return rateLimiter.isAllowed(userId, keyType);
    }

    @Override
    public KeyRequestStatsDTO getRequestStats(Long userId, String keyType) {
        KeyRequestStatsDTO stats = auditService.getRequestStats(userId, keyType);
        
        // Enrich with rate limiter info
        boolean isBlocked = rateLimiter.isBlocked(userId, keyType);
        int remainingRequests = rateLimiter.getRemainingRequests(userId, keyType);
        long resetTime = rateLimiter.getResetTime(userId, keyType);
        long blockRemaining = rateLimiter.getBlockRemainingSeconds(userId, keyType);

        stats.setIsBlocked(isBlocked);
        stats.setRemainingRequests(remainingRequests);
        stats.setBlockRemainingSeconds(blockRemaining);
        
        if (resetTime > 0) {
            stats.setResetTime(LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(resetTime), ZoneId.systemDefault()));
        }

        return stats;
    }

    @Override
    public KeyConfigInfo getKeyConfig(String keyType) {
        return switch (keyType.toLowerCase()) {
            case KEY_TYPE_AMAP -> new KeyConfigInfo(
                    KEY_TYPE_AMAP,
                    "AES-256-CBC",
                    apiKeyProperties.getAmap().getKeyExpirationSeconds(),
                    apiKeyProperties.getRateLimit().getMaxRequestsPerHour()
            );
            case KEY_TYPE_QWEATHER -> new KeyConfigInfo(
                    KEY_TYPE_QWEATHER,
                    "AES-256-CBC",
                    apiKeyProperties.getQweather().getCacheTtlSeconds(),
                    apiKeyProperties.getRateLimit().getMaxRequestsPerHour()
            );
            default -> throw new BusinessException("KEY_004", "不支持的 Key 类型: " + keyType);
        };
    }

    /**
     * Get the plain key from configuration based on key type
     */
    private String getPlainKey(String keyType) {
        return getPlainKey(keyType, null);
    }
    
    /**
     * Get the plain key from configuration based on key type and build type
     * @param keyType Key类型 (amap, qweather)
     * @param buildType 构建类型 (debug, release)，null表示使用默认key
     */
    private String getPlainKey(String keyType, String buildType) {
        return switch (keyType.toLowerCase()) {
            case KEY_TYPE_AMAP -> apiKeyProperties.getAmap().getKeyByBuildType(buildType);
            case KEY_TYPE_QWEATHER -> apiKeyProperties.getQweather().getKey();
            default -> null;
        };
    }
}
