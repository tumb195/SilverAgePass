package com.learning.silveragepass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 元数据增强配置
 * Metadata Enhancement Configuration
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "rag.metadata-enhancement")
public class MetadataEnhancementConfig {
    
    /** 是否启用元数据增强 */
    private boolean enabled = true;
    
    /** 最大关键词数量 */
    private int maxKeywords = 10;
    
    /** 最大摘要长度（字符数） */
    private int maxSummaryLength = 200;
    
    /** 是否启用医学实体提取 */
    private boolean medicalEntityExtractionEnabled = true;
    
    /** 是否启用主题分类 */
    private boolean categoryClassificationEnabled = true;
    
    /** 批量处理大小 */
    private int batchSize = 10;
    
    /** 处理超时时间（毫秒） */
    private long timeoutMs = 30000;
    
    /** 最小置信度阈值 */
    private double minConfidence = 0.5;

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getMaxKeywords() { return maxKeywords; }
    public void setMaxKeywords(int maxKeywords) { this.maxKeywords = maxKeywords; }
    
    public int getMaxSummaryLength() { return maxSummaryLength; }
    public void setMaxSummaryLength(int maxSummaryLength) { this.maxSummaryLength = maxSummaryLength; }
    
    public boolean isMedicalEntityExtractionEnabled() { return medicalEntityExtractionEnabled; }
    public void setMedicalEntityExtractionEnabled(boolean medicalEntityExtractionEnabled) { 
        this.medicalEntityExtractionEnabled = medicalEntityExtractionEnabled; 
    }
    
    public boolean isCategoryClassificationEnabled() { return categoryClassificationEnabled; }
    public void setCategoryClassificationEnabled(boolean categoryClassificationEnabled) { 
        this.categoryClassificationEnabled = categoryClassificationEnabled; 
    }
    
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    
    public double getMinConfidence() { return minConfidence; }
    public void setMinConfidence(double minConfidence) { this.minConfidence = minConfidence; }
}
