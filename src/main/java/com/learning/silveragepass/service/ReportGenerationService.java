package com.learning.silveragepass.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * Service for automated report generation and delivery.
 * Schedules daily and weekly reports for system monitoring and analytics.
 * 
 * Requirements:
 * - 15.6: Daily summary report generation
 * - 15.8: Weekly analytics report generation
 */
public interface ReportGenerationService {
    
    /**
     * Generate and log daily summary report for the previous day.
     * Scheduled to run automatically every day at 1:00 AM.
     * 
     * @return The generated daily summary report
     */
    Map<String, Object> generateAndLogDailySummary();
    
    /**
     * Generate and log weekly analytics report for the previous week.
     * Scheduled to run automatically every Monday at 2:00 AM.
     * 
     * @return The generated weekly analytics report
     */
    Map<String, Object> generateAndLogWeeklyReport();
    
    /**
     * Generate daily summary report for a specific date.
     * Used for manual report generation or historical analysis.
     * 
     * @param date The date for the report
     * @return The generated daily summary report
     */
    Map<String, Object> generateDailySummaryForDate(LocalDate date);
    
    /**
     * Generate weekly analytics report for a specific date range.
     * Used for manual report generation or historical analysis.
     * 
     * @param startDate Start of the week
     * @param endDate End of the week
     * @return The generated weekly analytics report
     */
    Map<String, Object> generateWeeklyReportForDateRange(LocalDate startDate, LocalDate endDate);
}
