package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.health.*;

import java.util.List;

/**
 * 健康统计服务接口
 * Health statistics service interface
 * 
 * Provides aggregated health metrics and trend analysis for elderly users.
 * Implements caching for performance optimization of frequently accessed statistics.
 * 
 * Key Features:
 * - Emotion trend analysis for 7/30/90 day periods
 * - Emotion distribution with type grouping
 * - Health summary with average calculations
 * - Personalized suggestions based on patterns
 * - Redis caching for computed statistics
 * 
 * Requirements Validated:
 * - Requirement 7.6: Calculate and display summary metrics
 * - Requirement 7.7: Generate personalized improvement suggestions
 * - Property 27: Statistics Metric Calculation Accuracy
 * - Property 28: Pattern-Based Suggestion Generation
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
public interface HealthStatisticsService {

    /**
     * 获取用户的情绪趋势数据
     * Get emotion trends for specified time period
     * 
     * Returns daily emotion scores and primary emotions for visualization.
     * Supports 7, 30, and 90 day periods as specified in requirements.
     * 
     * @param userId The ID of the user
     * @param days Number of days to analyze (7, 30, or 90)
     * @return Emotion trend data with daily data points
     * @throws IllegalArgumentException if userId is null or days is invalid
     */
    EmotionTrendData getEmotionTrends(Long userId, int days);

    /**
     * 获取用户的情绪分布统计
     * Get emotion distribution with emotion type grouping
     * 
     * Returns percentage distribution of different emotion types
     * for the specified time period.
     * 
     * @param userId The ID of the user
     * @param days Number of days to analyze (7, 30, or 90)
     * @return Emotion distribution with percentages by type
     * @throws IllegalArgumentException if userId is null or days is invalid
     */
    EmotionDistribution getEmotionDistribution(Long userId, int days);

    /**
     * 获取用户的健康摘要统计
     * Get health summary with average calculations
     * 
     * Returns comprehensive health metrics including:
     * - Average emotion score
     * - Average sleep quality
     * - Average stress level
     * - Trend indicators (improving/declining)
     * 
     * @param userId The ID of the user
     * @param days Number of days to analyze (7, 30, or 90)
     * @return Health metrics summary
     * @throws IllegalArgumentException if userId is null or days is invalid
     */
    HealthMetricsSummary getHealthSummary(Long userId, int days);

    /**
     * 生成个性化改善建议
     * Generate personalized suggestions based on patterns
     * 
     * Analyzes user's health data patterns and generates actionable
     * suggestions tailored for elderly users. Considers:
     * - Emotion trends
     * - Sleep quality patterns
     * - Stress level patterns
     * - Recent high-risk indicators
     * 
     * @param userId The ID of the user
     * @return List of personalized suggestions (2-5 suggestions)
     * @throws IllegalArgumentException if userId is null
     */
    List<String> generatePersonalizedSuggestions(Long userId);

    /**
     * 清除用户的统计缓存
     * Clear cached statistics for a user
     * 
     * Should be called when new diary entries or analyses are created
     * to ensure fresh data on next request.
     * 
     * @param userId The ID of the user
     */
    void clearStatisticsCache(Long userId);
}
