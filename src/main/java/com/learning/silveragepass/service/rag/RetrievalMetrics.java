package com.learning.silveragepass.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 检索性能指标收集器
 * Retrieval Performance Metrics Collector
 * 
 * 收集和统计检索相关的性能指标。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Component
public class RetrievalMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(RetrievalMetrics.class);
    
    // 检索计数
    private final AtomicLong vectorSearchCount = new AtomicLong(0);
    private final AtomicLong keywordSearchCount = new AtomicLong(0);
    private final AtomicLong hybridSearchCount = new AtomicLong(0);
    private final AtomicLong rerankCount = new AtomicLong(0);
    private final AtomicLong knowledgeGraphQueryCount = new AtomicLong(0);
    
    // 耗时统计
    private final AtomicLong vectorSearchTotalMs = new AtomicLong(0);
    private final AtomicLong keywordSearchTotalMs = new AtomicLong(0);
    private final AtomicLong hybridSearchTotalMs = new AtomicLong(0);
    private final AtomicLong rerankTotalMs = new AtomicLong(0);
    
    // 错误计数
    private final AtomicLong vectorSearchErrors = new AtomicLong(0);
    private final AtomicLong keywordSearchErrors = new AtomicLong(0);
    private final AtomicLong rerankErrors = new AtomicLong(0);
    
    // 超时计数
    private final AtomicLong timeoutCount = new AtomicLong(0);
    
    // 慢查询阈值（毫秒）
    private long slowQueryThresholdMs = 1000;
    
    /**
     * 记录向量检索
     */
    public void recordVectorSearch(long durationMs, boolean success) {
        vectorSearchCount.incrementAndGet();
        vectorSearchTotalMs.addAndGet(durationMs);
        
        if (!success) {
            vectorSearchErrors.incrementAndGet();
        }
        
        if (durationMs > slowQueryThresholdMs) {
            logger.warn("Slow vector search detected: {}ms", durationMs);
        }
    }
    
    /**
     * 记录关键词检索
     */
    public void recordKeywordSearch(long durationMs, boolean success) {
        keywordSearchCount.incrementAndGet();
        keywordSearchTotalMs.addAndGet(durationMs);
        
        if (!success) {
            keywordSearchErrors.incrementAndGet();
        }
        
        if (durationMs > slowQueryThresholdMs) {
            logger.warn("Slow keyword search detected: {}ms", durationMs);
        }
    }
    
    /**
     * 记录混合检索
     */
    public void recordHybridSearch(long durationMs) {
        hybridSearchCount.incrementAndGet();
        hybridSearchTotalMs.addAndGet(durationMs);
        
        if (durationMs > slowQueryThresholdMs) {
            logger.warn("Slow hybrid search detected: {}ms", durationMs);
        }
    }
    
    /**
     * 记录重排序
     */
    public void recordRerank(long durationMs, boolean success) {
        rerankCount.incrementAndGet();
        rerankTotalMs.addAndGet(durationMs);
        
        if (!success) {
            rerankErrors.incrementAndGet();
        }
    }
    
    /**
     * 记录知识图谱查询
     */
    public void recordKnowledgeGraphQuery() {
        knowledgeGraphQueryCount.incrementAndGet();
    }
    
    /**
     * 记录超时
     */
    public void recordTimeout() {
        timeoutCount.incrementAndGet();
    }
    
    /**
     * 获取统计摘要
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        // 检索计数
        stats.put("vectorSearchCount", vectorSearchCount.get());
        stats.put("keywordSearchCount", keywordSearchCount.get());
        stats.put("hybridSearchCount", hybridSearchCount.get());
        stats.put("rerankCount", rerankCount.get());
        stats.put("knowledgeGraphQueryCount", knowledgeGraphQueryCount.get());
        
        // 平均耗时
        long vectorCount = vectorSearchCount.get();
        long keywordCount = keywordSearchCount.get();
        long hybridCount = hybridSearchCount.get();
        long rerank = rerankCount.get();
        
        stats.put("avgVectorSearchMs", vectorCount > 0 ? vectorSearchTotalMs.get() / vectorCount : 0);
        stats.put("avgKeywordSearchMs", keywordCount > 0 ? keywordSearchTotalMs.get() / keywordCount : 0);
        stats.put("avgHybridSearchMs", hybridCount > 0 ? hybridSearchTotalMs.get() / hybridCount : 0);
        stats.put("avgRerankMs", rerank > 0 ? rerankTotalMs.get() / rerank : 0);
        
        // 错误率
        stats.put("vectorSearchErrorRate", vectorCount > 0 ? 
            (double) vectorSearchErrors.get() / vectorCount : 0);
        stats.put("keywordSearchErrorRate", keywordCount > 0 ? 
            (double) keywordSearchErrors.get() / keywordCount : 0);
        stats.put("rerankErrorRate", rerank > 0 ? 
            (double) rerankErrors.get() / rerank : 0);
        
        // 超时
        stats.put("timeoutCount", timeoutCount.get());
        
        return stats;
    }
    
    /**
     * 重置统计
     */
    public void reset() {
        vectorSearchCount.set(0);
        keywordSearchCount.set(0);
        hybridSearchCount.set(0);
        rerankCount.set(0);
        knowledgeGraphQueryCount.set(0);
        
        vectorSearchTotalMs.set(0);
        keywordSearchTotalMs.set(0);
        hybridSearchTotalMs.set(0);
        rerankTotalMs.set(0);
        
        vectorSearchErrors.set(0);
        keywordSearchErrors.set(0);
        rerankErrors.set(0);
        
        timeoutCount.set(0);
    }
    
    public void setSlowQueryThresholdMs(long slowQueryThresholdMs) {
        this.slowQueryThresholdMs = slowQueryThresholdMs;
    }
    
    public long getSlowQueryThresholdMs() {
        return slowQueryThresholdMs;
    }
}
