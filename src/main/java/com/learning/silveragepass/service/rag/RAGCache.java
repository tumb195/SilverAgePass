package com.learning.silveragepass.service.rag;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.learning.silveragepass.config.RAGAgentProperties;
import com.learning.silveragepass.dto.rag.RetrievalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * RAG缓存管理器
 * RAG Cache Manager
 * 
 * 提供嵌入缓存和检索结果缓存，减少重复计算和API调用。
 * 
 * Requirements:
 * - 11.1: RAG system caches common knowledge embeddings to reduce computation
 * - 11.2: RAG system supports batch embedding to reduce API calls
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Component
public class RAGCache {
    
    private static final Logger log = LoggerFactory.getLogger(RAGCache.class);
    
    private final RAGAgentProperties properties;
    
    /**
     * 嵌入缓存
     * Embedding cache: text -> embedding vector
     */
    private Cache<String, float[]> embeddingCache;
    
    /**
     * 检索结果缓存
     * Retrieval result cache: query hash -> results
     */
    private Cache<String, List<RetrievalResult>> retrievalCache;
    
    @Autowired
    public RAGCache(RAGAgentProperties properties) {
        this.properties = properties;
    }
    
    @PostConstruct
    public void init() {
        // 初始化嵌入缓存
        if (properties.getCache().isEmbeddingCacheEnabled()) {
            embeddingCache = Caffeine.newBuilder()
                    .maximumSize(properties.getCache().getEmbeddingCacheMaxSize())
                    .expireAfterWrite(properties.getCache().getEmbeddingCacheTtlSeconds(), TimeUnit.SECONDS)
                    .recordStats()
                    .build();
            log.info("Embedding cache initialized: maxSize={}, ttl={}s",
                    properties.getCache().getEmbeddingCacheMaxSize(),
                    properties.getCache().getEmbeddingCacheTtlSeconds());
        }
        
        // 初始化检索结果缓存
        if (properties.getCache().isRetrievalCacheEnabled()) {
            retrievalCache = Caffeine.newBuilder()
                    .maximumSize(properties.getCache().getRetrievalCacheMaxSize())
                    .expireAfterWrite(properties.getCache().getRetrievalCacheTtlSeconds(), TimeUnit.SECONDS)
                    .recordStats()
                    .build();
            log.info("Retrieval cache initialized: maxSize={}, ttl={}s",
                    properties.getCache().getRetrievalCacheMaxSize(),
                    properties.getCache().getRetrievalCacheTtlSeconds());
        }
    }
    
    // ==========================================
    // Embedding Cache Methods
    // ==========================================
    
    /**
     * 获取缓存的嵌入向量
     * Get cached embedding
     * 
     * @param text 文本 (text)
     * @return 嵌入向量，如果未缓存则返回null (embedding vector, or null if not cached)
     */
    public float[] getEmbedding(String text) {
        if (embeddingCache == null || text == null) {
            return null;
        }
        
        String key = generateEmbeddingKey(text);
        float[] embedding = embeddingCache.getIfPresent(key);
        
        if (embedding != null) {
            log.debug("Embedding cache hit for text: {}", truncate(text));
        }
        
        return embedding;
    }
    
    /**
     * 缓存嵌入向量
     * Cache embedding
     * 
     * @param text 文本 (text)
     * @param embedding 嵌入向量 (embedding vector)
     */
    public void putEmbedding(String text, float[] embedding) {
        if (embeddingCache == null || text == null || embedding == null) {
            return;
        }
        
        String key = generateEmbeddingKey(text);
        embeddingCache.put(key, embedding);
        log.debug("Cached embedding for text: {}", truncate(text));
    }
    
    /**
     * 检查嵌入是否已缓存
     * Check if embedding is cached
     * 
     * @param text 文本 (text)
     * @return true如果已缓存 (true if cached)
     */
    public boolean hasEmbedding(String text) {
        if (embeddingCache == null || text == null) {
            return false;
        }
        return embeddingCache.getIfPresent(generateEmbeddingKey(text)) != null;
    }
    
    // ==========================================
    // Retrieval Cache Methods
    // ==========================================
    
    /**
     * 获取缓存的检索结果
     * Get cached retrieval results
     * 
     * @param query 查询 (query)
     * @param topK top-k值 (top-k value)
     * @param threshold 阈值 (threshold)
     * @return 检索结果列表，如果未缓存则返回null (retrieval results, or null if not cached)
     */
    public List<RetrievalResult> getRetrievalResults(String query, int topK, double threshold) {
        if (retrievalCache == null || query == null) {
            return null;
        }
        
        String key = generateRetrievalKey(query, topK, threshold);
        List<RetrievalResult> results = retrievalCache.getIfPresent(key);
        
        if (results != null) {
            log.debug("Retrieval cache hit for query: {}", truncate(query));
        }
        
        return results;
    }
    
    /**
     * 缓存检索结果
     * Cache retrieval results
     * 
     * @param query 查询 (query)
     * @param topK top-k值 (top-k value)
     * @param threshold 阈值 (threshold)
     * @param results 检索结果 (retrieval results)
     */
    public void putRetrievalResults(String query, int topK, double threshold, 
                                    List<RetrievalResult> results) {
        if (retrievalCache == null || query == null || results == null) {
            return;
        }
        
        String key = generateRetrievalKey(query, topK, threshold);
        retrievalCache.put(key, results);
        log.debug("Cached retrieval results for query: {}", truncate(query));
    }
    
    /**
     * 检查检索结果是否已缓存
     * Check if retrieval results are cached
     * 
     * @param query 查询 (query)
     * @param topK top-k值 (top-k value)
     * @param threshold 阈值 (threshold)
     * @return true如果已缓存 (true if cached)
     */
    public boolean hasRetrievalResults(String query, int topK, double threshold) {
        if (retrievalCache == null || query == null) {
            return false;
        }
        return retrievalCache.getIfPresent(generateRetrievalKey(query, topK, threshold)) != null;
    }
    
    // ==========================================
    // Cache Management Methods
    // ==========================================
    
    /**
     * 清除所有缓存
     * Clear all caches
     */
    public void clearAll() {
        if (embeddingCache != null) {
            embeddingCache.invalidateAll();
        }
        if (retrievalCache != null) {
            retrievalCache.invalidateAll();
        }
        log.info("All RAG caches cleared");
    }
    
    /**
     * 清除嵌入缓存
     * Clear embedding cache
     */
    public void clearEmbeddingCache() {
        if (embeddingCache != null) {
            embeddingCache.invalidateAll();
            log.info("Embedding cache cleared");
        }
    }
    
    /**
     * 清除检索结果缓存
     * Clear retrieval cache
     */
    public void clearRetrievalCache() {
        if (retrievalCache != null) {
            retrievalCache.invalidateAll();
            log.info("Retrieval cache cleared");
        }
    }
    
    /**
     * 获取缓存统计信息
     * Get cache statistics
     * 
     * @return 缓存统计 (cache statistics)
     */
    public CacheStatistics getStatistics() {
        CacheStatistics stats = new CacheStatistics();
        
        if (embeddingCache != null) {
            var embeddingStats = embeddingCache.stats();
            stats.setEmbeddingCacheSize(embeddingCache.estimatedSize());
            stats.setEmbeddingCacheHitCount(embeddingStats.hitCount());
            stats.setEmbeddingCacheMissCount(embeddingStats.missCount());
            stats.setEmbeddingCacheHitRate(embeddingStats.hitRate());
        }
        
        if (retrievalCache != null) {
            var retrievalStats = retrievalCache.stats();
            stats.setRetrievalCacheSize(retrievalCache.estimatedSize());
            stats.setRetrievalCacheHitCount(retrievalStats.hitCount());
            stats.setRetrievalCacheMissCount(retrievalStats.missCount());
            stats.setRetrievalCacheHitRate(retrievalStats.hitRate());
        }
        
        return stats;
    }
    
    /**
     * 检查嵌入缓存是否启用
     * Check if embedding cache is enabled
     * 
     * @return true如果启用 (true if enabled)
     */
    public boolean isEmbeddingCacheEnabled() {
        return embeddingCache != null;
    }
    
    /**
     * 检查检索缓存是否启用
     * Check if retrieval cache is enabled
     * 
     * @return true如果启用 (true if enabled)
     */
    public boolean isRetrievalCacheEnabled() {
        return retrievalCache != null;
    }
    
    // ==========================================
    // Private Helper Methods
    // ==========================================
    
    private String generateEmbeddingKey(String text) {
        // 使用文本的哈希值作为键
        return "emb:" + text.hashCode();
    }
    
    private String generateRetrievalKey(String query, int topK, double threshold) {
        // 组合查询、topK和阈值生成键
        return String.format("ret:%d:%d:%.2f", query.hashCode(), topK, threshold);
    }
    
    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }
    
    /**
     * 缓存统计类
     */
    public static class CacheStatistics {
        private long embeddingCacheSize;
        private long embeddingCacheHitCount;
        private long embeddingCacheMissCount;
        private double embeddingCacheHitRate;
        
        private long retrievalCacheSize;
        private long retrievalCacheHitCount;
        private long retrievalCacheMissCount;
        private double retrievalCacheHitRate;
        
        // Getters and Setters
        public long getEmbeddingCacheSize() { return embeddingCacheSize; }
        public void setEmbeddingCacheSize(long embeddingCacheSize) { this.embeddingCacheSize = embeddingCacheSize; }
        
        public long getEmbeddingCacheHitCount() { return embeddingCacheHitCount; }
        public void setEmbeddingCacheHitCount(long embeddingCacheHitCount) { this.embeddingCacheHitCount = embeddingCacheHitCount; }
        
        public long getEmbeddingCacheMissCount() { return embeddingCacheMissCount; }
        public void setEmbeddingCacheMissCount(long embeddingCacheMissCount) { this.embeddingCacheMissCount = embeddingCacheMissCount; }
        
        public double getEmbeddingCacheHitRate() { return embeddingCacheHitRate; }
        public void setEmbeddingCacheHitRate(double embeddingCacheHitRate) { this.embeddingCacheHitRate = embeddingCacheHitRate; }
        
        public long getRetrievalCacheSize() { return retrievalCacheSize; }
        public void setRetrievalCacheSize(long retrievalCacheSize) { this.retrievalCacheSize = retrievalCacheSize; }
        
        public long getRetrievalCacheHitCount() { return retrievalCacheHitCount; }
        public void setRetrievalCacheHitCount(long retrievalCacheHitCount) { this.retrievalCacheHitCount = retrievalCacheHitCount; }
        
        public long getRetrievalCacheMissCount() { return retrievalCacheMissCount; }
        public void setRetrievalCacheMissCount(long retrievalCacheMissCount) { this.retrievalCacheMissCount = retrievalCacheMissCount; }
        
        public double getRetrievalCacheHitRate() { return retrievalCacheHitRate; }
        public void setRetrievalCacheHitRate(double retrievalCacheHitRate) { this.retrievalCacheHitRate = retrievalCacheHitRate; }
    }
}
