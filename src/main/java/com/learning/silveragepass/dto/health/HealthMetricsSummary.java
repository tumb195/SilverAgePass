package com.learning.silveragepass.dto.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 健康指标摘要DTO
 * Health metrics summary data transfer object
 * 
 * Contains aggregated health metrics with averages and trends.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricsSummary implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 分析天数
     */
    private int days;
    
    /**
     * 平均情绪评分（1-10）
     */
    private Double averageEmotionScore;
    
    /**
     * 平均睡眠质量（1-5）
     */
    private Double averageSleepQuality;
    
    /**
     * 平均压力水平（1-5）
     */
    private Double averageStressLevel;
    
    /**
     * 情绪趋势（"improving", "stable", "declining"）
     */
    private String emotionTrend;
    
    /**
     * 睡眠趋势（"improving", "stable", "declining"）
     */
    private String sleepTrend;
    
    /**
     * 压力趋势（"improving", "stable", "declining"）
     */
    private String stressTrend;
    
    /**
     * 日记条目总数
     */
    private int totalEntries;
    
    /**
     * 高风险天数（风险等级≥2）
     */
    private int highRiskDays;
    
    /**
     * 最近一次分析的风险等级
     */
    private Integer latestRiskLevel;
    
    /**
     * 整体健康评分（0-100，综合计算）
     */
    private Double overallHealthScore;
}
