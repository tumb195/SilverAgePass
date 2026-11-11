package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.config.ApiKeyProperties;
import com.learning.silveragepass.service.KeyRequestRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的滑动窗口请求频率限制器实现
 * 使用 Redis Sorted Set 实现精确的滑动窗口算法
 * 
 * 当 Redis 不可用时，会优雅降级：允许请求通过（跳过速率限制检查）
 */
@Service
public class KeyRequestRateLimiterImpl implements KeyRequestRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(KeyRequestRateLimiterImpl.class);

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:key_request:";
    private static final String BLOCK_KEY_PREFIX = "rate_limit:blocked:";
    private static final long WINDOW_SIZE_MS = 3600000L; // 1小时窗口（毫秒）

    private final RedisTemplate<String, Object> redisTemplate;
    private final ApiKeyProperties apiKeyProperties;

    public KeyRequestRateLimiterImpl(RedisTemplate<String, Object> redisTemplate,
                                      ApiKeyProperties apiKeyProperties) {
        this.redisTemplate = redisTemplate;
        this.apiKeyProperties = apiKeyProperties;
    }

    @Override
    public boolean isAllowed(Long userId, String keyType) {
        try {
            // 首先检查是否被阻止
            if (isBlockedInternal(userId, keyType)) {
                logger.debug("User {} is blocked for key type {}", userId, keyType);
                return false;
            }

            // 检查当前窗口内的请求数量
            int currentCount = getCurrentRequestCount(userId, keyType);
            int maxRequests = apiKeyProperties.getRateLimit().getMaxRequestsPerHour();

            boolean allowed = currentCount < maxRequests;
            logger.debug("Rate limit check for user {}, keyType {}: count={}, max={}, allowed={}",
                    userId, keyType, currentCount, maxRequests, allowed);

            return allowed;
        } catch (RedisConnectionFailureException e) {
            // Redis不可用时，优雅降级：允许请求通过
            logger.warn("Redis unavailable for rate limit check, allowing request for user {}, keyType {}. Error: {}", 
                    userId, keyType, e.getMessage());
            return true;
        } catch (Exception e) {
            // 其他异常也优雅降级
            logger.error("Unexpected error during rate limit check for user {}, keyType {}, allowing request. Error: {}", 
                    userId, keyType, e.getMessage(), e);
            return true;
        }
    }

    @Override
    public int getRemainingRequests(Long userId, String keyType) {
        try {
            if (isBlockedInternal(userId, keyType)) {
                return 0;
            }

            int currentCount = getCurrentRequestCount(userId, keyType);
            int maxRequests = apiKeyProperties.getRateLimit().getMaxRequestsPerHour();
            return Math.max(0, maxRequests - currentCount);
        } catch (RedisConnectionFailureException e) {
            // Redis不可用时，返回最大值（表示无限制）
            logger.warn("Redis unavailable for getRemainingRequests, returning max for user {}, keyType {}", 
                    userId, keyType);
            return apiKeyProperties.getRateLimit().getMaxRequestsPerHour();
        } catch (Exception e) {
            logger.error("Unexpected error in getRemainingRequests for user {}, keyType {}", 
                    userId, keyType, e);
            return apiKeyProperties.getRateLimit().getMaxRequestsPerHour();
        }
    }

    @Override
    public long getResetTime(Long userId, String keyType) {
        try {
            String rateLimitKey = getRateLimitKey(userId, keyType);
            
            // 获取窗口内最早的请求时间
            Set<Object> oldestRequests = redisTemplate.opsForZSet().range(rateLimitKey, 0, 0);
            if (oldestRequests == null || oldestRequests.isEmpty()) {
                return 0;
            }

            // 最早请求的过期时间就是重置时间
            Object oldest = oldestRequests.iterator().next();
            if (oldest instanceof Number) {
                long oldestTime = ((Number) oldest).longValue();
                return (oldestTime + WINDOW_SIZE_MS) / 1000; // 转换为秒
            }

            return 0;
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis unavailable for getResetTime, returning 0 for user {}, keyType {}", 
                    userId, keyType);
            return 0;
        } catch (Exception e) {
            logger.error("Unexpected error in getResetTime for user {}, keyType {}", 
                    userId, keyType, e);
            return 0;
        }
    }

    @Override
    public void recordRequest(Long userId, String keyType) {
        try {
            String rateLimitKey = getRateLimitKey(userId, keyType);
            long now = System.currentTimeMillis();
            long windowStart = now - WINDOW_SIZE_MS;

            // 清理窗口外的旧请求
            redisTemplate.opsForZSet().removeRangeByScore(rateLimitKey, 0, windowStart);

            // 添加当前请求（使用时间戳作为 score 和 member）
            String member = String.valueOf(now) + ":" + System.nanoTime(); // 确保唯一性
            redisTemplate.opsForZSet().add(rateLimitKey, member, now);

            // 设置 key 过期时间（窗口大小 + 缓冲）
            redisTemplate.expire(rateLimitKey, WINDOW_SIZE_MS + 60000, TimeUnit.MILLISECONDS);

            // 检查是否需要阻止用户
            int currentCount = getCurrentRequestCount(userId, keyType);
            int maxRequests = apiKeyProperties.getRateLimit().getMaxRequestsPerHour();

            if (currentCount >= maxRequests) {
                blockUser(userId, keyType);
            }

            logger.debug("Recorded request for user {}, keyType {}, current count: {}", 
                    userId, keyType, currentCount);
        } catch (RedisConnectionFailureException e) {
            // Redis不可用时，跳过记录（优雅降级）
            logger.warn("Redis unavailable for recordRequest, skipping for user {}, keyType {}. Error: {}", 
                    userId, keyType, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in recordRequest for user {}, keyType {}, skipping", 
                    userId, keyType, e);
        }
    }

    @Override
    public boolean isBlocked(Long userId, String keyType) {
        try {
            return isBlockedInternal(userId, keyType);
        } catch (RedisConnectionFailureException e) {
            // Redis不可用时，假设用户未被阻止
            logger.warn("Redis unavailable for isBlocked check, returning false for user {}, keyType {}", 
                    userId, keyType);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error in isBlocked for user {}, keyType {}, returning false", 
                    userId, keyType, e);
            return false;
        }
    }
    
    /**
     * 内部方法：检查用户是否被阻止（不捕获异常）
     */
    private boolean isBlockedInternal(Long userId, String keyType) {
        String blockKey = getBlockKey(userId, keyType);
        Boolean hasKey = redisTemplate.hasKey(blockKey);
        return Boolean.TRUE.equals(hasKey);
    }

    @Override
    public long getBlockRemainingSeconds(Long userId, String keyType) {
        try {
            String blockKey = getBlockKey(userId, keyType);
            Long ttl = redisTemplate.getExpire(blockKey, TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (RedisConnectionFailureException e) {
            logger.warn("Redis unavailable for getBlockRemainingSeconds, returning 0 for user {}, keyType {}", 
                    userId, keyType);
            return 0;
        } catch (Exception e) {
            logger.error("Unexpected error in getBlockRemainingSeconds for user {}, keyType {}", 
                    userId, keyType, e);
            return 0;
        }
    }

    /**
     * 获取当前窗口内的请求数量
     */
    private int getCurrentRequestCount(Long userId, String keyType) {
        String rateLimitKey = getRateLimitKey(userId, keyType);
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_SIZE_MS;

        // 清理窗口外的旧请求
        redisTemplate.opsForZSet().removeRangeByScore(rateLimitKey, 0, windowStart);

        // 统计窗口内的请求数量
        Long count = redisTemplate.opsForZSet().zCard(rateLimitKey);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 阻止用户
     */
    private void blockUser(Long userId, String keyType) {
        String blockKey = getBlockKey(userId, keyType);
        int blockDuration = apiKeyProperties.getRateLimit().getBlockDurationMinutes();
        
        redisTemplate.opsForValue().set(blockKey, "blocked", blockDuration, TimeUnit.MINUTES);
        logger.warn("User {} blocked for key type {} for {} minutes", userId, keyType, blockDuration);
    }

    /**
     * 生成频率限制 Redis key
     */
    private String getRateLimitKey(Long userId, String keyType) {
        return RATE_LIMIT_KEY_PREFIX + userId + ":" + keyType;
    }

    /**
     * 生成阻止状态 Redis key
     */
    private String getBlockKey(Long userId, String keyType) {
        return BLOCK_KEY_PREFIX + userId + ":" + keyType;
    }
}
