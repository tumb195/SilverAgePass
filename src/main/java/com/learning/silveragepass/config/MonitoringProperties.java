package com.learning.silveragepass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for performance monitoring and alerting.
 * 
 * Loads threshold values and alert notification settings from application.properties.
 * Supports configurable thresholds for:
 * - API response times
 * - API error rates
 * - Async task queue depth
 * - Async task failure rates
 * 
 * Requirements:
 * - 15.7: Performance degradation alerting with configurable thresholds
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.monitoring")
public class MonitoringProperties {
    
    /**
     * Performance threshold configuration.
     */
    private Threshold threshold = new Threshold();
    
    /**
     * Alert notification configuration.
     */
    private Alert alert = new Alert();
    
    /**
     * Performance monitoring check interval (Cron expression).
     * Default: every 5 minutes
     */
    private String checkInterval = "0 */5 * * * ?";
    
    @Data
    public static class Threshold {
        /**
         * API response time warning threshold in milliseconds.
         * Default: 3000ms (3 seconds)
         */
        private long apiResponseTimeWarningMs = 3000;
        
        /**
         * API response time critical threshold in milliseconds.
         * Default: 5000ms (5 seconds)
         */
        private long apiResponseTimeCriticalMs = 5000;
        
        /**
         * API error rate warning threshold in percentage (0-100).
         * Default: 10%
         */
        private double apiErrorRateWarningPercent = 10.0;
        
        /**
         * API error rate critical threshold in percentage (0-100).
         * Default: 20%
         */
        private double apiErrorRateCriticalPercent = 20.0;
        
        /**
         * Queue depth warning threshold.
         * Default: 50 tasks
         */
        private int queueDepthWarning = 50;
        
        /**
         * Queue depth critical threshold.
         * Default: 100 tasks
         */
        private int queueDepthCritical = 100;
        
        /**
         * Task failure rate warning threshold in percentage (0-100).
         * Default: 5%
         */
        private double taskFailureRateWarningPercent = 5.0;
        
        /**
         * Task failure rate critical threshold in percentage (0-100).
         * Default: 15%
         */
        private double taskFailureRateCriticalPercent = 15.0;
    }
    
    @Data
    public static class Alert {
        /**
         * Administrator email addresses for alert notifications.
         * Multiple emails separated by comma.
         */
        private String adminEmails = "admin@silveragepass.com";
        
        /**
         * Administrator phone numbers for alert notifications.
         * Multiple phones separated by comma.
         */
        private String adminPhones = "";
        
        /**
         * Alert cooldown period in minutes.
         * Same type of alert won't be sent again within this period.
         * Default: 30 minutes
         */
        private int cooldownMinutes = 30;
        
        /**
         * Get list of admin email addresses.
         */
        public List<String> getAdminEmailList() {
            List<String> emails = new ArrayList<>();
            if (adminEmails != null && !adminEmails.trim().isEmpty()) {
                for (String email : adminEmails.split(",")) {
                    String trimmed = email.trim();
                    if (!trimmed.isEmpty()) {
                        emails.add(trimmed);
                    }
                }
            }
            return emails;
        }
        
        /**
         * Get list of admin phone numbers.
         */
        public List<String> getAdminPhoneList() {
            List<String> phones = new ArrayList<>();
            if (adminPhones != null && !adminPhones.trim().isEmpty()) {
                for (String phone : adminPhones.split(",")) {
                    String trimmed = phone.trim();
                    if (!trimmed.isEmpty()) {
                        phones.add(trimmed);
                    }
                }
            }
            return phones;
        }
    }
}
