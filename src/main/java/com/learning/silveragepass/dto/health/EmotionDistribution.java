package com.learning.silveragepass.dto.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 情绪分布统计DTO
 * Emotion distribution data transfer object
 * 
 * Contains percentage distribution of emotion types for pie/bar chart visualization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmotionDistribution implements Serializable {
    
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
     * 情绪类型分布（情绪类型 -> 百分比）
     * Map of emotion type to percentage (0-100)
     */
    private Map<String, Double> distribution;
    
    /**
     * 情绪类型计数（情绪类型 -> 出现次数）
     * Map of emotion type to count
     */
    private Map<String, Integer> counts;
    
    /**
     * 总分析次数
     */
    private int totalAnalyses;
    
    /**
     * 主导情绪类型（出现最多的情绪）
     */
    private String dominantEmotion;
    
    /**
     * 积极情绪百分比
     */
    private Double positivePercentage;
    
    /**
     * 消极情绪百分比
     */
    private Double negativePercentage;
}
