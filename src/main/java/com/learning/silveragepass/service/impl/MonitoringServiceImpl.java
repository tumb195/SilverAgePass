package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.config.MonitoringProperties;
import com.learning.silveragepass.service.DiaryAnalysisQueue;
import com.learning.silveragepass.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of MonitoringService for AI Health Assistant system monitoring.
 * 
 * This service provides privacy-preserving monitoring and analytics:
 * - Usage metrics without storing message content
 * - Anonymized high-risk alert logging
 * - API performance tracking
 * - Async task monitoring
 * 
 * All metrics are stored in-memory with structured logging for external monitoring systems.
 * For production, consider integrating with Prometheus, Grafana, or similar monitoring tools.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {
    
    private final DiaryAnalysisQueue diaryAnalysisQueue;
    private final MonitoringProperties monitoringProperties;
    
    // In-memory metrics storage (for production, use Redis or time-series database)
    private final Map<String, DailyMetrics> dailyMetricsCache = new ConcurrentHashMap<>();
    private final Map<String, ApiMetrics> apiMetricsCache = new ConcurrentHashMap<>();
    private final Map<String, AsyncTaskMetrics> asyncTaskMetricsCache = new ConcurrentHashMap<>();
    
    // Alert cooldown tracking (alertType -> lastAlertTime)
    private final Map<String, LocalDateTime> alertCooldownMap = new ConcurrentHashMap<>();
    
    // ==================== Usage Metrics (Requirement 15.1) ====================
    
    @Override
    public void logSessionCreated(Long userId, String sessionId) {
        String dateKey = LocalDate.now().toString();
        DailyMetrics metrics = dailyMetricsCache.computeIfAbsent(dateKey, k -> new DailyMetrics());
        
        metrics.sessionCount.incrementAndGet();
        metrics.activeUsers.add(userId);
        
        // Structured logging without sensitive content
        log.info("Session created - Date: {}, UserId: {}, SessionId: {}, TotalSessions: {}",
                dateKey, anonymizeUserId(userId), sessionId, metrics.sessionCount.get());
    }
    
    @Override
    public void logMessageSent(Long userId, String sessionId, String role) {
        String dateKey = LocalDate.now().toString();
        DailyMetrics metrics = dailyMetricsCache.computeIfAbsent(dateKey, k -> new DailyMetrics());
        
        metrics.messageCount.incrementAndGet();
        if ("user".equalsIgnoreCase(role)) {
            metrics.userMessageCount.incrementAndGet();
        } else if ("assistant".equalsIgnoreCase(role)) {
            metrics.assistantMessageCount.incrementAndGet();
        }
        
        // Log without message content
        log.debug("Message sent - Date: {}, UserId: {}, SessionId: {}, Role: {}, TotalMessages: {}",
                dateKey, anonymizeUserId(userId), sessionId, role, metrics.messageCount.get());
    }
    
    @Override
    public void logFeatureUsage(Long userId, String featureName) {
        String dateKey = LocalDate.now().toString();
        DailyMetrics metrics = dailyMetricsCache.computeIfAbsent(dateKey, k -> new DailyMetrics());
        
        metrics.featureUsage.merge(featureName, 1L, Long::sum);
        
        log.info("Feature used - Date: {}, UserId: {}, Feature: {}, Count: {}",
                dateKey, anonymizeUserId(userId), featureName, metrics.featureUsage.get(featureName));
    }
    
    @Override
    public Map<String, Object> getUsageMetrics(LocalDate date) {
        String dateKey = date.toString();
        DailyMetrics metrics = dailyMetricsCache.getOrDefault(dateKey, new DailyMetrics());
        
        Map<String, Object> result = new HashMap<>();
        result.put("date", dateKey);
        result.put("sessionCount", metrics.sessionCount.get());
        result.put("messageCount", metrics.messageCount.get());
        result.put("userMessageCount", metrics.userMessageCount.get());
        result.put("assistantMessageCount", metrics.assistantMessageCount.get());
        result.put("activeUsers", metrics.activeUsers.size());
        result.put("featureUsage", new HashMap<>(metrics.featureUsage));
        
        return result;
    }
    
    // ==================== High-Risk Alert Logging (Requirement 15.2) ====================
    
    @Override
    public void logHighRiskAlert(Long userId, int riskLevel, String primaryEmotion, String sessionId) {
        String dateKey = LocalDate.now().toString();
        DailyMetrics metrics = dailyMetricsCache.computeIfAbsent(dateKey, k -> new DailyMetrics());
        
        // Increment alert counters
        if (riskLevel == 2) {
            metrics.moderateRiskAlerts.incrementAndGet();
        } else if (riskLevel == 3) {
            metrics.highRiskAlerts.incrementAndGet();
        }
        
        // Track by emotion type
        String emotionKey = primaryEmotion + "_risk" + riskLevel;
        metrics.alertsByEmotion.merge(emotionKey, 1L, Long::sum);
        
        // Anonymized logging for administrative review
        log.warn("HIGH_RISK_ALERT - Date: {}, UserId: {}, RiskLevel: {}, Emotion: {}, SessionId: {}, Timestamp: {}",
                dateKey, anonymizeUserId(userId), riskLevel, primaryEmotion, sessionId, 
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        // Additional alert for risk level 3 (critical)
        if (riskLevel == 3) {
            log.error("CRITICAL_RISK_ALERT - UserId: {}, Emotion: {}, IMMEDIATE_ATTENTION_REQUIRED",
                    anonymizeUserId(userId), primaryEmotion);
        }
    }
    
    @Override
    public Map<String, Object> getHighRiskAlertStats(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        long totalModerateAlerts = 0;
        long totalHighAlerts = 0;
        Map<String, Long> emotionBreakdown = new HashMap<>();
        
        // Aggregate metrics across date range
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String dateKey = current.toString();
            DailyMetrics metrics = dailyMetricsCache.get(dateKey);
            
            if (metrics != null) {
                totalModerateAlerts += metrics.moderateRiskAlerts.get();
                totalHighAlerts += metrics.highRiskAlerts.get();
                
                // Merge emotion breakdowns
                metrics.alertsByEmotion.forEach((emotion, count) -> 
                    emotionBreakdown.merge(emotion, count, Long::sum));
            }
            
            current = current.plusDays(1);
        }
        
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("moderateRiskAlerts", totalModerateAlerts);
        result.put("highRiskAlerts", totalHighAlerts);
        result.put("totalAlerts", totalModerateAlerts + totalHighAlerts);
        result.put("alertsByEmotion", emotionBreakdown);
        
        return result;
    }
    
    // ==================== API Metrics (Requirement 15.3) ====================
    
    @Override
    public void logApiCall(String endpoint, long responseTimeMs, boolean success, 
                          Integer tokenUsage, String errorMessage) {
        String dateKey = LocalDate.now().toString();
        ApiMetrics metrics = apiMetricsCache.computeIfAbsent(dateKey, k -> new ApiMetrics());
        
        metrics.totalCalls.incrementAndGet();
        metrics.totalResponseTime.addAndGet(responseTimeMs);
        
        if (success) {
            metrics.successfulCalls.incrementAndGet();
        } else {
            metrics.failedCalls.incrementAndGet();
        }
        
        if (tokenUsage != null) {
            metrics.totalTokens.addAndGet(tokenUsage);
        }
        
        // Log API call metrics
        if (success) {
            log.info("API_CALL - Endpoint: {}, ResponseTime: {}ms, Tokens: {}, Status: SUCCESS",
                    endpoint, responseTimeMs, tokenUsage);
        } else {
            log.error("API_CALL - Endpoint: {}, ResponseTime: {}ms, Status: FAILED, Error: {}",
                    endpoint, responseTimeMs, errorMessage);
        }
        
        // Alert on slow responses (> 5 seconds)
        if (responseTimeMs > 5000) {
            log.warn("SLOW_API_RESPONSE - Endpoint: {}, ResponseTime: {}ms, Threshold: 5000ms",
                    endpoint, responseTimeMs);
        }
    }
    
    @Override
    public Map<String, Object> getApiMetrics(LocalDate startDate, LocalDate endDate) {
        long totalCalls = 0;
        long successfulCalls = 0;
        long failedCalls = 0;
        long totalResponseTime = 0;
        long totalTokens = 0;
        
        // Aggregate metrics across date range
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String dateKey = current.toString();
            ApiMetrics metrics = apiMetricsCache.get(dateKey);
            
            if (metrics != null) {
                totalCalls += metrics.totalCalls.get();
                successfulCalls += metrics.successfulCalls.get();
                failedCalls += metrics.failedCalls.get();
                totalResponseTime += metrics.totalResponseTime.get();
                totalTokens += metrics.totalTokens.get();
            }
            
            current = current.plusDays(1);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("totalCalls", totalCalls);
        result.put("successfulCalls", successfulCalls);
        result.put("failedCalls", failedCalls);
        result.put("errorRate", totalCalls > 0 ? (double) failedCalls / totalCalls : 0.0);
        result.put("averageResponseTimeMs", totalCalls > 0 ? totalResponseTime / totalCalls : 0);
        result.put("totalTokenUsage", totalTokens);
        
        return result;
    }
    
    // ==================== Async Task Monitoring (Requirement 15.4) ====================
    
    @Override
    public void logAsyncTask(String entryId, int queueDepth, long processingTimeMs, 
                            boolean success, int retryCount) {
        String dateKey = LocalDate.now().toString();
        AsyncTaskMetrics metrics = asyncTaskMetricsCache.computeIfAbsent(dateKey, k -> new AsyncTaskMetrics());
        
        metrics.totalTasks.incrementAndGet();
        metrics.totalProcessingTime.addAndGet(processingTimeMs);
        metrics.totalQueueDepth.addAndGet(queueDepth);
        
        if (success) {
            metrics.successfulTasks.incrementAndGet();
        } else {
            metrics.failedTasks.incrementAndGet();
        }
        
        if (retryCount > 0) {
            metrics.retriedTasks.incrementAndGet();
        }
        
        // Log task metrics
        if (success) {
            log.info("ASYNC_TASK - EntryId: {}, QueueDepth: {}, ProcessingTime: {}ms, Retries: {}, Status: SUCCESS",
                    entryId, queueDepth, processingTimeMs, retryCount);
        } else {
            log.error("ASYNC_TASK - EntryId: {}, QueueDepth: {}, ProcessingTime: {}ms, Retries: {}, Status: FAILED",
                    entryId, queueDepth, processingTimeMs, retryCount);
        }
        
        // Alert on high queue depth (> 50 tasks)
        if (queueDepth > 50) {
            log.warn("HIGH_QUEUE_DEPTH - Current: {}, Threshold: 50, Action: Consider scaling processing capacity",
                    queueDepth);
        }
        
        // Alert on slow processing (> 30 seconds)
        if (processingTimeMs > 30000) {
            log.warn("SLOW_TASK_PROCESSING - EntryId: {}, ProcessingTime: {}ms, Threshold: 30000ms",
                    entryId, processingTimeMs);
        }
    }
    
    @Override
    public Map<String, Object> getAsyncTaskMetrics(LocalDate startDate, LocalDate endDate) {
        long totalTasks = 0;
        long successfulTasks = 0;
        long failedTasks = 0;
        long retriedTasks = 0;
        long totalProcessingTime = 0;
        long totalQueueDepth = 0;
        int daysWithData = 0;
        
        // Aggregate metrics across date range
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            String dateKey = current.toString();
            AsyncTaskMetrics metrics = asyncTaskMetricsCache.get(dateKey);
            
            if (metrics != null && metrics.totalTasks.get() > 0) {
                totalTasks += metrics.totalTasks.get();
                successfulTasks += metrics.successfulTasks.get();
                failedTasks += metrics.failedTasks.get();
                retriedTasks += metrics.retriedTasks.get();
                totalProcessingTime += metrics.totalProcessingTime.get();
                totalQueueDepth += metrics.totalQueueDepth.get();
                daysWithData++;
            }
            
            current = current.plusDays(1);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        result.put("totalTasks", totalTasks);
        result.put("successfulTasks", successfulTasks);
        result.put("failedTasks", failedTasks);
        result.put("retriedTasks", retriedTasks);
        result.put("failureRate", totalTasks > 0 ? (double) failedTasks / totalTasks : 0.0);
        result.put("retryRate", totalTasks > 0 ? (double) retriedTasks / totalTasks : 0.0);
        result.put("averageProcessingTimeMs", totalTasks > 0 ? totalProcessingTime / totalTasks : 0);
        result.put("averageQueueDepth", daysWithData > 0 ? totalQueueDepth / daysWithData : 0);
        
        return result;
    }
    
    @Override
    public int getCurrentQueueDepth() {
        // Delegate to DiaryAnalysisQueue for real-time queue depth
        return diaryAnalysisQueue.getQueueDepth();
    }
    
    // ==================== Daily/Weekly Reports ====================
    
    @Override
    public Map<String, Object> generateDailySummary(LocalDate date) {
        Map<String, Object> summary = new HashMap<>();
        
        // Usage metrics
        Map<String, Object> usageMetrics = getUsageMetrics(date);
        summary.put("usage", usageMetrics);
        
        // High-risk alerts
        Map<String, Object> alertStats = getHighRiskAlertStats(date, date);
        summary.put("alerts", alertStats);
        
        // API metrics
        Map<String, Object> apiMetrics = getApiMetrics(date, date);
        summary.put("api", apiMetrics);
        
        // Async task metrics
        Map<String, Object> taskMetrics = getAsyncTaskMetrics(date, date);
        summary.put("asyncTasks", taskMetrics);
        
        // Summary statistics
        summary.put("date", date.toString());
        summary.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        log.info("DAILY_SUMMARY - Date: {}, ActiveUsers: {}, Sessions: {}, Messages: {}, HighRiskAlerts: {}",
                date, usageMetrics.get("activeUsers"), usageMetrics.get("sessionCount"),
                usageMetrics.get("messageCount"), alertStats.get("highRiskAlerts"));
        
        return summary;
    }
    
    @Override
    public Map<String, Object> generateWeeklyReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        
        // Aggregate weekly metrics
        long totalSessions = 0;
        long totalMessages = 0;
        long totalActiveUsers = 0;
        Map<String, Long> weeklyFeatureUsage = new HashMap<>();
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Map<String, Object> dailyMetrics = getUsageMetrics(current);
            totalSessions += (Integer) dailyMetrics.get("sessionCount");
            totalMessages += (Integer) dailyMetrics.get("messageCount");
            totalActiveUsers += (Integer) dailyMetrics.get("activeUsers");
            
            @SuppressWarnings("unchecked")
            Map<String, Long> featureUsage = (Map<String, Long>) dailyMetrics.get("featureUsage");
            featureUsage.forEach((feature, count) -> 
                weeklyFeatureUsage.merge(feature, count, Long::sum));
            
            current = current.plusDays(1);
        }
        
        // High-risk alerts
        Map<String, Object> alertStats = getHighRiskAlertStats(startDate, endDate);
        
        // API metrics
        Map<String, Object> apiMetrics = getApiMetrics(startDate, endDate);
        
        // Async task metrics
        Map<String, Object> taskMetrics = getAsyncTaskMetrics(startDate, endDate);
        
        // Build report
        report.put("startDate", startDate.toString());
        report.put("endDate", endDate.toString());
        report.put("totalSessions", totalSessions);
        report.put("totalMessages", totalMessages);
        report.put("totalActiveUsers", totalActiveUsers);
        report.put("featureAdoption", weeklyFeatureUsage);
        report.put("alerts", alertStats);
        report.put("api", apiMetrics);
        report.put("asyncTasks", taskMetrics);
        report.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        log.info("WEEKLY_REPORT - Period: {} to {}, TotalSessions: {}, TotalMessages: {}, ActiveUsers: {}",
                startDate, endDate, totalSessions, totalMessages, totalActiveUsers);
        
        return report;
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Anonymize user ID for privacy-preserving logging.
     * Uses simple hash to maintain consistency while protecting identity.
     */
    private String anonymizeUserId(Long userId) {
        if (userId == null) {
            return "UNKNOWN";
        }
        // Simple anonymization - in production, use proper hashing
        return "USER_" + Math.abs(userId.hashCode() % 10000);
    }
    
    // ==================== Performance Degradation Alerting (Requirement 15.7) ====================
    
    /**
     * Scheduled task to check performance metrics against thresholds.
     * Runs every 5 minutes by default (configurable via app.monitoring.check-interval).
     * 
     * Validates: Property 74 - Performance Degradation Alerting
     */
    @Scheduled(cron = "${app.monitoring.check-interval:0 */5 * * * ?}")
    @Override
    public void checkPerformanceThresholds() {
        log.debug("Starting performance threshold check...");
        
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        try {
            // Check API metrics (last 24 hours)
            checkApiPerformance(yesterday, today);
            
            // Check async task metrics (last 24 hours)
            checkAsyncTaskPerformance(yesterday, today);
            
            // Check current queue depth
            checkQueueDepth();
            
            log.debug("Performance threshold check completed successfully");
            
        } catch (Exception e) {
            log.error("Error during performance threshold check: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Check API performance metrics against thresholds.
     */
    private void checkApiPerformance(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> apiMetrics = getApiMetrics(startDate, endDate);
        
        // Check average response time
        long avgResponseTime = (Long) apiMetrics.getOrDefault("averageResponseTimeMs", 0L);
        MonitoringProperties.Threshold threshold = monitoringProperties.getThreshold();
        
        if (avgResponseTime > threshold.getApiResponseTimeCriticalMs()) {
            sendAdminAlert(
                "API_RESPONSE_TIME",
                "CRITICAL",
                String.format("API average response time (%dms) exceeds critical threshold (%dms). " +
                        "System performance is severely degraded. Immediate action required.",
                        avgResponseTime, threshold.getApiResponseTimeCriticalMs()),
                avgResponseTime,
                threshold.getApiResponseTimeCriticalMs()
            );
        } else if (avgResponseTime > threshold.getApiResponseTimeWarningMs()) {
            sendAdminAlert(
                "API_RESPONSE_TIME",
                "WARNING",
                String.format("API average response time (%dms) exceeds warning threshold (%dms). " +
                        "System performance may be degrading. Please investigate.",
                        avgResponseTime, threshold.getApiResponseTimeWarningMs()),
                avgResponseTime,
                threshold.getApiResponseTimeWarningMs()
            );
        }
        
        // Check error rate
        double errorRate = (Double) apiMetrics.getOrDefault("errorRate", 0.0);
        double errorRatePercent = errorRate * 100;
        
        if (errorRatePercent > threshold.getApiErrorRateCriticalPercent()) {
            sendAdminAlert(
                "API_ERROR_RATE",
                "CRITICAL",
                String.format("API error rate (%.2f%%) exceeds critical threshold (%.2f%%). " +
                        "System reliability is severely compromised. Immediate action required.",
                        errorRatePercent, threshold.getApiErrorRateCriticalPercent()),
                errorRatePercent,
                threshold.getApiErrorRateCriticalPercent()
            );
        } else if (errorRatePercent > threshold.getApiErrorRateWarningPercent()) {
            sendAdminAlert(
                "API_ERROR_RATE",
                "WARNING",
                String.format("API error rate (%.2f%%) exceeds warning threshold (%.2f%%). " +
                        "System reliability may be degrading. Please investigate.",
                        errorRatePercent, threshold.getApiErrorRateWarningPercent()),
                errorRatePercent,
                threshold.getApiErrorRateWarningPercent()
            );
        }
    }
    
    /**
     * Check async task performance metrics against thresholds.
     */
    private void checkAsyncTaskPerformance(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> taskMetrics = getAsyncTaskMetrics(startDate, endDate);
        
        // Check failure rate
        double failureRate = (Double) taskMetrics.getOrDefault("failureRate", 0.0);
        double failureRatePercent = failureRate * 100;
        MonitoringProperties.Threshold threshold = monitoringProperties.getThreshold();
        
        if (failureRatePercent > threshold.getTaskFailureRateCriticalPercent()) {
            sendAdminAlert(
                "TASK_FAILURE_RATE",
                "CRITICAL",
                String.format("Async task failure rate (%.2f%%) exceeds critical threshold (%.2f%%). " +
                        "Diary analysis system is severely impacted. Immediate action required.",
                        failureRatePercent, threshold.getTaskFailureRateCriticalPercent()),
                failureRatePercent,
                threshold.getTaskFailureRateCriticalPercent()
            );
        } else if (failureRatePercent > threshold.getTaskFailureRateWarningPercent()) {
            sendAdminAlert(
                "TASK_FAILURE_RATE",
                "WARNING",
                String.format("Async task failure rate (%.2f%%) exceeds warning threshold (%.2f%%). " +
                        "Diary analysis system may be degrading. Please investigate.",
                        failureRatePercent, threshold.getTaskFailureRateWarningPercent()),
                failureRatePercent,
                threshold.getTaskFailureRateWarningPercent()
            );
        }
    }
    
    /**
     * Check current queue depth against thresholds.
     */
    private void checkQueueDepth() {
        int currentQueueDepth = getCurrentQueueDepth();
        MonitoringProperties.Threshold threshold = monitoringProperties.getThreshold();
        
        if (currentQueueDepth > threshold.getQueueDepthCritical()) {
            sendAdminAlert(
                "QUEUE_DEPTH",
                "CRITICAL",
                String.format("Async task queue depth (%d) exceeds critical threshold (%d). " +
                        "System is severely backlogged. Immediate scaling required.",
                        currentQueueDepth, threshold.getQueueDepthCritical()),
                currentQueueDepth,
                threshold.getQueueDepthCritical()
            );
        } else if (currentQueueDepth > threshold.getQueueDepthWarning()) {
            sendAdminAlert(
                "QUEUE_DEPTH",
                "WARNING",
                String.format("Async task queue depth (%d) exceeds warning threshold (%d). " +
                        "System may be experiencing high load. Consider scaling.",
                        currentQueueDepth, threshold.getQueueDepthWarning()),
                currentQueueDepth,
                threshold.getQueueDepthWarning()
            );
        }
    }
    
    @Override
    public void sendAdminAlert(String alertType, String severity, String message, 
                              double currentValue, double threshold) {
        // Check cooldown period to avoid alert spam
        String cooldownKey = alertType + "_" + severity;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastAlert = alertCooldownMap.get(cooldownKey);
        
        if (lastAlert != null) {
            long minutesSinceLastAlert = java.time.Duration.between(lastAlert, now).toMinutes();
            if (minutesSinceLastAlert < monitoringProperties.getAlert().getCooldownMinutes()) {
                log.debug("Alert {} skipped due to cooldown period ({}min remaining)",
                        cooldownKey, 
                        monitoringProperties.getAlert().getCooldownMinutes() - minutesSinceLastAlert);
                return;
            }
        }
        
        // Update cooldown timestamp
        alertCooldownMap.put(cooldownKey, now);
        
        // Log alert with appropriate severity
        String logMessage = String.format(
                "PERFORMANCE_ALERT [%s] - Type: %s, Severity: %s, CurrentValue: %.2f, Threshold: %.2f, Message: %s",
                now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                alertType,
                severity,
                currentValue,
                threshold,
                message
        );
        
        if ("CRITICAL".equals(severity)) {
            log.error(logMessage);
        } else {
            log.warn(logMessage);
        }
        
        // Send notifications to administrators
        sendAlertNotifications(alertType, severity, message, currentValue, threshold);
    }
    
    /**
     * Send alert notifications to configured administrator channels.
     * 
     * In production, this should integrate with:
     * - Email service (SMTP, SendGrid, etc.)
     * - SMS service (Twilio, AWS SNS, etc.)
     * - Monitoring platforms (PagerDuty, Opsgenie, etc.)
     * - Messaging platforms (Slack, Microsoft Teams, etc.)
     * 
     * For now, logs the notification details for external monitoring systems to pick up.
     */
    private void sendAlertNotifications(String alertType, String severity, String message,
                                       double currentValue, double threshold) {
        MonitoringProperties.Alert alertConfig = monitoringProperties.getAlert();
        
        // Log notification details for external systems
        log.info("ALERT_NOTIFICATION - Type: {}, Severity: {}, Recipients: {}, Message: {}",
                alertType, severity, alertConfig.getAdminEmailList(), message);
        
        // TODO: Implement actual notification sending
        // Example integrations:
        // 1. Email: emailService.sendAlert(alertConfig.getAdminEmailList(), subject, message);
        // 2. SMS: smsService.sendAlert(alertConfig.getAdminPhoneList(), message);
        // 3. Slack: slackService.sendAlert(webhookUrl, message);
        // 4. PagerDuty: pagerDutyService.triggerIncident(alertType, severity, message);
        
        // For demonstration, log the notification attempt
        if (!alertConfig.getAdminEmailList().isEmpty()) {
            log.info("Email alert would be sent to: {}", alertConfig.getAdminEmailList());
        }
        
        if (!alertConfig.getAdminPhoneList().isEmpty()) {
            log.info("SMS alert would be sent to: {}", alertConfig.getAdminPhoneList());
        }
    }
    
    // ==================== Inner Classes for Metrics Storage ====================
    
    /**
     * Daily usage metrics container.
     */
    private static class DailyMetrics {
        final AtomicInteger sessionCount = new AtomicInteger(0);
        final AtomicInteger messageCount = new AtomicInteger(0);
        final AtomicInteger userMessageCount = new AtomicInteger(0);
        final AtomicInteger assistantMessageCount = new AtomicInteger(0);
        final ConcurrentHashMap.KeySetView<Long, Boolean> activeUsers = ConcurrentHashMap.newKeySet();
        final Map<String, Long> featureUsage = new ConcurrentHashMap<>();
        final AtomicInteger moderateRiskAlerts = new AtomicInteger(0);
        final AtomicInteger highRiskAlerts = new AtomicInteger(0);
        final Map<String, Long> alertsByEmotion = new ConcurrentHashMap<>();
    }
    
    /**
     * API metrics container.
     */
    private static class ApiMetrics {
        final AtomicLong totalCalls = new AtomicLong(0);
        final AtomicLong successfulCalls = new AtomicLong(0);
        final AtomicLong failedCalls = new AtomicLong(0);
        final AtomicLong totalResponseTime = new AtomicLong(0);
        final AtomicLong totalTokens = new AtomicLong(0);
    }
    
    /**
     * Async task metrics container.
     */
    private static class AsyncTaskMetrics {
        final AtomicLong totalTasks = new AtomicLong(0);
        final AtomicLong successfulTasks = new AtomicLong(0);
        final AtomicLong failedTasks = new AtomicLong(0);
        final AtomicLong retriedTasks = new AtomicLong(0);
        final AtomicLong totalProcessingTime = new AtomicLong(0);
        final AtomicLong totalQueueDepth = new AtomicLong(0);
    }
}
