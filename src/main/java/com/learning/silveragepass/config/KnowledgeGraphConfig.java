package com.learning.silveragepass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 知识图谱配置
 * Knowledge Graph Configuration
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "rag.knowledge-graph")
public class KnowledgeGraphConfig {
    
    /** 是否启用知识图谱 */
    private boolean enabled = true;
    
    /** 最大跳数（查询扩展时的最大深度） */
    private int maxHops = 2;
    
    /** 每跳最大返回实体数 */
    private int maxEntitiesPerHop = 10;
    
    /** 最小关系权重阈值 */
    private double minRelationWeight = 0.3;
    
    /** 是否启用降级（服务不可用时跳过知识图谱扩展） */
    private boolean fallbackEnabled = true;
    
    /** 查询超时时间（毫秒） */
    private long timeoutMs = 3000;

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getMaxHops() { return maxHops; }
    public void setMaxHops(int maxHops) { this.maxHops = maxHops; }
    
    public int getMaxEntitiesPerHop() { return maxEntitiesPerHop; }
    public void setMaxEntitiesPerHop(int maxEntitiesPerHop) { this.maxEntitiesPerHop = maxEntitiesPerHop; }
    
    public double getMinRelationWeight() { return minRelationWeight; }
    public void setMinRelationWeight(double minRelationWeight) { this.minRelationWeight = minRelationWeight; }
    
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
    
    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
}
