package com.learning.silveragepass.service.rag.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.config.RAGAgentProperties;
import com.learning.silveragepass.service.rag.EmbeddingException;
import com.learning.silveragepass.service.rag.EmbeddingService;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 嵌入服务实现
 * Embedding Service Implementation
 * 
 * 实现BGE-M3嵌入模型调用，支持：
 * - 单个文本嵌入生成
 * - 批量文本嵌入生成
 * - 嵌入缓存机制（使用LRU缓存）
 * - 中文文本处理
 * - 超时和回退处理
 * 
 * BGE-M3模型特点：
 * - 支持多语言（包括中文）
 * - 向量维度：1024
 * - 适合语义搜索和RAG应用
 * 
 * Requirements:
 * - 3.1: Embedding model converts text queries to vector embeddings
 * - 3.2: Embedding model converts documents to vector embeddings during ingestion
 * - 3.3: Embedding model supports Chinese text processing
 * - 3.5: Embedding model supports batch embedding generation for efficiency
 * - 11.1: RAG system caches common knowledge embeddings to reduce computation
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {
    
    private final RAGAgentProperties properties;
    private final ObjectMapper objectMapper;
    
    private HttpClient httpClient;
    
    // LRU缓存实现
    private Map<String, CacheEntry> embeddingCache;
    
    // 缓存统计
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    
    // 服务可用性标志
    private volatile boolean serviceAvailable = true;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_INTERVAL_MS = 60000; // 1分钟
    
    /**
     * 初始化HTTP客户端和缓存
     * Initialize HTTP client and cache
     */
    @PostConstruct
    public void init() {
        // 初始化HTTP客户端
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getEmbedding().getTimeoutMs()))
                .build();
        
        // 初始化LRU缓存
        int maxSize = properties.getCache().getEmbeddingCacheMaxSize();
        this.embeddingCache = new ConcurrentHashMap<>(maxSize);
        
        log.info("EmbeddingService initialized with provider: {}, model: {}, dimension: {}, cache enabled: {}",
                properties.getEmbedding().getProvider(),
                properties.getEmbedding().getModelName(),
                properties.getEmbedding().getDimension(),
                properties.getCache().isEmbeddingCacheEnabled());
    }
    
    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw EmbeddingException.invalidInput("Text cannot be null or empty");
        }
        
        // 检查缓存
        if (properties.getCache().isEmbeddingCacheEnabled()) {
            float[] cached = getFromCache(text);
            if (cached != null) {
                cacheHits.incrementAndGet();
                log.debug("Cache hit for text: {}", truncateText(text));
                return cached;
            }
            cacheMisses.incrementAndGet();
        }
        
        // 调用嵌入API
        float[] embedding = callEmbeddingApi(text);
        
        // 存入缓存
        if (properties.getCache().isEmbeddingCacheEnabled()) {
            putToCache(text, embedding);
        }
        
        return embedding;
    }
    
    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw EmbeddingException.invalidInput("Texts list cannot be null or empty");
        }
        
        // 验证所有文本
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i) == null || texts.get(i).isBlank()) {
                throw EmbeddingException.invalidInput("Text at index " + i + " cannot be null or empty");
            }
        }
        
        List<float[]> results = new ArrayList<>(texts.size());
        List<String> uncachedTexts = new ArrayList<>();
        List<Integer> uncachedIndices = new ArrayList<>();
        
        // 检查缓存
        if (properties.getCache().isEmbeddingCacheEnabled()) {
            for (int i = 0; i < texts.size(); i++) {
                float[] cached = getFromCache(texts.get(i));
                if (cached != null) {
                    results.add(cached);
                    cacheHits.incrementAndGet();
                } else {
                    results.add(null); // 占位
                    uncachedTexts.add(texts.get(i));
                    uncachedIndices.add(i);
                    cacheMisses.incrementAndGet();
                }
            }
        } else {
            uncachedTexts.addAll(texts);
            for (int i = 0; i < texts.size(); i++) {
                uncachedIndices.add(i);
                results.add(null);
            }
        }
        
        // 批量调用API获取未缓存的嵌入
        if (!uncachedTexts.isEmpty()) {
            List<float[]> newEmbeddings = callBatchEmbeddingApi(uncachedTexts);
            
            // 填充结果并更新缓存
            for (int i = 0; i < uncachedTexts.size(); i++) {
                int originalIndex = uncachedIndices.get(i);
                float[] embedding = newEmbeddings.get(i);
                results.set(originalIndex, embedding);
                
                if (properties.getCache().isEmbeddingCacheEnabled()) {
                    putToCache(uncachedTexts.get(i), embedding);
                }
            }
        }
        
        log.debug("Batch embedding completed: {} texts, {} from cache, {} from API",
                texts.size(), texts.size() - uncachedTexts.size(), uncachedTexts.size());
        
        return results;
    }
    
    @Override
    public int getDimension() {
        return properties.getEmbedding().getDimension();
    }
    
    @Override
    public boolean isAvailable() {
        // 定期检查服务可用性
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime > HEALTH_CHECK_INTERVAL_MS) {
            checkServiceHealth();
            lastHealthCheckTime = now;
        }
        return serviceAvailable;
    }
    
    @Override
    public String getModelName() {
        return properties.getEmbedding().getModelName();
    }
    
    @Override
    public void clearCache() {
        embeddingCache.clear();
        cacheHits.set(0);
        cacheMisses.set(0);
        log.info("Embedding cache cleared");
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
                embeddingCache.size(),
                properties.getCache().getEmbeddingCacheMaxSize(),
                hitRate
        );
    }
    
    /**
     * 调用嵌入API生成单个文本的嵌入
     * Call embedding API to generate embedding for a single text
     */
    private float[] callEmbeddingApi(String text) {
        try {
            String provider = properties.getEmbedding().getProvider();
            
            return switch (provider.toLowerCase()) {
                case "bge-m3" -> callBgeM3Api(text);
                case "openai" -> callOpenAiApi(text);
                case "local" -> callLocalApi(text);
                default -> throw EmbeddingException.configurationError(
                        "Unsupported embedding provider: " + provider);
            };
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate embedding for text: {}", truncateText(text), e);
            serviceAvailable = false;
            throw EmbeddingException.apiError("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量调用嵌入API
     * Call embedding API in batch
     */
    private List<float[]> callBatchEmbeddingApi(List<String> texts) {
        try {
            String provider = properties.getEmbedding().getProvider();
            int batchSize = properties.getEmbedding().getBatchSize();
            
            List<float[]> allEmbeddings = new ArrayList<>();
            
            // 分批处理
            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);
                
                List<float[]> batchEmbeddings = switch (provider.toLowerCase()) {
                    case "bge-m3" -> callBgeM3BatchApi(batch);
                    case "openai" -> callOpenAiBatchApi(batch);
                    case "local" -> callLocalBatchApi(batch);
                    default -> throw EmbeddingException.configurationError(
                            "Unsupported embedding provider: " + provider);
                };
                
                allEmbeddings.addAll(batchEmbeddings);
            }
            
            return allEmbeddings;
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate batch embeddings", e);
            serviceAvailable = false;
            throw EmbeddingException.apiError("Failed to generate batch embeddings: " + e.getMessage(), e);
        }
    }
    
    /**
     * 调用BGE-M3 API
     * Call BGE-M3 API
     */
    private float[] callBgeM3Api(String text) throws Exception {
        List<float[]> results = callBgeM3BatchApi(List.of(text));
        return results.get(0);
    }
    
    /**
     * 批量调用BGE-M3 API
     * Call BGE-M3 API in batch
     */
    private List<float[]> callBgeM3BatchApi(List<String> texts) throws Exception {
        String apiUrl = properties.getEmbedding().getApiBaseUrl() + "/embeddings";
        
        // 构建请求体
        BgeM3Request request = new BgeM3Request();
        request.setModel(properties.getEmbedding().getModelName());
        request.setInput(texts);
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(properties.getEmbedding().getTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));
        
        // 添加API密钥（如果配置了）
        String apiKey = properties.getEmbedding().getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }
        
        HttpResponse<String> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("API returned status code: " + response.statusCode() + 
                    ", body: " + response.body());
        }
        
        // 解析响应
        BgeM3Response bgeResponse = objectMapper.readValue(response.body(), BgeM3Response.class);
        
        List<float[]> embeddings = new ArrayList<>();
        for (BgeM3Response.EmbeddingData data : bgeResponse.getData()) {
            embeddings.add(toFloatArray(data.getEmbedding()));
        }
        
        serviceAvailable = true;
        return embeddings;
    }
    
    /**
     * 调用OpenAI兼容API
     * Call OpenAI compatible API
     */
    private float[] callOpenAiApi(String text) throws Exception {
        List<float[]> results = callOpenAiBatchApi(List.of(text));
        return results.get(0);
    }
    
    /**
     * 批量调用OpenAI兼容API
     * Call OpenAI compatible API in batch
     */
    private List<float[]> callOpenAiBatchApi(List<String> texts) throws Exception {
        // OpenAI API格式与BGE-M3相同
        return callBgeM3BatchApi(texts);
    }
    
    /**
     * 调用本地嵌入服务
     * Call local embedding service
     */
    private float[] callLocalApi(String text) throws Exception {
        List<float[]> results = callLocalBatchApi(List.of(text));
        return results.get(0);
    }
    
    /**
     * 批量调用本地嵌入服务
     * Call local embedding service in batch
     */
    private List<float[]> callLocalBatchApi(List<String> texts) throws Exception {
        // 本地服务使用相同的API格式
        return callBgeM3BatchApi(texts);
    }
    
    /**
     * 从缓存获取嵌入
     * Get embedding from cache
     */
    private float[] getFromCache(String text) {
        String key = generateCacheKey(text);
        CacheEntry entry = embeddingCache.get(key);
        
        if (entry == null) {
            return null;
        }
        
        // 检查是否过期
        long ttlMs = properties.getCache().getEmbeddingCacheTtlSeconds() * 1000;
        if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
            embeddingCache.remove(key);
            return null;
        }
        
        // 更新访问时间（LRU）
        entry.lastAccessTime = System.currentTimeMillis();
        return entry.embedding;
    }
    
    /**
     * 存入缓存
     * Put embedding to cache
     */
    private void putToCache(String text, float[] embedding) {
        String key = generateCacheKey(text);
        
        // 检查缓存大小，如果超过限制则清理最旧的条目
        int maxSize = properties.getCache().getEmbeddingCacheMaxSize();
        if (embeddingCache.size() >= maxSize) {
            evictOldestEntry();
        }
        
        CacheEntry entry = new CacheEntry();
        entry.embedding = embedding;
        entry.timestamp = System.currentTimeMillis();
        entry.lastAccessTime = System.currentTimeMillis();
        
        embeddingCache.put(key, entry);
    }
    
    /**
     * 清理最旧的缓存条目
     * Evict oldest cache entry
     */
    private void evictOldestEntry() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry> entry : embeddingCache.entrySet()) {
            if (entry.getValue().lastAccessTime < oldestTime) {
                oldestTime = entry.getValue().lastAccessTime;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            embeddingCache.remove(oldestKey);
            log.debug("Evicted oldest cache entry");
        }
    }
    
    /**
     * 生成缓存键
     * Generate cache key
     */
    private String generateCacheKey(String text) {
        // 使用文本的哈希值作为缓存键
        return String.valueOf(text.hashCode());
    }
    
    /**
     * 检查服务健康状态
     * Check service health
     */
    private void checkServiceHealth() {
        try {
            // 发送一个简单的测试请求
            String testText = "健康检查测试";
            callEmbeddingApi(testText);
            serviceAvailable = true;
            log.debug("Embedding service health check passed");
        } catch (Exception e) {
            serviceAvailable = false;
            log.warn("Embedding service health check failed: {}", e.getMessage());
        }
    }
    
    /**
     * 截断文本用于日志
     * Truncate text for logging
     */
    private String truncateText(String text) {
        if (text.length() <= 50) {
            return text;
        }
        return text.substring(0, 50) + "...";
    }
    
    /**
     * 将Double列表转换为float数组
     * Convert Double list to float array
     */
    private float[] toFloatArray(List<Double> doubles) {
        float[] floats = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            floats[i] = doubles.get(i).floatValue();
        }
        return floats;
    }
    
    /**
     * 缓存条目
     * Cache entry
     */
    private static class CacheEntry {
        float[] embedding;
        long timestamp;
        long lastAccessTime;
    }
    
    /**
     * BGE-M3 API请求体
     * BGE-M3 API request body
     */
    @Data
    private static class BgeM3Request {
        private String model;
        private List<String> input;
    }
    
    /**
     * BGE-M3 API响应体
     * BGE-M3 API response body
     */
    @Data
    private static class BgeM3Response {
        private String object;
        private List<EmbeddingData> data;
        private String model;
        private Usage usage;
        
        @Data
        public static class EmbeddingData {
            private String object;
            private int index;
            private List<Double> embedding;
        }
        
        @Data
        public static class Usage {
            @JsonProperty("prompt_tokens")
            private int promptTokens;
            @JsonProperty("total_tokens")
            private int totalTokens;
        }
    }
}
