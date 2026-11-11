package com.learning.silveragepass.controller;

import com.learning.silveragepass.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST API controller for report generation and retrieval.
 * Provides endpoints for administrators to manually generate and view reports.
 * 
 * Requirements:
 * - 15.6: Daily summary report generation
 * - 15.8: Weekly analytics report generation
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    
    private final ReportGenerationService reportGenerationService;
    
    /**
     * Generate daily summary report for a specific date.
     * 
     * GET /api/reports/daily?date=2024-01-15
     * 
     * @param date The date for the report (defaults to yesterday if not provided)
     * @return Daily summary report
     */
    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> getDailySummary(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate date) {
        
        // Default to yesterday if no date provided
        LocalDate reportDate = date != null ? date : LocalDate.now().minusDays(1);
        
        log.info("API request for daily summary report: date={}", reportDate);
        
        Map<String, Object> summary = reportGenerationService.generateDailySummaryForDate(reportDate);
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Generate weekly analytics report for a specific date range.
     * 
     * GET /api/reports/weekly?startDate=2024-01-08&endDate=2024-01-14
     * 
     * @param startDate Start of the week (defaults to last Monday if not provided)
     * @param endDate End of the week (defaults to last Sunday if not provided)
     * @return Weekly analytics report
     */
    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklyReport(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate endDate) {
        
        // Default to last week if no dates provided
        LocalDate reportStartDate = startDate;
        LocalDate reportEndDate = endDate;
        
        if (reportStartDate == null || reportEndDate == null) {
            LocalDate today = LocalDate.now();
            reportStartDate = today.minusWeeks(1).with(java.time.DayOfWeek.MONDAY);
            reportEndDate = reportStartDate.plusDays(6);
        }
        
        log.info("API request for weekly analytics report: startDate={}, endDate={}", 
                reportStartDate, reportEndDate);
        
        Map<String, Object> report = reportGenerationService.generateWeeklyReportForDateRange(
                reportStartDate, reportEndDate);
        
        return ResponseEntity.ok(report);
    }
    
    /**
     * Trigger manual generation of daily summary report.
     * This endpoint can be used to manually trigger the scheduled report generation.
     * 
     * POST /api/reports/daily/generate
     * 
     * @return Daily summary report for yesterday
     */
    @PostMapping("/daily/generate")
    public ResponseEntity<Map<String, Object>> triggerDailySummary() {
        log.info("Manual trigger for daily summary report generation");
        
        Map<String, Object> summary = reportGenerationService.generateAndLogDailySummary();
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Trigger manual generation of weekly analytics report.
     * This endpoint can be used to manually trigger the scheduled report generation.
     * 
     * POST /api/reports/weekly/generate
     * 
     * @return Weekly analytics report for last week
     */
    @PostMapping("/weekly/generate")
    public ResponseEntity<Map<String, Object>> triggerWeeklyReport() {
        log.info("Manual trigger for weekly analytics report generation");
        
        Map<String, Object> report = reportGenerationService.generateAndLogWeeklyReport();
        
        return ResponseEntity.ok(report);
    }
}
