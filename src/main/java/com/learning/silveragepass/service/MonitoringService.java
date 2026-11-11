package com.learning.silveragepass.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Service for monitoring and analytics of the AI Health Assistant system.
 * Tracks usage metrics, performance indicators, and system health without storing sensitive content.
 * 
 * Requirements:
 * - 15.1: Usage metrics logging (session count, message count) without storing message content
 * - 15.2: High-risk alert logging (anonymized)
 * - 15.3: API metrics tracking (response time, error rate, token usage)
 * - 15.4: Async task monitoring (queue depth, processing time, failure rate)
 */
public interface MonitoringService {
    
    // ==================== Usage Metrics (Requirement 15.1) ====================
    
    /**
     * Log a new conversation session creation.
     * Does not store message content, only metadata.
     * 
     * @param userId The user ID (anonymized for privacy)
     * @param sessionId The session UUID
     */
    void logSessionCreated(Long userId, String sessionId);
    
    /**
     * Log a message sent in a conversation.
     * Does not store message content, only counts and metadata.
     * 
     * @param userId The user ID
     * @param sessionId The session UUID
     * @param role The message role (user/assistant)
     */
    void logMessageSent(Long userId, String sessionId, String role);
    
    /**
     * Log feature usage (diary creation, statistics view, etc.).
     * 
     * @param userId The user ID
     * @param featureName The feature being used
     */
    void logFeatureUsage(Long userId, String featureName);
    
    /**
     * Get usage metrics for a specific date.
     * 
     * @param date The date to query
     * @return Map containing session count, message count, active users, etc.
     */
    Map<String, Object> getUsageMetrics(LocalDate date);
    
    // ==================== High-Risk Alert Logging (Requirement 15.2) ====================
    
    /**
     * Log a high-risk emotion analysis alert (anonymized).
     * Does not store message content, only risk level and metadata.
     * 
     * @param userId The user ID (anonymized)
     * @param riskLevel The risk level (2 or 3)
     * @param primaryEmotion The detected emotion type
     * @param sessionId The session UUID
     */
    void logHighRiskAlert(Long userId, int riskLevel, String primaryEmotion, String sessionId);
    
    /**
     * Get high-risk alert statistics for administrative review.
     * 
     * @param startDate Start date for query
     * @param endDate End date for query
     * @return Map containing alert counts by risk level and emotion type
     */
    Map<String, Object> getHighRiskAlertStats(LocalDate startDate, LocalDate endDate);
    
    // ==================== API Metrics (Requirement 15.3) ====================
    
    /**
     * Log an API call to external AI service (SiliconFlow).
     * 
     * @param endpoint The API endpoint called
     * @param responseTimeMs Response time in milliseconds
     * @param success Whether the call succeeded
     * @param tokenUsage Number of tokens used (if available)
     * @param errorMessage Error message if failed (null if success)
     */
    void logApiCall(String endpoint, long responseTimeMs, boolean success, Integer tokenUsage, String errorMessage);
    
    /**
     * Get API metrics for a specific date range.
     * 
     * @param startDate Start date for query
     * @param endDate End date for query
     * @return Map containing average response time, error rate, total token usage
     */
    Map<String, Object> getApiMetrics(LocalDate startDate, LocalDate endDate);
    
    // ==================== Async Task Monitoring (Requirement 15.4) ====================
    
    /**
     * Log async diary analysis task metrics.
     * 
     * @param entryId The diary entry ID
     * @param queueDepth Current queue depth when task was queued
     * @param processingTimeMs Time taken to process the task
     * @param success Whether the task succeeded
     * @param retryCount Number of retries attempted
     */
    void logAsyncTask(String entryId, int queueDepth, long processingTimeMs, boolean success, int retryCount);
    
    /**
     * Get async task metrics for monitoring.
     * 
     * @param startDate Start date for query
     * @param endDate End date for query
     * @return Map containing average queue depth, processing time, failure rate
     */
    Map<String, Object> getAsyncTaskMetrics(LocalDate startDate, LocalDate endDate);
    
    /**
     * Get current queue depth for real-time monitoring.
     * 
     * @return Current number of tasks in the queue
     */
    int getCurrentQueueDepth();
    
    // ==================== Daily/Weekly Reports ====================
    
    /**
     * Generate daily summary report.
     * 
     * @param date The date for the report
     * @return Map containing daily metrics summary
     */
    Map<String, Object> generateDailySummary(LocalDate date);
    
    /**
     * Generate weekly analytics report.
     * 
     * @param startDate Start of the week
     * @param endDate End of the week
     * @return Map containing weekly analytics
     */
    Map<String, Object> generateWeeklyReport(LocalDate startDate, LocalDate endDate);
    
    // ==================== Performance Degradation Alerting (Requirement 15.7) ====================
    
    /**
     * Check performance metrics against configured thresholds and trigger alerts if needed.
     * This method should be called periodically (e.g., every 5 minutes) to monitor system health.
     * 
     * Checks:
     * - API response times
     * - API error rates
     * - Async task queue depth
     * - Async task failure rates
     * 
     * Triggers alerts to administrators when thresholds are exceeded.
     */
    void checkPerformanceThresholds();
    
    /**
     * Send alert notification to administrators.
     * 
     * @param alertType Type of alert (e.g., "API_RESPONSE_TIME", "ERROR_RATE")
     * @param severity Severity level ("WARNING" or "CRITICAL")
     * @param message Alert message with details
     * @param currentValue Current metric value
     * @param threshold Threshold value that was exceeded
     */
    void sendAdminAlert(String alertType, String severity, String message, 
                       double currentValue, double threshold);
}
