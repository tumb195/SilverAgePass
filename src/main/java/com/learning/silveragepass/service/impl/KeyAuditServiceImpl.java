package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.KeyRequestStatsDTO;
import com.learning.silveragepass.entity.KeyRequestLog;
import com.learning.silveragepass.repository.KeyRequestLogRepository;
import com.learning.silveragepass.service.KeyAuditService;
import com.learning.silveragepass.service.KeyRequestRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API Key 审计服务实现
 * 负责记录和统计 API Key 请求日志
 */
@Service
public class KeyAuditServiceImpl implements KeyAuditService {

    private static final Logger logger = LoggerFactory.getLogger(KeyAuditServiceImpl.class);
    private static final int DEFAULT_RETENTION_DAYS = 30;

    private final KeyRequestLogRepository keyRequestLogRepository;
    private final KeyRequestRateLimiter rateLimiter;

    public KeyAuditServiceImpl(KeyRequestLogRepository keyRequestLogRepository,
                                KeyRequestRateLimiter rateLimiter) {
        this.keyRequestLogRepository = keyRequestLogRepository;
        this.rateLimiter = rateLimiter;
    }

    @Override
    @Async
    @Transactional
    public void logSuccessfulRequest(Long userId, String keyType, String clientIp) {
        KeyRequestLog log = new KeyRequestLog(userId, keyType, clientIp, true);
        keyRequestLogRepository.save(log);
        logger.debug("Logged successful key request: userId={}, keyType={}, clientIp={}", 
                userId, keyType, clientIp);
    }

    @Override
    @Async
    @Transactional
    public void logFailedRequest(Long userId, String keyType, String clientIp, String failureReason) {
        KeyRequestLog log = new KeyRequestLog(userId, keyType, clientIp, failureReason);
        keyRequestLogRepository.save(log);
        logger.debug("Logged failed key request: userId={}, keyType={}, reason={}", 
                userId, keyType, failureReason);
    }

    @Override
    @Transactional(readOnly = true)
    public KeyRequestStatsDTO getRequestStats(Long userId) {
        return getRequestStats(userId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public KeyRequestStatsDTO getRequestStats(Long userId, String keyType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime oneHourAgo = now.minusHours(1);

        // 获取今日请求数
        long requestsToday = keyRequestLogRepository.countTodayRequestsByUserId(userId, todayStart);

        // 获取最近一小时请求数
        long requestsThisHour = keyRequestLogRepository.countLastHourRequestsByUserId(userId, oneHourAgo);

        // 获取最后请求时间
        LocalDateTime lastRequestTime = keyRequestLogRepository.findLastRequestTimeByUserId(userId);

        // 获取频率限制状态
        String effectiveKeyType = keyType != null ? keyType : "amap"; // 默认使用 amap
        boolean isBlocked = rateLimiter.isBlocked(userId, effectiveKeyType);
        long blockRemainingSeconds = rateLimiter.getBlockRemainingSeconds(userId, effectiveKeyType);
        int remainingRequests = rateLimiter.getRemainingRequests(userId, effectiveKeyType);
        long resetTimeEpoch = rateLimiter.getResetTime(userId, effectiveKeyType);

        LocalDateTime resetTime = null;
        if (resetTimeEpoch > 0) {
            resetTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(resetTimeEpoch), 
                    ZoneId.systemDefault());
        }

        return KeyRequestStatsDTO.builder()
                .userId(userId)
                .keyType(keyType)
                .requestsToday((int) requestsToday)
                .requestsThisHour((int) requestsThisHour)
                .lastRequestTime(lastRequestTime)
                .isBlocked(isBlocked)
                .blockRemainingSeconds(blockRemainingSeconds)
                .remainingRequests(remainingRequests)
                .resetTime(resetTime)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KeyRequestLog> getUserRequestLogs(Long userId, Pageable pageable) {
        return keyRequestLogRepository.findByUserIdOrderByRequestTimeDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KeyRequestLog> getRequestLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return keyRequestLogRepository.findByRequestTimeBetweenOrderByRequestTimeDesc(startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStats() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(DEFAULT_RETENTION_DAYS);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", keyRequestLogRepository.countByUserIdAndRequestTimeAfter(null, thirtyDaysAgo));
        stats.put("successfulRequests", keyRequestLogRepository.countSuccessfulRequestsAfter(thirtyDaysAgo));
        stats.put("failedRequests", keyRequestLogRepository.countFailedRequestsAfter(thirtyDaysAgo));
        stats.put("statsPeriodDays", DEFAULT_RETENTION_DAYS);
        stats.put("requestsByKeyType", getStatsByKeyType(thirtyDaysAgo));

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStatsByKeyType(LocalDateTime startTime) {
        List<Object[]> results = keyRequestLogRepository.countByKeyTypeAfter(startTime);
        Map<String, Long> statsByType = new HashMap<>();

        for (Object[] result : results) {
            String keyType = (String) result[0];
            Long count = (Long) result[1];
            statsByType.put(keyType, count);
        }

        return statsByType;
    }

    @Override
    @Transactional
    public long cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
        
        // 先统计要删除的数量
        long countBefore = keyRequestLogRepository.count();
        
        keyRequestLogRepository.deleteByRequestTimeBefore(cutoffTime);
        
        long countAfter = keyRequestLogRepository.count();
        long deletedCount = countBefore - countAfter;
        
        logger.info("Cleaned up {} old key request logs (older than {} days)", deletedCount, daysToKeep);
        return deletedCount;
    }
}
