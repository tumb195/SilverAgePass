package com.learning.silveragepass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 重排序配置
 * Reranking Configuration
 * 
 * 配置重排序器的各项参数。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "rag.reranking")
public class RerankingConfig {
    
    /** 是否启用重排序 */
    private boolean enabled = true;
    
    /** 重排序模型端点URL */
    private String modelEndpoint = "http://localhost:8080/rerank";
    
    /** 重排序模型名称 */
    private String modelName = "bge-reranker-base";
    
    /** 请求超时时间（毫秒） */
    private long timeoutMs = 5000;
    
    /** 批量处理大小 */
    private int batchSize = 10;
    
    /** 默认返回结果数量 */
    private int defaultTopK = 5;
    
    /** 最小相关性分数阈值 */
    private double minRelevanceScore = 0.0;
    
    /** 是否启用降级（服务不可用时返回原始结果） */
    private boolean fallbackEnabled = true;
    
    /** 重试次数 */
    private int maxRetries = 2;
    
    /** 重试间隔（毫秒） */
    private long retryDelayMs = 100;

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getModelEndpoint() { return modelEndpoint; }
    public void setModelEndpoint(String modelEndpoint) { this.modelEndpoint = modelEndpoint; }
    
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    
    public int getDefaultTopK() { return defaultTopK; }
    public void setDefaultTopK(int defaultTopK) { this.defaultTopK = defaultTopK; }
    
    public double getMinRelevanceScore() { return minRelevanceScore; }
    public void setMinRelevanceScore(double minRelevanceScore) { this.minRelevanceScore = minRelevanceScore; }
    
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public long getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
}
