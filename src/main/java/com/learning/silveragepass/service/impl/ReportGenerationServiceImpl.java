package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.service.MonitoringService;
import com.learning.silveragepass.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Implementation of ReportGenerationService for automated report generation.
 * 
 * This service schedules and generates:
 * - Daily summary reports (every day at 1:00 AM)
 * - Weekly analytics reports (every Monday at 2:00 AM)
 * 
 * Reports are logged in structured format for external monitoring systems
 * and can be integrated with email/notification delivery systems.
 * 
 * Requirements:
 * - 15.6: Daily summary report generation
 * - 15.8: Weekly analytics report generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationServiceImpl implements ReportGenerationService {
    
    private final MonitoringService monitoringService;
    
    /**
     * Scheduled task to generate daily summary report.
     * Runs every day at 1:00 AM server time.
     * Generates report for the previous day.
     * 
     * Cron expression: "0 0 1 * * ?" = At 1:00 AM every day
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Override
    public Map<String, Object> generateAndLogDailySummary() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        log.info("=== SCHEDULED DAILY SUMMARY REPORT GENERATION ===");
        log.info("Generating daily summary report for date: {}", yesterday);
        
        try {
            Map<String, Object> summary = monitoringService.generateDailySummary(yesterday);
            
            // Log structured report
            logDailySummaryReport(summary);
            
            log.info("Daily summary report generated successfully for date: {}", yesterday);
            return summary;
            
        } catch (Exception e) {
            log.error("Failed to generate daily summary report for date: {}", yesterday, e);
            throw new RuntimeException("Daily report generation failed", e);
        }
    }
    
    /**
     * Scheduled task to generate weekly analytics report.
     * Runs every Monday at 2:00 AM server time.
     * Generates report for the previous week (Monday to Sunday).
     * 
     * Cron expression: "0 0 2 ? * MON" = At 2:00 AM every Monday
     */
    @Scheduled(cron = "0 0 2 ? * MON")
    @Override
    public Map<String, Object> generateAndLogWeeklyReport() {
        // Calculate previous week's date range (Monday to Sunday)
        LocalDate today = LocalDate.now();
        LocalDate lastMonday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
        LocalDate lastSunday = lastMonday.plusDays(6);
        
        log.info("=== SCHEDULED WEEKLY ANALYTICS REPORT GENERATION ===");
        log.info("Generating weekly analytics report for period: {} to {}", lastMonday, lastSunday);
        
        try {
            Map<String, Object> report = monitoringService.generateWeeklyReport(lastMonday, lastSunday);
            
            // Log structured report
            logWeeklyAnalyticsReport(report);
            
            log.info("Weekly analytics report generated successfully for period: {} to {}", 
                    lastMonday, lastSunday);
            return report;
            
        } catch (Exception e) {
            log.error("Failed to generate weekly analytics report for period: {} to {}", 
                    lastMonday, lastSunday, e);
            throw new RuntimeException("Weekly report generation failed", e);
        }
    }
    
    @Override
    public Map<String, Object> generateDailySummaryForDate(LocalDate date) {
        log.info("Generating manual daily summary report for date: {}", date);
        
        try {
            Map<String, Object> summary = monitoringService.generateDailySummary(date);
            logDailySummaryReport(summary);
            return summary;
            
        } catch (Exception e) {
            log.error("Failed to generate manual daily summary report for date: {}", date, e);
            throw new RuntimeException("Manual daily report generation failed", e);
        }
    }
    
    @Override
    public Map<String, Object> generateWeeklyReportForDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Generating manual weekly analytics report for period: {} to {}", startDate, endDate);
        
        try {
            Map<String, Object> report = monitoringService.generateWeeklyReport(startDate, endDate);
            logWeeklyAnalyticsReport(report);
            return report;
            
        } catch (Exception e) {
            log.error("Failed to generate manual weekly analytics report for period: {} to {}", 
                    startDate, endDate, e);
            throw new RuntimeException("Manual weekly report generation failed", e);
        }
    }
    
    /**
     * Log daily summary report in structured format.
     * This format can be parsed by external monitoring systems (Splunk, ELK, etc.)
     */
    private void logDailySummaryReport(Map<String, Object> summary) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║           DAILY SUMMARY REPORT - {}                    ║", summary.get("date"));
        log.info("╠════════════════════════════════════════════════════════════════╣");
        
        // Usage metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> usage = (Map<String, Object>) summary.get("usage");
        log.info("║ USAGE METRICS:                                                 ║");
        log.info("║   Active Users:        {:>10}                              ║", usage.get("activeUsers"));
        log.info("║   Total Sessions:      {:>10}                              ║", usage.get("sessionCount"));
        log.info("║   Total Messages:      {:>10}                              ║", usage.get("messageCount"));
        log.info("║   User Messages:       {:>10}                              ║", usage.get("userMessageCount"));
        log.info("║   Assistant Messages:  {:>10}                              ║", usage.get("assistantMessageCount"));
        
        // Alert metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> alerts = (Map<String, Object>) summary.get("alerts");
        log.info("║                                                                ║");
        log.info("║ ALERT METRICS:                                                 ║");
        log.info("║   Moderate Risk (Level 2): {:>10}                         ║", alerts.get("moderateRiskAlerts"));
        log.info("║   High Risk (Level 3):     {:>10}                         ║", alerts.get("highRiskAlerts"));
        log.info("║   Total Alerts:            {:>10}                         ║", alerts.get("totalAlerts"));
        
        // API metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> api = (Map<String, Object>) summary.get("api");
        log.info("║                                                                ║");
        log.info("║ API METRICS:                                                   ║");
        log.info("║   Total API Calls:     {:>10}                              ║", api.get("totalCalls"));
        log.info("║   Successful Calls:    {:>10}                              ║", api.get("successfulCalls"));
        log.info("║   Failed Calls:        {:>10}                              ║", api.get("failedCalls"));
        log.info("║   Error Rate:          {:>9.2f}%                             ║", 
                ((Number) api.get("errorRate")).doubleValue() * 100);
        log.info("║   Avg Response Time:   {:>10}ms                            ║", api.get("averageResponseTimeMs"));
        log.info("║   Total Token Usage:   {:>10}                              ║", api.get("totalTokenUsage"));
        
        // Async task metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> tasks = (Map<String, Object>) summary.get("asyncTasks");
        log.info("║                                                                ║");
        log.info("║ ASYNC TASK METRICS:                                            ║");
        log.info("║   Total Tasks:         {:>10}                              ║", tasks.get("totalTasks"));
        log.info("║   Successful Tasks:    {:>10}                              ║", tasks.get("successfulTasks"));
        log.info("║   Failed Tasks:        {:>10}                              ║", tasks.get("failedTasks"));
        log.info("║   Failure Rate:        {:>9.2f}%                             ║", 
                ((Number) tasks.get("failureRate")).doubleValue() * 100);
        log.info("║   Avg Processing Time: {:>10}ms                            ║", tasks.get("averageProcessingTimeMs"));
        log.info("║   Avg Queue Depth:     {:>10}                              ║", tasks.get("averageQueueDepth"));
        
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Generated at: {}                                    ║", summary.get("generatedAt"));
        log.info("╚════════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Log weekly analytics report in structured format.
     * This format can be parsed by external monitoring systems (Splunk, ELK, etc.)
     */
    private void logWeeklyAnalyticsReport(Map<String, Object> report) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║        WEEKLY ANALYTICS REPORT - {} to {}         ║", 
                report.get("startDate"), report.get("endDate"));
        log.info("╠════════════════════════════════════════════════════════════════╣");
        
        // Overall metrics
        log.info("║ OVERALL METRICS:                                               ║");
        log.info("║   Total Active Users:  {:>10}                              ║", report.get("totalActiveUsers"));
        log.info("║   Total Sessions:      {:>10}                              ║", report.get("totalSessions"));
        log.info("║   Total Messages:      {:>10}                              ║", report.get("totalMessages"));
        
        // Feature adoption
        @SuppressWarnings("unchecked")
        Map<String, Long> featureAdoption = (Map<String, Long>) report.get("featureAdoption");
        log.info("║                                                                ║");
        log.info("║ FEATURE ADOPTION:                                              ║");
        if (featureAdoption != null && !featureAdoption.isEmpty()) {
            featureAdoption.forEach((feature, count) -> 
                log.info("║   {:30} {:>10}                     ║", 
                        truncate(feature, 30), count));
        } else {
            log.info("║   No feature usage data available                              ║");
        }
        
        // Alert summary
        @SuppressWarnings("unchecked")
        Map<String, Object> alerts = (Map<String, Object>) report.get("alerts");
        log.info("║                                                                ║");
        log.info("║ ALERT SUMMARY:                                                 ║");
        log.info("║   Moderate Risk Alerts: {:>10}                             ║", alerts.get("moderateRiskAlerts"));
        log.info("║   High Risk Alerts:     {:>10}                             ║", alerts.get("highRiskAlerts"));
        log.info("║   Total Alerts:         {:>10}                             ║", alerts.get("totalAlerts"));
        
        // API performance
        @SuppressWarnings("unchecked")
        Map<String, Object> api = (Map<String, Object>) report.get("api");
        log.info("║                                                                ║");
        log.info("║ API PERFORMANCE:                                               ║");
        log.info("║   Total API Calls:     {:>10}                              ║", api.get("totalCalls"));
        log.info("║   Error Rate:          {:>9.2f}%                             ║", 
                ((Number) api.get("errorRate")).doubleValue() * 100);
        log.info("║   Avg Response Time:   {:>10}ms                            ║", api.get("averageResponseTimeMs"));
        log.info("║   Total Token Usage:   {:>10}                              ║", api.get("totalTokenUsage"));
        
        // Task processing
        @SuppressWarnings("unchecked")
        Map<String, Object> tasks = (Map<String, Object>) report.get("asyncTasks");
        log.info("║                                                                ║");
        log.info("║ TASK PROCESSING:                                               ║");
        log.info("║   Total Tasks:         {:>10}                              ║", tasks.get("totalTasks"));
        log.info("║   Failure Rate:        {:>9.2f}%                             ║", 
                ((Number) tasks.get("failureRate")).doubleValue() * 100);
        log.info("║   Retry Rate:          {:>9.2f}%                             ║", 
                ((Number) tasks.get("retryRate")).doubleValue() * 100);
        log.info("║   Avg Processing Time: {:>10}ms                            ║", tasks.get("averageProcessingTimeMs"));
        
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Generated at: {}                                    ║", report.get("generatedAt"));
        log.info("╚════════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Truncate string to specified length for formatting.
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }
}
