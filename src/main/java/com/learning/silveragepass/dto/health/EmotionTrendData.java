package com.learning.silveragepass.dto.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 情绪趋势数据DTO
 * Emotion trend data transfer object
 * 
 * Contains daily emotion data points for trend visualization.
 * Supports line chart rendering with emotion scores and types.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionTrendData implements Serializable {
    
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
     * 每日情绪数据点列表
     */
    private List<EmotionDataPoint> dataPoints;
    
    /**
     * 平均情绪评分
     */
    private Double averageEmotionScore;
    
    /**
     * 趋势方向（"improving", "stable", "declining"）
     */
    private String trendDirection;
    
    /**
     * 单个情绪数据点
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionDataPoint implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * 日期
         */
        private LocalDate date;
        
        /**
         * 情绪评分（1-10）
         */
        private Double emotionScore;
        
        /**
         * 主要情感类型
         */
        private String primaryEmotion;
        
        /**
         * 情感强度（0-100）
         */
        private Integer intensityScore;
    }
}
