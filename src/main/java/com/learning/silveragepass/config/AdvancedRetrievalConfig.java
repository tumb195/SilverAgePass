package com.learning.silveragepass.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 高级检索配置
 * Advanced Retrieval Configuration
 * 
 * 整合混合检索、重排序、知识图谱和元数据增强的配置。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "rag.advanced-retrieval")
public class AdvancedRetrievalConfig {
    
    /** 是否启用高级检索功能 */
    private boolean enabled = true;
    
    /** 混合检索配置 */
    private HybridConfig hybrid = new HybridConfig();
    
    /** 重排序配置 */
    private RerankConfig rerank = new RerankConfig();
    
    /** 知识图谱配置 */
    private KnowledgeGraphSettings knowledgeGraph = new KnowledgeGraphSettings();
    
    /** 元数据增强配置 */
    private MetadataSettings metadata = new MetadataSettings();
    
    /** 性能监控配置 */
    private MonitoringSettings monitoring = new MonitoringSettings();

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public HybridConfig getHybrid() { return hybrid; }
    public void setHybrid(HybridConfig hybrid) { this.hybrid = hybrid; }
    
    public RerankConfig getRerank() { return rerank; }
    public void setRerank(RerankConfig rerank) { this.rerank = rerank; }
    
    public KnowledgeGraphSettings getKnowledgeGraph() { return knowledgeGraph; }
    public void setKnowledgeGraph(KnowledgeGraphSettings knowledgeGraph) { this.knowledgeGraph = knowledgeGraph; }
    
    public MetadataSettings getMetadata() { return metadata; }
    public void setMetadata(MetadataSettings metadata) { this.metadata = metadata; }
    
    public MonitoringSettings getMonitoring() { return monitoring; }
    public void setMonitoring(MonitoringSettings monitoring) { this.monitoring = monitoring; }
    
    /**
     * 混合检索配置
     */
    public static class HybridConfig {
        private boolean enabled = true;
        private String fusionAlgorithm = "RRF"; // RRF or WEIGHTED_SUM
        private double vectorWeight = 0.6;
        private double keywordWeight = 0.4;
        private int rrfK = 60;
        private long timeoutMs = 5000;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getFusionAlgorithm() { return fusionAlgorithm; }
        public void setFusionAlgorithm(String fusionAlgorithm) { this.fusionAlgorithm = fusionAlgorithm; }
        
        public double getVectorWeight() { return vectorWeight; }
        public void setVectorWeight(double vectorWeight) { this.vectorWeight = vectorWeight; }
        
        public double getKeywordWeight() { return keywordWeight; }
        public void setKeywordWeight(double keywordWeight) { this.keywordWeight = keywordWeight; }
        
        public int getRrfK() { return rrfK; }
        public void setRrfK(int rrfK) { this.rrfK = rrfK; }
        
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
    }
    
    /**
     * 重排序配置
     */
    public static class RerankConfig {
        private boolean enabled = true;
        private String modelEndpoint = "http://localhost:8002/rerank";
        private int batchSize = 10;
        private long timeoutMs = 3000;
        private double minScore = 0.0;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getModelEndpoint() { return modelEndpoint; }
        public void setModelEndpoint(String modelEndpoint) { this.modelEndpoint = modelEndpoint; }
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
        
        public double getMinScore() { return minScore; }
        public void setMinScore(double minScore) { this.minScore = minScore; }
    }
    
    /**
     * 知识图谱配置
     */
    public static class KnowledgeGraphSettings {
        private boolean enabled = true;
        private boolean queryExpansionEnabled = true;
        private int maxHops = 2;
        private int maxEntitiesPerHop = 10;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public boolean isQueryExpansionEnabled() { return queryExpansionEnabled; }
        public void setQueryExpansionEnabled(boolean queryExpansionEnabled) { 
            this.queryExpansionEnabled = queryExpansionEnabled; 
        }
        
        public int getMaxHops() { return maxHops; }
        public void setMaxHops(int maxHops) { this.maxHops = maxHops; }
        
        public int getMaxEntitiesPerHop() { return maxEntitiesPerHop; }
        public void setMaxEntitiesPerHop(int maxEntitiesPerHop) { this.maxEntitiesPerHop = maxEntitiesPerHop; }
    }
    
    /**
     * 元数据增强配置
     */
    public static class MetadataSettings {
        private boolean enabled = true;
        private boolean autoEnhanceOnImport = true;
        private int maxKeywords = 10;
        private int maxSummaryLength = 200;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public boolean isAutoEnhanceOnImport() { return autoEnhanceOnImport; }
        public void setAutoEnhanceOnImport(boolean autoEnhanceOnImport) { 
            this.autoEnhanceOnImport = autoEnhanceOnImport; 
        }
        
        public int getMaxKeywords() { return maxKeywords; }
        public void setMaxKeywords(int maxKeywords) { this.maxKeywords = maxKeywords; }
        
        public int getMaxSummaryLength() { return maxSummaryLength; }
        public void setMaxSummaryLength(int maxSummaryLength) { this.maxSummaryLength = maxSummaryLength; }
    }
    
    /**
     * 性能监控配置
     */
    public static class MonitoringSettings {
        private boolean enabled = true;
        private boolean logSlowQueries = true;
        private long slowQueryThresholdMs = 1000;
        private boolean collectMetrics = true;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public boolean isLogSlowQueries() { return logSlowQueries; }
        public void setLogSlowQueries(boolean logSlowQueries) { this.logSlowQueries = logSlowQueries; }
        
        public long getSlowQueryThresholdMs() { return slowQueryThresholdMs; }
        public void setSlowQueryThresholdMs(long slowQueryThresholdMs) { 
            this.slowQueryThresholdMs = slowQueryThresholdMs; 
        }
        
        public boolean isCollectMetrics() { return collectMetrics; }
        public void setCollectMetrics(boolean collectMetrics) { this.collectMetrics = collectMetrics; }
    }
}
