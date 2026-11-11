package com.learning.silveragepass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 混合检索配置
 * Hybrid Retrieval Configuration
 * 
 * 配置混合检索的各项参数，包括融合算法、权重和降级策略。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "rag.hybrid-retrieval")
public class HybridRetrievalConfig {
    
    /** 是否启用混合检索 */
    private boolean enabled = true;
    
    /** 融合算法类型: rrf (Reciprocal Rank Fusion) 或 weighted (加权求和) */
    private FusionAlgorithm fusionAlgorithm = FusionAlgorithm.RRF;
    
    /** 向量检索权重 (0.0-1.0) */
    private double vectorWeight = 0.5;
    
    /** 关键词检索权重 (0.0-1.0) */
    private double keywordWeight = 0.5;
    
    /** RRF算法的k参数，用于控制排名影响 */
    private int rrfK = 60;
    
    /** 默认返回结果数量 */
    private int defaultTopK = 10;
    
    /** 向量检索超时时间（毫秒） */
    private long vectorTimeoutMs = 3000;
    
    /** 关键词检索超时时间（毫秒） */
    private long keywordTimeoutMs = 2000;
    
    /** 是否启用向量检索失败降级 */
    private boolean vectorFallbackEnabled = true;
    
    /** 是否启用关键词检索失败降级 */
    private boolean keywordFallbackEnabled = true;
    
    /** 最小融合分数阈值 */
    private double minFusionScore = 0.0;
    
    /** 是否启用结果去重 */
    private boolean deduplicationEnabled = true;
    
    /**
     * 融合算法枚举
     */
    public enum FusionAlgorithm {
        /** Reciprocal Rank Fusion - 基于排名的融合 */
        RRF,
        /** Weighted Sum - 加权求和融合 */
        WEIGHTED
    }
    
    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public FusionAlgorithm getFusionAlgorithm() { return fusionAlgorithm; }
    public void setFusionAlgorithm(FusionAlgorithm fusionAlgorithm) { this.fusionAlgorithm = fusionAlgorithm; }
    
    public double getVectorWeight() { return vectorWeight; }
    public void setVectorWeight(double vectorWeight) { this.vectorWeight = vectorWeight; }
    
    public double getKeywordWeight() { return keywordWeight; }
    public void setKeywordWeight(double keywordWeight) { this.keywordWeight = keywordWeight; }
    
    public int getRrfK() { return rrfK; }
    public void setRrfK(int rrfK) { this.rrfK = rrfK; }
    
    public int getDefaultTopK() { return defaultTopK; }
    public void setDefaultTopK(int defaultTopK) { this.defaultTopK = defaultTopK; }
    
    public long getVectorTimeoutMs() { return vectorTimeoutMs; }
    public void setVectorTimeoutMs(long vectorTimeoutMs) { this.vectorTimeoutMs = vectorTimeoutMs; }
    
    public long getKeywordTimeoutMs() { return keywordTimeoutMs; }
    public void setKeywordTimeoutMs(long keywordTimeoutMs) { this.keywordTimeoutMs = keywordTimeoutMs; }
    
    public boolean isVectorFallbackEnabled() { return vectorFallbackEnabled; }
    public void setVectorFallbackEnabled(boolean vectorFallbackEnabled) { this.vectorFallbackEnabled = vectorFallbackEnabled; }
    
    public boolean isKeywordFallbackEnabled() { return keywordFallbackEnabled; }
    public void setKeywordFallbackEnabled(boolean keywordFallbackEnabled) { this.keywordFallbackEnabled = keywordFallbackEnabled; }
    
    public double getMinFusionScore() { return minFusionScore; }
    public void setMinFusionScore(double minFusionScore) { this.minFusionScore = minFusionScore; }
    
    public boolean isDeduplicationEnabled() { return deduplicationEnabled; }
    public void setDeduplicationEnabled(boolean deduplicationEnabled) { this.deduplicationEnabled = deduplicationEnabled; }
    
    /**
     * 动态调整权重
     */
    public void adjustWeights(double newVectorWeight, double newKeywordWeight) {
        if (newVectorWeight < 0 || newVectorWeight > 1 || newKeywordWeight < 0 || newKeywordWeight > 1) {
            throw new IllegalArgumentException("Weights must be between 0 and 1");
        }
        this.vectorWeight = newVectorWeight;
        this.keywordWeight = newKeywordWeight;
    }
}
