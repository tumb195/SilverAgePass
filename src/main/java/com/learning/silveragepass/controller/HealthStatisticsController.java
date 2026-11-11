package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.health.EmotionDistribution;
import com.learning.silveragepass.dto.health.EmotionTrendData;
import com.learning.silveragepass.dto.health.HealthMetricsSummary;
import com.learning.silveragepass.security.CurrentUser;
import com.learning.silveragepass.security.UserPrincipal;
import com.learning.silveragepass.service.FamilyAccessService;
import com.learning.silveragepass.service.HealthStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for Health Statistics
 * 
 * This controller exposes the HealthStatisticsService via REST API, providing
 * aggregated health metrics and trend analysis for elderly users. All endpoints
 * require JWT authentication and support family member access with privacy filtering.
 * 
 * Key Features:
 * - Emotion trend analysis for 7/30/90 day periods
 * - Emotion distribution with type grouping
 * - Health summary with average calculations
 * - Personalized improvement suggestions
 * - Family member access with privacy filtering
 * - JWT authentication on all endpoints
 * - Appropriate HTTP status codes
 * - Graceful error handling
 * 
 * Endpoints:
 * - GET /api/health-assistant/statistics/trends - Get emotion trends
 * - GET /api/health-assistant/statistics/distribution - Get emotion distribution
 * - GET /api/health-assistant/statistics/summary - Get health metrics summary
 * - GET /api/health-assistant/statistics/suggestions - Get personalized suggestions
 * 
 * Requirements Validated:
 * - Requirement 7.8: Family member access with privacy filtering
 * - Requirement 7.6: Calculate and display summary metrics
 * - Requirement 7.7: Generate personalized improvement suggestions
 * - Property 27: Statistics Metric Calculation Accuracy
 * - Property 28: Pattern-Based Suggestion Generation
 * - Property 29: Family Privacy Filtering
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@RestController
@RequestMapping("/api/health-assistant/statistics")
public class HealthStatisticsController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthStatisticsController.class);
    
    @Autowired
    private HealthStatisticsService healthStatisticsService;
    
    @Autowired
    private FamilyAccessService familyAccessService;
    
    /**
     * Get emotion trends for specified time period
     * 
     * This endpoint retrieves daily emotion scores and primary emotions for
     * visualization in line charts. Supports 7, 30, and 90 day periods as
     * specified in requirements. Data is optimized for elderly users with
     * clear trend indicators.
     * 
     * Query Parameters:
     * - days: Number of days to analyze (7, 30, or 90) - defaults to 7
     * - userId: Optional user ID for family member access (requires authorization)
     * 
     * Family Member Access:
     * When a family member accesses another user's statistics, privacy filtering
     * is applied to show only aggregated data without detailed diary content.
     * The system verifies explicit consent has been granted before allowing access.
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Determine target user (self or family member)
     * 3. Verify family access authorization if needed
     * 4. Retrieve emotion trend data from service
     * 5. Return trend data with daily data points
     * 
     * @param days Number of days to analyze (7, 30, or 90) - defaults to 7
     * @param targetUserId Optional user ID for family member access
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing emotion trend data
     * 
     * Example Request (own data):
     * GET /api/health-assistant/statistics/trends?days=30
     * Authorization: Bearer {jwt_token}
     * 
     * Example Request (family member access):
     * GET /api/health-assistant/statistics/trends?days=30&userId=12345
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response:
     * {
     *   "userId": 12345,
     *   "days": 30,
     *   "dataPoints": [
     *     {
     *       "date": "2024-01-15",
     *       "emotionScore": 7.5,
     *       "primaryEmotion": "HAPPY",
     *       "intensityScore": 75
     *     },
     *     {
     *       "date": "2024-01-14",
     *       "emotionScore": 6.0,
     *       "primaryEmotion": "CONTENT",
     *       "intensityScore": 60
     *     }
     *   ],
     *   "averageEmotionScore": 6.8,
     *   "trendDirection": "improving"
     * }
     * 
     * Error Responses:
     * - 400 Bad Request: Invalid days parameter (not 7, 30, or 90)
     * - 403 Forbidden: Family member access not authorized
     * - 401 Unauthorized: Invalid or missing JWT token
     * 
     * Requirements: 7.1, 7.2, 7.6, 7.8
     * Properties: 27, 29
     */
    @GetMapping("/trends")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<EmotionTrendData>> getEmotionTrends(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) Long targetUserId,
            @CurrentUser UserPrincipal currentUser) {
        
        // Determine which user's data to retrieve
        Long userId = determineTargetUser(targetUserId, currentUser);
        
        logger.info("Retrieving emotion trends for user {} ({} days) - requested by user {}", 
                userId, days, currentUser.getId());
        
        try {
            // Validate days parameter
            if (days != 7 && days != 30 && days != 90) {
                logger.warn("Invalid days parameter: {}. Must be 7, 30, or 90", days);
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "天数参数必须是7、30或90"));
            }
            
            // Verify family access authorization if accessing another user's data
            if (!userId.equals(currentUser.getId())) {
                if (!isFamilyAccessAuthorized(userId, currentUser.getId())) {
                    logger.warn("User {} not authorized to access statistics for user {}", 
                            currentUser.getId(), userId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权访问此用户的统计数据"));
                }
            }
            
            // Get emotion trends
            EmotionTrendData trendData = healthStatisticsService.getEmotionTrends(userId, days);
            
            logger.info("Retrieved emotion trends for user {} ({} data points)", 
                    userId, trendData.getDataPoints().size());
            
            return ResponseEntity.ok(ApiResponse.success(trendData));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求参数错误: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to retrieve emotion trends: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "获取情绪趋势失败"));
        }
    }
    
    /**
     * Get emotion distribution with emotion type grouping
     * 
     * This endpoint retrieves percentage distribution of different emotion types
     * for the specified time period. Data is optimized for pie chart or bar graph
     * visualization with clear percentage labels and emotion type indicators.
     * 
     * Query Parameters:
     * - days: Number of days to analyze (7, 30, or 90) - defaults to 7
     * - userId: Optional user ID for family member access (requires authorization)
     * 
     * Family Member Access:
     * When a family member accesses another user's statistics, privacy filtering
     * is applied to show only aggregated emotion distribution without detailed
     * diary content or specific message text.
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Determine target user (self or family member)
     * 3. Verify family access authorization if needed
     * 4. Retrieve emotion distribution from service
     * 5. Return distribution with percentages and counts
     * 
     * @param days Number of days to analyze (7, 30, or 90) - defaults to 7
     * @param targetUserId Optional user ID for family member access
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing emotion distribution data
     * 
     * Example Request:
     * GET /api/health-assistant/statistics/distribution?days=30
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response:
     * {
     *   "userId": 12345,
     *   "days": 30,
     *   "distribution": {
     *     "HAPPY": 35.5,
     *     "CONTENT": 28.2,
     *     "ANXIOUS": 15.8,
     *     "LONELY": 12.3,
     *     "SAD": 8.2
     *   },
     *   "counts": {
     *     "HAPPY": 11,
     *     "CONTENT": 9,
     *     "ANXIOUS": 5,
     *     "LONELY": 4,
     *     "SAD": 3
     *   },
     *   "totalAnalyses": 32,
     *   "dominantEmotion": "HAPPY",
     *   "positivePercentage": 63.7,
     *   "negativePercentage": 36.3
     * }
     * 
     * Error Responses:
     * - 400 Bad Request: Invalid days parameter (not 7, 30, or 90)
     * - 403 Forbidden: Family member access not authorized
     * - 401 Unauthorized: Invalid or missing JWT token
     * 
     * Requirements: 7.3, 7.6, 7.8
     * Properties: 27, 29
     */
    @GetMapping("/distribution")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<EmotionDistribution>> getEmotionDistribution(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) Long targetUserId,
            @CurrentUser UserPrincipal currentUser) {
        
        // Determine which user's data to retrieve
        Long userId = determineTargetUser(targetUserId, currentUser);
        
        logger.info("Retrieving emotion distribution for user {} ({} days) - requested by user {}", 
                userId, days, currentUser.getId());
        
        try {
            // Validate days parameter
            if (days != 7 && days != 30 && days != 90) {
                logger.warn("Invalid days parameter: {}. Must be 7, 30, or 90", days);
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "天数参数必须是7、30或90"));
            }
            
            // Verify family access authorization if accessing another user's data
            if (!userId.equals(currentUser.getId())) {
                if (!isFamilyAccessAuthorized(userId, currentUser.getId())) {
                    logger.warn("User {} not authorized to access statistics for user {}", 
                            currentUser.getId(), userId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权访问此用户的统计数据"));
                }
            }
            
            // Get emotion distribution
            EmotionDistribution distribution = healthStatisticsService.getEmotionDistribution(userId, days);
            
            logger.info("Retrieved emotion distribution for user {} ({} total analyses)", 
                    userId, distribution.getTotalAnalyses());
            
            return ResponseEntity.ok(ApiResponse.success(distribution));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求参数错误: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to retrieve emotion distribution: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "获取情绪分布失败"));
        }
    }
    
    /**
     * Get health metrics summary with average calculations
     * 
     * This endpoint retrieves comprehensive health metrics including average
     * emotion score, sleep quality, stress level, and trend indicators. Data
     * is optimized for summary card display with large, clear numbers suitable
     * for elderly users.
     * 
     * Query Parameters:
     * - days: Number of days to analyze (7, 30, or 90) - defaults to 7
     * - userId: Optional user ID for family member access (requires authorization)
     * 
     * Family Member Access:
     * When a family member accesses another user's statistics, privacy filtering
     * is applied to show only aggregated summary metrics without detailed diary
     * content. High-risk indicators are included to alert family members of
     * potential concerns.
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Determine target user (self or family member)
     * 3. Verify family access authorization if needed
     * 4. Retrieve health summary from service
     * 5. Return summary with averages and trends
     * 
     * @param days Number of days to analyze (7, 30, or 90) - defaults to 7
     * @param targetUserId Optional user ID for family member access
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing health metrics summary
     * 
     * Example Request:
     * GET /api/health-assistant/statistics/summary?days=30
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response:
     * {
     *   "userId": 12345,
     *   "days": 30,
     *   "averageEmotionScore": 6.8,
     *   "averageSleepQuality": 3.5,
     *   "averageStressLevel": 2.8,
     *   "emotionTrend": "improving",
     *   "sleepTrend": "stable",
     *   "stressTrend": "declining",
     *   "totalEntries": 28,
     *   "highRiskDays": 2,
     *   "latestRiskLevel": 1,
     *   "overallHealthScore": 72.5
     * }
     * 
     * Error Responses:
     * - 400 Bad Request: Invalid days parameter (not 7, 30, or 90)
     * - 403 Forbidden: Family member access not authorized
     * - 401 Unauthorized: Invalid or missing JWT token
     * 
     * Requirements: 7.4, 7.5, 7.6, 7.8
     * Properties: 27, 29
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<HealthMetricsSummary>> getHealthSummary(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) Long targetUserId,
            @CurrentUser UserPrincipal currentUser) {
        
        // Determine which user's data to retrieve
        Long userId = determineTargetUser(targetUserId, currentUser);
        
        logger.info("Retrieving health summary for user {} ({} days) - requested by user {}", 
                userId, days, currentUser.getId());
        
        try {
            // Validate days parameter
            if (days != 7 && days != 30 && days != 90) {
                logger.warn("Invalid days parameter: {}. Must be 7, 30, or 90", days);
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "天数参数必须是7、30或90"));
            }
            
            // Verify family access authorization if accessing another user's data
            if (!userId.equals(currentUser.getId())) {
                if (!isFamilyAccessAuthorized(userId, currentUser.getId())) {
                    logger.warn("User {} not authorized to access statistics for user {}", 
                            currentUser.getId(), userId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权访问此用户的统计数据"));
                }
            }
            
            // Get health summary
            HealthMetricsSummary summary = healthStatisticsService.getHealthSummary(userId, days);
            
            logger.info("Retrieved health summary for user {} ({} total entries, overall score: {})", 
                    userId, summary.getTotalEntries(), summary.getOverallHealthScore());
            
            return ResponseEntity.ok(ApiResponse.success(summary));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求参数错误: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to retrieve health summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "获取健康摘要失败"));
        }
    }
    
    /**
     * Get personalized improvement suggestions
     * 
     * This endpoint generates personalized suggestions based on the user's health
     * data patterns. Suggestions are tailored for elderly users and consider emotion
     * trends, sleep quality patterns, stress level patterns, and recent high-risk
     * indicators.
     * 
     * Query Parameters:
     * - userId: Optional user ID for family member access (requires authorization)
     * 
     * Family Member Access:
     * Family members can access suggestions to better understand how to support
     * the elderly user. Suggestions are privacy-appropriate and do not expose
     * detailed diary content.
     * 
     * Process Flow:
     * 1. Authenticate user via JWT token
     * 2. Determine target user (self or family member)
     * 3. Verify family access authorization if needed
     * 4. Generate personalized suggestions from service
     * 5. Return list of 2-5 actionable suggestions
     * 
     * @param targetUserId Optional user ID for family member access
     * @param currentUser Authenticated user principal (injected by Spring Security)
     * @return ResponseEntity containing list of personalized suggestions
     * 
     * Example Request:
     * GET /api/health-assistant/statistics/suggestions
     * Authorization: Bearer {jwt_token}
     * 
     * Example Response:
     * [
     *   "您的睡眠质量有所改善，建议继续保持规律的作息时间",
     *   "最近压力水平较高，建议多进行户外活动和社交互动",
     *   "情绪趋势向好，继续保持积极的生活态度",
     *   "建议每天进行30分钟的轻度运动，如散步或太极"
     * ]
     * 
     * Error Responses:
     * - 403 Forbidden: Family member access not authorized
     * - 401 Unauthorized: Invalid or missing JWT token
     * 
     * Requirements: 7.7, 7.8
     * Properties: 28, 29
     */
    @GetMapping("/suggestions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<String>>> getPersonalizedSuggestions(
            @RequestParam(required = false) Long targetUserId,
            @CurrentUser UserPrincipal currentUser) {
        
        // Determine which user's data to retrieve
        Long userId = determineTargetUser(targetUserId, currentUser);
        
        logger.info("Generating personalized suggestions for user {} - requested by user {}", 
                userId, currentUser.getId());
        
        try {
            // Verify family access authorization if accessing another user's data
            if (!userId.equals(currentUser.getId())) {
                if (!isFamilyAccessAuthorized(userId, currentUser.getId())) {
                    logger.warn("User {} not authorized to access statistics for user {}", 
                            currentUser.getId(), userId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "无权访问此用户的建议"));
                }
            }
            
            // Generate personalized suggestions
            List<String> suggestions = healthStatisticsService.generatePersonalizedSuggestions(userId);
            
            logger.info("Generated {} personalized suggestions for user {}", 
                    suggestions.size(), userId);
            
            return ResponseEntity.ok(ApiResponse.success(suggestions));
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求参数错误: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to generate personalized suggestions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "生成建议失败"));
        }
    }
    
    /**
     * Determine the target user for statistics retrieval
     * 
     * If targetUserId is provided, use it (for family member access).
     * Otherwise, use the current authenticated user's ID.
     * 
     * @param targetUserId Optional target user ID
     * @param currentUser Current authenticated user
     * @return User ID to retrieve statistics for
     */
    private Long determineTargetUser(Long targetUserId, UserPrincipal currentUser) {
        return targetUserId != null ? targetUserId : currentUser.getId();
    }
    
    /**
     * Check if family member access is authorized
     * 
     * This method verifies that the requesting user (family member) has explicit
     * consent to access the target user's (elderly user's) health statistics.
     * 
     * Implementation:
     * This method now uses the FamilyAccessService to perform proper authorization checks:
     * 1. Verifies a verified family relationship exists
     * 2. Checks that valid consent for HEALTH_STATISTICS permission exists
     * 3. Validates that the consent is still active (not expired or revoked)
     * 
     * Requirements: 12.4 - Role-based access control with explicit user consent
     * Property 53: Family Access Consent Requirement
     * 
     * @param targetUserId The elderly user whose data is being accessed
     * @param requestingUserId The family member requesting access
     * @return true if access is authorized, false otherwise
     */
    private boolean isFamilyAccessAuthorized(Long targetUserId, Long requestingUserId) {
        logger.debug("Checking family access authorization: target={}, requesting={}", 
                targetUserId, requestingUserId);
        
        // Use FamilyAccessService to check authorization
        boolean authorized = familyAccessService.isAuthorizedForHealthStatistics(targetUserId, requestingUserId);
        
        if (!authorized) {
            logger.warn("Family access authorization denied: target={}, requesting={}", 
                    targetUserId, requestingUserId);
        } else {
            logger.info("Family access authorization granted: target={}, requesting={}", 
                    targetUserId, requestingUserId);
        }
        
        return authorized;
    }
}
