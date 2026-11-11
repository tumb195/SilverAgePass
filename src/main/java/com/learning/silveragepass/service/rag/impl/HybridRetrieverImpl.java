package com.learning.silveragepass.service.rag.impl;

import com.learning.silveragepass.config.HybridRetrievalConfig;
import com.learning.silveragepass.config.HybridRetrievalConfig.FusionAlgorithm;
import com.learning.silveragepass.dto.rag.*;
import com.learning.silveragepass.service.rag.EmbeddingService;
import com.learning.silveragepass.service.rag.HybridRetriever;
import com.learning.silveragepass.service.rag.KeywordSearchEngine;
import com.learning.silveragepass.service.rag.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 混合检索器实现
 * Hybrid Retriever Implementation
 * 
 * 实现混合检索功能，包括：
 * - 并行执行向量检索和关键词检索
 * - RRF (Reciprocal Rank Fusion) 融合算法
 * - 加权求和融合算法
 * - 结果去重和来源标注
 * - 降级策略处理
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Service
public class HybridRetrieverImpl implements HybridRetriever {
    
    private static final Logger logger = LoggerFactory.getLogger(HybridRetrieverImpl.class);
    
    private final VectorStore vectorStore;
    private final KeywordSearchEngine keywordSearchEngine;
    private final EmbeddingService embeddingService;
    private final HybridRetrievalConfig config;
    private final ExecutorService executorService;
    
    // 动态权重（可运行时调整）
    private volatile double currentVectorWeight;
    private volatile double currentKeywordWeight;
    
    public HybridRetrieverImpl(
            VectorStore vectorStore,
            KeywordSearchEngine keywordSearchEngine,
            EmbeddingService embeddingService,
            HybridRetrievalConfig config) {
        this.vectorStore = vectorStore;
        this.keywordSearchEngine = keywordSearchEngine;
        this.embeddingService = embeddingService;
        this.config = config;
        this.currentVectorWeight = config.getVectorWeight();
        this.currentKeywordWeight = config.getKeywordWeight();
        this.executorService = Executors.newFixedThreadPool(2);
    }
    
    @Override
    public List<HybridSearchResult> search(String query, int topK, Map<String, Object> filter) {
        if (!config.isEnabled()) {
            logger.debug("Hybrid retrieval disabled, falling back to vector search");
            return vectorSearch(query, topK, filter);
        }
        
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        long startTime = System.currentTimeMillis();
        
        // 并行执行两种检索
        CompletableFuture<List<VectorSearchResult>> vectorFuture = 
            executeVectorSearchAsync(query, topK * 2, filter);
        CompletableFuture<List<KeywordSearchResult>> keywordFuture = 
            executeKeywordSearchAsync(query, topK * 2, filter);
        
        List<VectorSearchResult> vectorResults = Collections.emptyList();
        List<KeywordSearchResult> keywordResults = Collections.emptyList();
        
        try {
            // 等待向量检索结果
            vectorResults = vectorFuture.get(config.getVectorTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.warn("Vector search timed out after {}ms", config.getVectorTimeoutMs());
            if (!config.isVectorFallbackEnabled()) {
                throw new RuntimeException("Vector search timeout and fallback disabled", e);
            }
        } catch (Exception e) {
            logger.error("Vector search failed: {}", e.getMessage());
            if (!config.isVectorFallbackEnabled()) {
                throw new RuntimeException("Vector search failed and fallback disabled", e);
            }
        }
        
        try {
            // 等待关键词检索结果
            keywordResults = keywordFuture.get(config.getKeywordTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.warn("Keyword search timed out after {}ms", config.getKeywordTimeoutMs());
            if (!config.isKeywordFallbackEnabled()) {
                throw new RuntimeException("Keyword search timeout and fallback disabled", e);
            }
        } catch (Exception e) {
            logger.error("Keyword search failed: {}", e.getMessage());
            if (!config.isKeywordFallbackEnabled()) {
                throw new RuntimeException("Keyword search failed and fallback disabled", e);
            }
        }
        
        // 融合结果
        List<HybridSearchResult> fusedResults = fuseResults(vectorResults, keywordResults);
        
        // 去重
        if (config.isDeduplicationEnabled()) {
            fusedResults = deduplicateResults(fusedResults);
        }
        
        // 按融合分数排序并截取topK
        fusedResults = fusedResults.stream()
            .filter(r -> r.getFusionScore() >= config.getMinFusionScore())
            .sorted((a, b) -> Double.compare(b.getFusionScore(), a.getFusionScore()))
            .limit(topK)
            .collect(Collectors.toList());
        
        long duration = System.currentTimeMillis() - startTime;
        logger.debug("Hybrid search completed in {}ms, returned {} results", duration, fusedResults.size());
        
        return fusedResults;
    }
    
    @Override
    public List<HybridSearchResult> vectorSearch(String query, int topK, Map<String, Object> filter) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            float[] queryEmbedding = embeddingService.embed(query);
            List<VectorSearchResult> results = vectorStore.search(queryEmbedding, topK, filter);
            return results.stream()
                .map(r -> {
                    int rank = results.indexOf(r) + 1;
                    HybridSearchResult hybrid = HybridSearchResult.fromVectorResult(r, rank);
                    hybrid.setFusionScore(r.getSimilarityScore());
                    return hybrid;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Vector search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<HybridSearchResult> keywordSearch(String query, int topK, Map<String, Object> filter) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            List<KeywordSearchResult> results = keywordSearchEngine.search(query, topK, filter);
            return results.stream()
                .map(r -> {
                    int rank = results.indexOf(r) + 1;
                    HybridSearchResult hybrid = HybridSearchResult.fromKeywordResult(r, rank);
                    hybrid.setFusionScore(r.getBm25Score());
                    return hybrid;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Keyword search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * 融合向量检索和关键词检索结果
     */
    private List<HybridSearchResult> fuseResults(
            List<VectorSearchResult> vectorResults,
            List<KeywordSearchResult> keywordResults) {
        
        Map<String, HybridSearchResult> resultMap = new HashMap<>();
        
        // 处理向量检索结果
        for (int i = 0; i < vectorResults.size(); i++) {
            VectorSearchResult vr = vectorResults.get(i);
            HybridSearchResult hybrid = HybridSearchResult.fromVectorResult(vr, i + 1);
            resultMap.put(vr.getDocumentId(), hybrid);
        }
        
        // 处理关键词检索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            KeywordSearchResult kr = keywordResults.get(i);
            HybridSearchResult existing = resultMap.get(kr.getDocumentId());
            
            if (existing != null) {
                // 合并结果
                HybridSearchResult keywordHybrid = HybridSearchResult.fromKeywordResult(kr, i + 1);
                existing.mergeWith(keywordHybrid);
            } else {
                // 新增结果
                HybridSearchResult hybrid = HybridSearchResult.fromKeywordResult(kr, i + 1);
                resultMap.put(kr.getDocumentId(), hybrid);
            }
        }
        
        // 计算融合分数
        List<HybridSearchResult> results = new ArrayList<>(resultMap.values());
        for (HybridSearchResult result : results) {
            double fusionScore = calculateFusionScore(result);
            result.setFusionScore(fusionScore);
        }
        
        return results;
    }
    
    /**
     * 计算融合分数
     */
    private double calculateFusionScore(HybridSearchResult result) {
        if (config.getFusionAlgorithm() == FusionAlgorithm.RRF) {
            return calculateRRFScore(result);
        } else {
            return calculateWeightedScore(result);
        }
    }
    
    /**
     * RRF (Reciprocal Rank Fusion) 分数计算
     * 
     * RRF(d) = Σ 1 / (k + rank(d))
     * 其中k是常数（默认60），rank是文档在各检索结果中的排名
     */
    double calculateRRFScore(HybridSearchResult result) {
        double score = 0.0;
        int k = config.getRrfK();
        
        if (result.getVectorRank() != null) {
            score += currentVectorWeight / (k + result.getVectorRank());
        }
        
        if (result.getKeywordRank() != null) {
            score += currentKeywordWeight / (k + result.getKeywordRank());
        }
        
        return score;
    }
    
    /**
     * 加权求和分数计算
     * 
     * Score = vectorWeight * normalizedVectorScore + keywordWeight * normalizedKeywordScore
     */
    double calculateWeightedScore(HybridSearchResult result) {
        double score = 0.0;
        
        if (result.getVectorScore() != null) {
            // 向量分数通常在0-1之间
            score += currentVectorWeight * result.getVectorScore();
        }
        
        if (result.getKeywordScore() != null) {
            // BM25分数需要归一化（假设最大值为20）
            double normalizedKeywordScore = Math.min(result.getKeywordScore() / 20.0, 1.0);
            score += currentKeywordWeight * normalizedKeywordScore;
        }
        
        return score;
    }
    
    /**
     * 结果去重
     */
    private List<HybridSearchResult> deduplicateResults(List<HybridSearchResult> results) {
        Map<String, HybridSearchResult> uniqueResults = new LinkedHashMap<>();
        
        for (HybridSearchResult result : results) {
            String docId = result.getDocumentId();
            if (uniqueResults.containsKey(docId)) {
                // 保留融合分数更高的结果
                HybridSearchResult existing = uniqueResults.get(docId);
                if (result.getFusionScore() > existing.getFusionScore()) {
                    result.mergeWith(existing);
                    uniqueResults.put(docId, result);
                } else {
                    existing.mergeWith(result);
                }
            } else {
                uniqueResults.put(docId, result);
            }
        }
        
        return new ArrayList<>(uniqueResults.values());
    }
    
    /**
     * 异步执行向量检索
     */
    private CompletableFuture<List<VectorSearchResult>> executeVectorSearchAsync(
            String query, int topK, Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 先将查询文本转换为嵌入向量
                float[] queryEmbedding = embeddingService.embed(query);
                return vectorStore.search(queryEmbedding, topK, filter);
            } catch (Exception e) {
                logger.error("Async vector search failed: {}", e.getMessage());
                return Collections.emptyList();
            }
        }, executorService);
    }
    
    /**
     * 异步执行关键词检索
     */
    private CompletableFuture<List<KeywordSearchResult>> executeKeywordSearchAsync(
            String query, int topK, Map<String, Object> filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return keywordSearchEngine.search(query, topK, filter);
            } catch (Exception e) {
                logger.error("Async keyword search failed: {}", e.getMessage());
                return Collections.emptyList();
            }
        }, executorService);
    }
    
    @Override
    public void adjustWeights(double vectorWeight, double keywordWeight) {
        if (vectorWeight < 0 || vectorWeight > 1 || keywordWeight < 0 || keywordWeight > 1) {
            throw new IllegalArgumentException("Weights must be between 0 and 1");
        }
        this.currentVectorWeight = vectorWeight;
        this.currentKeywordWeight = keywordWeight;
        logger.info("Adjusted weights: vector={}, keyword={}", vectorWeight, keywordWeight);
    }
    
    @Override
    public double getVectorWeight() {
        return currentVectorWeight;
    }
    
    @Override
    public double getKeywordWeight() {
        return currentKeywordWeight;
    }
    
    @Override
    public boolean isAvailable() {
        return isVectorSearchAvailable() || isKeywordSearchAvailable();
    }
    
    @Override
    public boolean isVectorSearchAvailable() {
        try {
            return vectorStore != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean isKeywordSearchAvailable() {
        try {
            return keywordSearchEngine != null;
        } catch (Exception e) {
            return false;
        }
    }
}
