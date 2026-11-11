package com.learning.silveragepass.service.rag.impl;

import com.learning.silveragepass.config.RAGAgentProperties;
import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.dto.rag.RetrievalResult;
import com.learning.silveragepass.dto.rag.VectorSearchResult;
import com.learning.silveragepass.service.rag.EmbeddingService;
import com.learning.silveragepass.service.rag.RAGService;
import com.learning.silveragepass.service.rag.VectorStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * RAG检索服务实现
 * RAG Retrieval Service Implementation
 * 
 * 实现从知识库检索相关文档并增强LLM上下文，提供：
 * - 语义检索（支持top-k和相似度阈值配置）
 * - 提示词增强（将检索到的知识融入提示词）
 * - 服务健康检查
 * - 检索结果缓存（LRU缓存）
 * 
 * Requirements:
 * - 1.1: Health assistant retrieves relevant knowledge documents from knowledge base
 * - 1.2: Health assistant incorporates knowledge content into response context
 * - 1.3: Knowledge retriever supports configurable retrieval count (default top-k=5)
 * - 1.4: Knowledge retriever supports configurable similarity threshold (default 0.7)
 * - 1.5: When no relevant knowledge is retrieved, use basic LLM to generate response
 * - 1.6: Knowledge retriever includes knowledge source and confidence score in retrieval results
 * - 11.2: RAG system supports batch embedding to reduce API calls
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGServiceImpl implements RAGService {
    
    private final RAGAgentProperties properties;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    
    // LRU缓存实现
    private Map<String, CacheEntry> retrievalCache;
    
    // 缓存统计
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    // 服务可用性标志
    private volatile boolean serviceAvailable = true;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_INTERVAL_MS = 60000; // 1分钟
    
    /**
     * 初始化缓存
     * Initialize cache
     */
    @PostConstruct
    public void init() {
        // 初始化LRU缓存
        int maxSize = properties.getCache().getRetrievalCacheMaxSize();
        this.retrievalCache = new ConcurrentHashMap<>(maxSize);
        
        log.info("RAGService initialized with RAG enabled: {}, topK: {}, threshold: {}, cache enabled: {}",
                properties.getRag().isEnabled(),
                properties.getRag().getTopK(),
                properties.getRag().getSimilarityThreshold(),
                properties.getCache().isRetrievalCacheEnabled());
    }
    
    @Override
    public List<RetrievalResult> retrieve(String query, ConversationContext context, 
                                          int topK, double threshold) {
        if (!isEnabled()) {
            log.debug("RAG is disabled, returning empty results");
            return Collections.emptyList();
        }
        
        if (query == null || query.isBlank()) {
            log.warn("Query is null or empty, returning empty results");
            return Collections.emptyList();
        }
        
        long startTime = System.currentTimeMillis();
        log.debug("Retrieving knowledge for query: {}, topK: {}, threshold: {}", 
                truncateText(query), topK, threshold);
        
        try {
            // 构建增强查询（融入对话上下文）
            String enhancedQuery = buildEnhancedQuery(query, context);
            
            // 检查缓存
            String cacheKey = generateCacheKey(enhancedQuery, topK, threshold, null);
            if (properties.getCache().isRetrievalCacheEnabled()) {
                List<RetrievalResult> cached = getFromCache(cacheKey);
                if (cached != null) {
                    cacheHits.incrementAndGet();
                    log.debug("Cache hit for query: {}", truncateText(query));
                    return filterDiscussedDocuments(cached, context);
                }
                cacheMisses.incrementAndGet();
            }
            
            // 生成查询嵌入
            float[] queryEmbedding = embeddingService.embed(enhancedQuery);
            
            // 执行向量搜索
            List<VectorSearchResult> searchResults = vectorStore.search(queryEmbedding, topK, null);
            
            // 转换为RetrievalResult并过滤低于阈值的结果
            List<RetrievalResult> results = searchResults.stream()
                    .filter(r -> r.getSimilarityScore() >= threshold)
                    .map(this::convertToRetrievalResult)
                    .collect(Collectors.toList());
            
            // 存入缓存
            if (properties.getCache().isRetrievalCacheEnabled() && !results.isEmpty()) {
                putToCache(cacheKey, results);
            }
            
            // 过滤已讨论的文档
            results = filterDiscussedDocuments(results, context);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Retrieved {} knowledge documents in {}ms for query: {}", 
                    results.size(), duration, truncateText(query));
            
            serviceAvailable = true;
            return results;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to retrieve knowledge in {}ms: {}", duration, e.getMessage(), e);
            serviceAvailable = false;
            
            // Requirement 1.5: When retrieval fails, return empty list (fallback to basic LLM)
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<RetrievalResult> retrieve(String query, ConversationContext context) {
        return retrieve(query, context, 
                properties.getRag().getTopK(), 
                properties.getRag().getSimilarityThreshold());
    }
    
    @Override
    public List<RetrievalResult> retrieveByCategory(String query, ConversationContext context,
                                                    String category, int topK, double threshold) {
        if (!isEnabled()) {
            log.debug("RAG is disabled, returning empty results");
            return Collections.emptyList();
        }
        
        if (query == null || query.isBlank()) {
            log.warn("Query is null or empty, returning empty results");
            return Collections.emptyList();
        }
        
        if (category == null || category.isBlank()) {
            log.warn("Category is null or empty, falling back to non-category search");
            return retrieve(query, context, topK, threshold);
        }
        
        long startTime = System.currentTimeMillis();
        log.debug("Retrieving knowledge by category: {} for query: {}", category, truncateText(query));
        
        try {
            // 构建增强查询
            String enhancedQuery = buildEnhancedQuery(query, context);
            
            // 检查缓存
            String cacheKey = generateCacheKey(enhancedQuery, topK, threshold, category);
            if (properties.getCache().isRetrievalCacheEnabled()) {
                List<RetrievalResult> cached = getFromCache(cacheKey);
                if (cached != null) {
                    cacheHits.incrementAndGet();
                    return filterDiscussedDocuments(cached, context);
                }
                cacheMisses.incrementAndGet();
            }
            
            // 生成查询嵌入
            float[] queryEmbedding = embeddingService.embed(enhancedQuery);
            
            // 执行带类别过滤的向量搜索
            Map<String, Object> filter = Map.of("category", category);
            List<VectorSearchResult> searchResults = vectorStore.search(queryEmbedding, topK, filter);
            
            // 转换并过滤
            List<RetrievalResult> results = searchResults.stream()
                    .filter(r -> r.getSimilarityScore() >= threshold)
                    .map(this::convertToRetrievalResult)
                    .collect(Collectors.toList());
            
            // 存入缓存
            if (properties.getCache().isRetrievalCacheEnabled() && !results.isEmpty()) {
                putToCache(cacheKey, results);
            }
            
            // 过滤已讨论的文档
            results = filterDiscussedDocuments(results, context);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Retrieved {} knowledge documents by category {} in {}ms", 
                    results.size(), category, duration);
            
            serviceAvailable = true;
            return results;
            
        } catch (Exception e) {
            log.error("Failed to retrieve knowledge by category: {}", e.getMessage(), e);
            serviceAvailable = false;
            return Collections.emptyList();
        }
    }
    
    @Override
    public String augmentPrompt(String originalPrompt, List<RetrievalResult> retrievalResults) {
        return augmentPrompt(originalPrompt, retrievalResults, 
                properties.getRag().getMaxContextLength());
    }
    
    @Override
    public String augmentPrompt(String originalPrompt, List<RetrievalResult> retrievalResults, 
                                int maxContextLength) {
        if (originalPrompt == null || originalPrompt.isBlank()) {
            return originalPrompt;
        }
        
        if (retrievalResults == null || retrievalResults.isEmpty()) {
            log.debug("No retrieval results to augment prompt");
            return originalPrompt;
        }
        
        log.debug("Augmenting prompt with {} retrieval results, max context length: {}", 
                retrievalResults.size(), maxContextLength);
        
        StringBuilder augmentedPrompt = new StringBuilder();
        
        // 添加知识上下文前缀
        augmentedPrompt.append("【参考知识】\n");
        augmentedPrompt.append("以下是与用户问题相关的健康知识，请参考这些信息回答用户问题：\n\n");
        
        int currentLength = augmentedPrompt.length();
        int knowledgeCount = 0;
        
        // 按相似度排序（确保最相关的知识优先）
        List<RetrievalResult> sortedResults = retrievalResults.stream()
                .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                .collect(Collectors.toList());
        
        for (RetrievalResult result : sortedResults) {
            String knowledgeEntry = formatKnowledgeEntry(result, knowledgeCount + 1);
            
            // 检查是否超过最大长度
            if (currentLength + knowledgeEntry.length() + originalPrompt.length() > maxContextLength) {
                log.debug("Reached max context length, stopping at {} knowledge entries", knowledgeCount);
                break;
            }
            
            augmentedPrompt.append(knowledgeEntry);
            currentLength += knowledgeEntry.length();
            knowledgeCount++;
        }
        
        if (knowledgeCount == 0) {
            // 没有添加任何知识，返回原始提示词
            return originalPrompt;
        }
        
        // 添加分隔符和原始提示词
        augmentedPrompt.append("\n【用户问题】\n");
        augmentedPrompt.append(originalPrompt);
        
        // 添加回答指导
        augmentedPrompt.append("\n\n【回答要求】\n");
        augmentedPrompt.append("1. 请基于上述参考知识回答用户问题\n");
        augmentedPrompt.append("2. 如果参考知识不足以回答问题，请结合您的专业知识补充\n");
        augmentedPrompt.append("3. 请使用温暖、耐心的语气，适合老年用户阅读\n");
        
        if (properties.getRag().isIncludeSourceAttribution()) {
            augmentedPrompt.append("4. 在回答中适当标注知识来源\n");
        }
        
        log.debug("Augmented prompt with {} knowledge entries, total length: {}", 
                knowledgeCount, augmentedPrompt.length());
        
        return augmentedPrompt.toString();
    }
    
    @Override
    public boolean isAvailable() {
        // 定期检查服务可用性
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime > HEALTH_CHECK_INTERVAL_MS) {
            checkServiceHealth();
            lastHealthCheckTime = now;
        }
        return serviceAvailable && isEnabled();
    }
    
    @Override
    public boolean isEnabled() {
        return properties.getRag().isEnabled();
    }
    
    @Override
    public void clearCache() {
        if (retrievalCache != null) {
            retrievalCache.clear();
        }
        cacheHits.set(0);
        cacheMisses.set(0);
        log.info("Retrieval cache cleared");
    }
    
    @Override
    public CacheStats getCacheStats() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total : 0.0;
        
        return new CacheStats(
                hits,
                misses,
                retrievalCache != null ? retrievalCache.size() : 0,
                properties.getCache().getRetrievalCacheMaxSize(),
                hitRate
        );
    }
    
    /**
     * 构建增强查询
     * Build enhanced query
     * 
     * 融入对话上下文以提高检索相关性
     * Incorporate conversation context to improve retrieval relevance
     * 
     * Requirement 7.3: Context manager incorporates conversation context into retrieval queries
     */
    private String buildEnhancedQuery(String query, ConversationContext context) {
        if (context == null || !context.hasHistory()) {
            return query;
        }
        
        StringBuilder enhanced = new StringBuilder();
        
        // 添加用户画像信息（如果有）
        if (context.hasUserProfile() && context.getUserProfile() != null) {
            var profile = context.getUserProfile();
            if (profile.getChronicConditions() != null && !profile.getChronicConditions().isEmpty()) {
                enhanced.append("用户健康背景：").append(profile.getChronicConditions()).append("\n");
            }
        }
        
        // 添加当前主题（如果有）
        if (context.getCurrentTopic() != null && !context.getCurrentTopic().isBlank()) {
            enhanced.append("当前话题：").append(context.getCurrentTopic()).append("\n");
        }
        
        // 添加最近的对话摘要（最多3条消息）
        String summary = context.generateSummary(3);
        if (!summary.isBlank()) {
            enhanced.append("对话背景：").append(summary).append("\n");
        }
        
        // 添加原始查询
        enhanced.append("当前问题：").append(query);
        
        return enhanced.toString();
    }
    
    /**
     * 过滤已讨论的文档
     * Filter discussed documents
     * 
     * Requirement 7.5: Context manager avoids retrieving duplicate knowledge already discussed
     */
    private List<RetrievalResult> filterDiscussedDocuments(List<RetrievalResult> results, 
                                                          ConversationContext context) {
        if (context == null || results == null || results.isEmpty()) {
            return results;
        }
        
        return results.stream()
                .filter(r -> !context.isDocumentDiscussed(r.getDocumentId()))
                .collect(Collectors.toList());
    }
    
    /**
     * 转换VectorSearchResult为RetrievalResult
     * Convert VectorSearchResult to RetrievalResult
     * 
     * Requirement 1.6: Knowledge retriever includes knowledge source and confidence score
     */
    private RetrievalResult convertToRetrievalResult(VectorSearchResult searchResult) {
        // 计算置信度分数（综合考虑相似度和来源可靠性）
        double confidenceScore = calculateConfidenceScore(searchResult);
        
        // 生成知识来源描述
        String knowledgeSource = generateKnowledgeSource(searchResult);
        
        return RetrievalResult.builder()
                .documentId(searchResult.getDocumentId())
                .content(searchResult.getContent())
                .category(searchResult.getCategory())
                .similarityScore(searchResult.getSimilarityScore())
                .metadata(searchResult.getMetadata())
                .confidenceScore(confidenceScore)
                .knowledgeSource(knowledgeSource)
                .build();
    }
    
    /**
     * 计算置信度分数
     * Calculate confidence score
     * 
     * 综合考虑相似度、来源可靠性等因素
     */
    private double calculateConfidenceScore(VectorSearchResult searchResult) {
        double baseScore = searchResult.getSimilarityScore();
        
        // 根据来源类型调整置信度
        String sourceType = searchResult.getSourceType();
        double sourceMultiplier = 1.0;
        
        if (sourceType != null) {
            switch (sourceType.toLowerCase()) {
                case "official":
                case "medical":
                    sourceMultiplier = 1.1; // 官方/医学来源加权
                    break;
                case "manual":
                    sourceMultiplier = 1.05; // 人工审核来源加权
                    break;
                case "api":
                case "file":
                    sourceMultiplier = 1.0; // 标准权重
                    break;
                default:
                    sourceMultiplier = 0.95; // 未知来源降权
            }
        }
        
        // 根据版本调整（新版本可能更准确）
        Integer version = searchResult.getVersion();
        if (version != null && version > 1) {
            sourceMultiplier *= 1.02; // 更新过的文档略微加权
        }
        
        // 确保置信度在[0, 1]范围内
        return Math.min(1.0, baseScore * sourceMultiplier);
    }
    
    /**
     * 生成知识来源描述
     * Generate knowledge source description
     */
    private String generateKnowledgeSource(VectorSearchResult searchResult) {
        String sourceName = searchResult.getSourceName();
        String sourceType = searchResult.getSourceType();
        String category = searchResult.getCategory();
        
        if (sourceName != null && !sourceName.isBlank()) {
            return "来源：" + sourceName;
        }
        
        if (sourceType != null && !sourceType.isBlank()) {
            return "来源：" + sourceType;
        }
        
        if (category != null && !category.isBlank()) {
            return "来源：" + getCategoryDisplayName(category) + "知识库";
        }
        
        return "来源：健康知识库";
    }
    
    /**
     * 获取类别显示名称
     * Get category display name
     */
    private String getCategoryDisplayName(String category) {
        if (category == null) {
            return "未分类";
        }
        
        switch (category) {
            case "elderly_care":
                return "老年护理";
            case "chronic_disease":
                return "慢病管理";
            case "medication":
                return "用药指导";
            case "mental_health":
                return "心理健康";
            default:
                return category;
        }
    }
    
    /**
     * 格式化知识条目
     * Format knowledge entry
     */
    private String formatKnowledgeEntry(RetrievalResult result, int index) {
        StringBuilder entry = new StringBuilder();
        
        entry.append("---\n");
        entry.append("知识").append(index).append("：\n");
        entry.append(result.getContent()).append("\n");
        
        if (properties.getRag().isIncludeSourceAttribution()) {
            entry.append("（").append(result.getFormattedCitation());
            entry.append("，相关度：").append(String.format("%.0f%%", result.getSimilarityScore() * 100));
            entry.append("）\n");
        }
        
        return entry.toString();
    }
    
    /**
     * 生成缓存键
     * Generate cache key
     */
    private String generateCacheKey(String query, int topK, double threshold, String category) {
        StringBuilder key = new StringBuilder();
        key.append(query.hashCode());
        key.append("_").append(topK);
        key.append("_").append((int)(threshold * 100));
        if (category != null) {
            key.append("_").append(category);
        }
        return key.toString();
    }
    
    /**
     * 从缓存获取结果
     * Get results from cache
     */
    private List<RetrievalResult> getFromCache(String key) {
        if (retrievalCache == null) {
            return null;
        }
        
        CacheEntry entry = retrievalCache.get(key);
        if (entry == null) {
            return null;
        }
        
        // 检查是否过期
        long ttlMs = properties.getCache().getRetrievalCacheTtlSeconds() * 1000;
        if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
            retrievalCache.remove(key);
            return null;
        }
        
        // 更新访问时间（LRU）
        entry.lastAccessTime = System.currentTimeMillis();
        
        // 返回副本以避免外部修改
        return new ArrayList<>(entry.results);
    }
    
    /**
     * 存入缓存
     * Put results to cache
     */
    private void putToCache(String key, List<RetrievalResult> results) {
        if (retrievalCache == null) {
            return;
        }
        
        // 检查缓存大小，如果超过限制则清理最旧的条目
        int maxSize = properties.getCache().getRetrievalCacheMaxSize();
        if (retrievalCache.size() >= maxSize) {
            evictOldestEntry();
        }
        
        CacheEntry entry = new CacheEntry();
        entry.results = new ArrayList<>(results); // 存储副本
        entry.timestamp = System.currentTimeMillis();
        entry.lastAccessTime = System.currentTimeMillis();
        
        retrievalCache.put(key, entry);
    }
    
    /**
     * 清理最旧的缓存条目
     * Evict oldest cache entry
     */
    private void evictOldestEntry() {
        if (retrievalCache == null || retrievalCache.isEmpty()) {
            return;
        }
        
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry> entry : retrievalCache.entrySet()) {
            if (entry.getValue().lastAccessTime < oldestTime) {
                oldestTime = entry.getValue().lastAccessTime;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            retrievalCache.remove(oldestKey);
            log.debug("Evicted oldest cache entry");
        }
    }
    
    /**
     * 检查服务健康状态
     * Check service health
     */
    private void checkServiceHealth() {
        try {
            boolean embeddingAvailable = embeddingService.isAvailable();
            boolean vectorStoreAvailable = vectorStore.isAvailable();
            
            serviceAvailable = embeddingAvailable && vectorStoreAvailable;
            
            if (!serviceAvailable) {
                log.warn("RAG service health check: embedding={}, vectorStore={}", 
                        embeddingAvailable, vectorStoreAvailable);
            } else {
                log.debug("RAG service health check passed");
            }
        } catch (Exception e) {
            serviceAvailable = false;
            log.warn("RAG service health check failed: {}", e.getMessage());
        }
    }
    
    /**
     * 截断文本用于日志
     * Truncate text for logging
     */
    private String truncateText(String text) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= 50) {
            return text;
        }
        return text.substring(0, 50) + "...";
    }
    
    /**
     * 缓存条目
     * Cache entry
     */
    private static class CacheEntry {
        List<RetrievalResult> results;
        long timestamp;
        long lastAccessTime;
    }
}
