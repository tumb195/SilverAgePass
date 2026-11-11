package com.learning.silveragepass.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RAG监控器
 * RAG Monitor
 * 
 * 记录和监控RAG系统的性能指标。
 * 
 * Requirements:
 * - 14.1: Health assistant logs all RAG retrieval operations (query, results, latency)
 * - 14.3: Health assistant tracks embedding generation metrics (count, latency, errors)
 * - 14.4: Health assistant tracks vector database query metrics (count, latency, result quality)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Component
public class RAGMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(RAGMonitor.class);
    
    /**
     * 检索操作计数器
     */
    private final AtomicLong retrievalCount = new AtomicLong(0);
    private final AtomicLong retrievalSuccessCount = new AtomicLong(0);
    private final AtomicLong retrievalFailureCount = new AtomicLong(0);
    
    /**
     * 嵌入操作计数器
     */
    private final AtomicLong embeddingCount = new AtomicLong(0);
    private final AtomicLong embeddingSuccessCount = new AtomicLong(0);
    private final AtomicLong embeddingFailureCount = new AtomicLong(0);
    
    /**
     * 向量数据库操作计数器
     */
    private final AtomicLong vectorQueryCount = new AtomicLong(0);
    private final AtomicLong vectorQuerySuccessCount = new AtomicLong(0);
    private final AtomicLong vectorQueryFailureCount = new AtomicLong(0);
    
    /**
     * 延迟统计
     */
    private final AtomicLong totalRetrievalLatencyMs = new AtomicLong(0);
    private final AtomicLong totalEmbeddingLatencyMs = new AtomicLong(0);
    private final AtomicLong totalVectorQueryLatencyMs = new AtomicLong(0);
    
    /**
     * 最近的操作记录（用于详细分析）
     */
    private final List<RetrievalRecord> recentRetrievals = new ArrayList<>();
    private static final int MAX_RECENT_RECORDS = 100;
    
    /**
     * 记录检索操作
     * Record retrieval operation
     * 
     * @param query 查询 (query)
     * @param resultCount 结果数量 (result count)
     * @param latencyMs 延迟毫秒 (latency in milliseconds)
     * @param success 是否成功 (whether successful)
     */
    public void recordRetrieval(String query, int resultCount, long latencyMs, boolean success) {
        retrievalCount.incrementAndGet();
        totalRetrievalLatencyMs.addAndGet(latencyMs);
        
        if (success) {
            retrievalSuccessCount.incrementAndGet();
        } else {
            retrievalFailureCount.incrementAndGet();
        }
        
        // 记录详细信息
        RetrievalRecord record = new RetrievalRecord();
        record.setQuery(query);
        record.setResultCount(resultCount);
        record.setLatencyMs(latencyMs);
        record.setSuccess(success);
        record.setTimestamp(LocalDateTime.now());
        
        addRecentRecord(record);
        
        // 日志记录
        log.info("RAG Retrieval: query='{}', results={}, latency={}ms, success={}",
                truncateQuery(query), resultCount, latencyMs, success);
    }
    
    /**
     * 记录嵌入操作
     * Record embedding operation
     * 
     * @param textCount 文本数量 (text count)
     * @param latencyMs 延迟毫秒 (latency in milliseconds)
     * @param success 是否成功 (whether successful)
     */
    public void recordEmbedding(int textCount, long latencyMs, boolean success) {
        embeddingCount.incrementAndGet();
        totalEmbeddingLatencyMs.addAndGet(latencyMs);
        
        if (success) {
            embeddingSuccessCount.incrementAndGet();
        } else {
            embeddingFailureCount.incrementAndGet();
        }
        
        log.debug("Embedding: texts={}, latency={}ms, success={}", textCount, latencyMs, success);
    }
    
    /**
     * 记录向量数据库查询
     * Record vector database query
     * 
     * @param resultCount 结果数量 (result count)
     * @param latencyMs 延迟毫秒 (latency in milliseconds)
     * @param success 是否成功 (whether successful)
     */
    public void recordVectorQuery(int resultCount, long latencyMs, boolean success) {
        vectorQueryCount.incrementAndGet();
        totalVectorQueryLatencyMs.addAndGet(latencyMs);
        
        if (success) {
            vectorQuerySuccessCount.incrementAndGet();
        } else {
            vectorQueryFailureCount.incrementAndGet();
        }
        
        log.debug("Vector Query: results={}, latency={}ms, success={}", resultCount, latencyMs, success);
    }
    
    /**
     * 获取RAG指标
     * Get RAG metrics
     * 
     * @return RAG指标 (RAG metrics)
     */
    public RAGMetrics getMetrics() {
        RAGMetrics metrics = new RAGMetrics();
        
        // 检索指标
        metrics.setRetrievalCount(retrievalCount.get());
        metrics.setRetrievalSuccessCount(retrievalSuccessCount.get());
        metrics.setRetrievalFailureCount(retrievalFailureCount.get());
        metrics.setRetrievalSuccessRate(calculateSuccessRate(
                retrievalSuccessCount.get(), retrievalCount.get()));
        metrics.setAverageRetrievalLatencyMs(calculateAverageLatency(
                totalRetrievalLatencyMs.get(), retrievalCount.get()));
        
        // 嵌入指标
        metrics.setEmbeddingCount(embeddingCount.get());
        metrics.setEmbeddingSuccessCount(embeddingSuccessCount.get());
        metrics.setEmbeddingFailureCount(embeddingFailureCount.get());
        metrics.setEmbeddingSuccessRate(calculateSuccessRate(
                embeddingSuccessCount.get(), embeddingCount.get()));
        metrics.setAverageEmbeddingLatencyMs(calculateAverageLatency(
                totalEmbeddingLatencyMs.get(), embeddingCount.get()));
        
        // 向量查询指标
        metrics.setVectorQueryCount(vectorQueryCount.get());
        metrics.setVectorQuerySuccessCount(vectorQuerySuccessCount.get());
        metrics.setVectorQueryFailureCount(vectorQueryFailureCount.get());
        metrics.setVectorQuerySuccessRate(calculateSuccessRate(
                vectorQuerySuccessCount.get(), vectorQueryCount.get()));
        metrics.setAverageVectorQueryLatencyMs(calculateAverageLatency(
                totalVectorQueryLatencyMs.get(), vectorQueryCount.get()));
        
        return metrics;
    }
    
    /**
     * 获取最近的检索记录
     * Get recent retrieval records
     * 
     * @param limit 限制数量 (limit)
     * @return 检索记录列表 (list of retrieval records)
     */
    public List<RetrievalRecord> getRecentRetrievals(int limit) {
        synchronized (recentRetrievals) {
            int size = recentRetrievals.size();
            int start = Math.max(0, size - limit);
            return new ArrayList<>(recentRetrievals.subList(start, size));
        }
    }
    
    /**
     * 重置指标
     * Reset metrics
     */
    public void resetMetrics() {
        retrievalCount.set(0);
        retrievalSuccessCount.set(0);
        retrievalFailureCount.set(0);
        embeddingCount.set(0);
        embeddingSuccessCount.set(0);
        embeddingFailureCount.set(0);
        vectorQueryCount.set(0);
        vectorQuerySuccessCount.set(0);
        vectorQueryFailureCount.set(0);
        totalRetrievalLatencyMs.set(0);
        totalEmbeddingLatencyMs.set(0);
        totalVectorQueryLatencyMs.set(0);
        
        synchronized (recentRetrievals) {
            recentRetrievals.clear();
        }
        
        log.info("RAG metrics reset");
    }
    
    // ==========================================
    // Private Helper Methods
    // ==========================================
    
    private void addRecentRecord(RetrievalRecord record) {
        synchronized (recentRetrievals) {
            recentRetrievals.add(record);
            while (recentRetrievals.size() > MAX_RECENT_RECORDS) {
                recentRetrievals.remove(0);
            }
        }
    }
    
    private double calculateSuccessRate(long successCount, long totalCount) {
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) successCount / totalCount * 100.0;
    }
    
    private double calculateAverageLatency(long totalLatency, long count) {
        if (count == 0) {
            return 0.0;
        }
        return (double) totalLatency / count;
    }
    
    private String truncateQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.length() > 50 ? query.substring(0, 50) + "..." : query;
    }
    
    /**
     * RAG指标类
     */
    public static class RAGMetrics {
        private long retrievalCount;
        private long retrievalSuccessCount;
        private long retrievalFailureCount;
        private double retrievalSuccessRate;
        private double averageRetrievalLatencyMs;
        
        private long embeddingCount;
        private long embeddingSuccessCount;
        private long embeddingFailureCount;
        private double embeddingSuccessRate;
        private double averageEmbeddingLatencyMs;
        
        private long vectorQueryCount;
        private long vectorQuerySuccessCount;
        private long vectorQueryFailureCount;
        private double vectorQuerySuccessRate;
        private double averageVectorQueryLatencyMs;
        
        // Getters and Setters
        public long getRetrievalCount() { return retrievalCount; }
        public void setRetrievalCount(long retrievalCount) { this.retrievalCount = retrievalCount; }
        
        public long getRetrievalSuccessCount() { return retrievalSuccessCount; }
        public void setRetrievalSuccessCount(long retrievalSuccessCount) { this.retrievalSuccessCount = retrievalSuccessCount; }
        
        public long getRetrievalFailureCount() { return retrievalFailureCount; }
        public void setRetrievalFailureCount(long retrievalFailureCount) { this.retrievalFailureCount = retrievalFailureCount; }
        
        public double getRetrievalSuccessRate() { return retrievalSuccessRate; }
        public void setRetrievalSuccessRate(double retrievalSuccessRate) { this.retrievalSuccessRate = retrievalSuccessRate; }
        
        public double getAverageRetrievalLatencyMs() { return averageRetrievalLatencyMs; }
        public void setAverageRetrievalLatencyMs(double averageRetrievalLatencyMs) { this.averageRetrievalLatencyMs = averageRetrievalLatencyMs; }
        
        public long getEmbeddingCount() { return embeddingCount; }
        public void setEmbeddingCount(long embeddingCount) { this.embeddingCount = embeddingCount; }
        
        public long getEmbeddingSuccessCount() { return embeddingSuccessCount; }
        public void setEmbeddingSuccessCount(long embeddingSuccessCount) { this.embeddingSuccessCount = embeddingSuccessCount; }
        
        public long getEmbeddingFailureCount() { return embeddingFailureCount; }
        public void setEmbeddingFailureCount(long embeddingFailureCount) { this.embeddingFailureCount = embeddingFailureCount; }
        
        public double getEmbeddingSuccessRate() { return embeddingSuccessRate; }
        public void setEmbeddingSuccessRate(double embeddingSuccessRate) { this.embeddingSuccessRate = embeddingSuccessRate; }
        
        public double getAverageEmbeddingLatencyMs() { return averageEmbeddingLatencyMs; }
        public void setAverageEmbeddingLatencyMs(double averageEmbeddingLatencyMs) { this.averageEmbeddingLatencyMs = averageEmbeddingLatencyMs; }
        
        public long getVectorQueryCount() { return vectorQueryCount; }
        public void setVectorQueryCount(long vectorQueryCount) { this.vectorQueryCount = vectorQueryCount; }
        
        public long getVectorQuerySuccessCount() { return vectorQuerySuccessCount; }
        public void setVectorQuerySuccessCount(long vectorQuerySuccessCount) { this.vectorQuerySuccessCount = vectorQuerySuccessCount; }
        
        public long getVectorQueryFailureCount() { return vectorQueryFailureCount; }
        public void setVectorQueryFailureCount(long vectorQueryFailureCount) { this.vectorQueryFailureCount = vectorQueryFailureCount; }
        
        public double getVectorQuerySuccessRate() { return vectorQuerySuccessRate; }
        public void setVectorQuerySuccessRate(double vectorQuerySuccessRate) { this.vectorQuerySuccessRate = vectorQuerySuccessRate; }
        
        public double getAverageVectorQueryLatencyMs() { return averageVectorQueryLatencyMs; }
        public void setAverageVectorQueryLatencyMs(double averageVectorQueryLatencyMs) { this.averageVectorQueryLatencyMs = averageVectorQueryLatencyMs; }
    }
    
    /**
     * 检索记录类
     */
    public static class RetrievalRecord {
        private String query;
        private int resultCount;
        private long latencyMs;
        private boolean success;
        private LocalDateTime timestamp;
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public int getResultCount() { return resultCount; }
        public void setResultCount(int resultCount) { this.resultCount = resultCount; }
        
        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
