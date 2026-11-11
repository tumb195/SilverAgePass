package com.learning.silveragepass.service.rag.impl;

import com.learning.silveragepass.config.AdvancedRetrievalConfig;
import com.learning.silveragepass.config.RAGAgentProperties;
import com.learning.silveragepass.dto.rag.*;
import com.learning.silveragepass.service.rag.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强版RAG检索服务实现
 * Enhanced RAG Retrieval Service Implementation
 * 
 * 集成高级检索功能：
 * - 混合检索（向量 + 关键词）
 * - 知识图谱查询扩展
 * - 重排序
 * - 元数据过滤
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
@Service("enhancedRAGService")
@Primary
public class EnhancedRAGServiceImpl implements RAGService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedRAGServiceImpl.class);
    
    private final RAGAgentProperties properties;
    private final AdvancedRetrievalConfig advancedConfig;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    
    @Autowired(required = false)
    private HybridRetriever hybridRetriever;
    
    @Autowired(required = false)
    private KnowledgeGraph knowledgeGraph;
    
    @Autowired(required = false)
    private Reranker reranker;
    
    @Autowired(required = false)
    private RetrievalMetrics metrics;
    
    public EnhancedRAGServiceImpl(
            RAGAgentProperties properties,
            AdvancedRetrievalConfig advancedConfig,
            EmbeddingService embeddingService,
            VectorStore vectorStore) {
        this.properties = properties;
        this.advancedConfig = advancedConfig;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }
    
    @Override
    public List<RetrievalResult> retrieve(String query, ConversationContext context, 
                                          int topK, double threshold) {
        if (!isEnabled()) {
            logger.debug("RAG is disabled, returning empty results");
            return Collections.emptyList();
        }
        
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        
        long startTime = System.currentTimeMillis();
        logger.debug("Enhanced retrieval for query: {}", truncate(query));
        
        try {
            // 1. 知识图谱查询扩展
            List<String> expandedQueries = expandQueryWithKnowledgeGraph(query);
            
            // 2. 执行混合检索或基础检索
            List<RetrievalResult> results;
            if (advancedConfig.isEnabled() && advancedConfig.getHybrid().isEnabled() && hybridRetriever != null) {
                results = executeHybridRetrieval(expandedQueries, topK, threshold);
            } else {
                results = executeBasicRetrieval(query, topK, threshold);
            }
            
            // 3. 重排序
            if (advancedConfig.isEnabled() && advancedConfig.getRerank().isEnabled() && reranker != null) {
                results = rerankResults(query, results, topK);
            }
            
            // 4. 过滤已讨论的文档
            results = filterDiscussedDocuments(results, context);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Enhanced retrieval completed: {} results in {}ms", results.size(), duration);
            
            if (metrics != null) {
                metrics.recordHybridSearch(duration);
            }
            
            return results;
            
        } catch (Exception e) {
            logger.error("Enhanced retrieval failed: {}", e.getMessage(), e);
            // 降级到基础检索
            return executeBasicRetrieval(query, topK, threshold);
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
        List<RetrievalResult> results = retrieve(query, context, topK * 2, threshold);
        
        // 按类别过滤
        if (category != null && !category.isBlank()) {
            results = results.stream()
                    .filter(r -> category.equals(r.getCategory()))
                    .limit(topK)
                    .collect(Collectors.toList());
        }
        
        return results;
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
            return originalPrompt;
        }
        
        StringBuilder augmented = new StringBuilder();
        augmented.append("【参考知识】\n");
        
        int currentLength = augmented.length();
        int count = 0;
        
        for (RetrievalResult result : retrievalResults) {
            String entry = formatKnowledgeEntry(result, count + 1);
            
            if (currentLength + entry.length() + originalPrompt.length() > maxContextLength) {
                break;
            }
            
            augmented.append(entry);
            currentLength += entry.length();
            count++;
        }
        
        if (count == 0) {
            return originalPrompt;
        }
        
        augmented.append("\n【用户问题】\n");
        augmented.append(originalPrompt);
        
        return augmented.toString();
    }
    
    @Override
    public boolean isAvailable() {
        return isEnabled() && embeddingService.isAvailable() && vectorStore.isAvailable();
    }
    
    @Override
    public boolean isEnabled() {
        return properties.getRag().isEnabled();
    }
    
    @Override
    public void clearCache() {
        // 清除缓存逻辑
        logger.info("Cache cleared");
    }
    
    @Override
    public CacheStats getCacheStats() {
        return CacheStats.empty();
    }
    
    // ==================== Private Methods ====================
    
    private List<String> expandQueryWithKnowledgeGraph(String query) {
        if (!advancedConfig.isEnabled() || 
            !advancedConfig.getKnowledgeGraph().isQueryExpansionEnabled() ||
            knowledgeGraph == null || 
            !knowledgeGraph.isAvailable()) {
            return Collections.singletonList(query);
        }
        
        try {
            List<String> expanded = knowledgeGraph.expandQuery(query);
            logger.debug("Query expanded from '{}' to {} terms", truncate(query), expanded.size());
            
            if (metrics != null) {
                metrics.recordKnowledgeGraphQuery();
            }
            
            return expanded;
        } catch (Exception e) {
            logger.warn("Knowledge graph expansion failed: {}", e.getMessage());
            return Collections.singletonList(query);
        }
    }
    
    private List<RetrievalResult> executeHybridRetrieval(List<String> queries, int topK, double threshold) {
        if (hybridRetriever == null) {
            return executeBasicRetrieval(queries.get(0), topK, threshold);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 使用第一个查询（原始查询）进行混合检索
            String primaryQuery = queries.get(0);
            List<HybridSearchResult> hybridResults = hybridRetriever.search(primaryQuery, topK, null);
            
            // 转换为 RetrievalResult
            List<RetrievalResult> results = hybridResults.stream()
                    .filter(r -> r.getFusionScore() >= threshold)
                    .map(this::convertHybridToRetrieval)
                    .collect(Collectors.toList());
            
            long duration = System.currentTimeMillis() - startTime;
            if (metrics != null) {
                metrics.recordHybridSearch(duration);
            }
            
            return results;
            
        } catch (Exception e) {
            logger.error("Hybrid retrieval failed: {}", e.getMessage());
            return executeBasicRetrieval(queries.get(0), topK, threshold);
        }
    }
    
    private List<RetrievalResult> executeBasicRetrieval(String query, int topK, double threshold) {
        long startTime = System.currentTimeMillis();
        
        try {
            float[] embedding = embeddingService.embed(query);
            List<VectorSearchResult> searchResults = vectorStore.search(embedding, topK, null);
            
            List<RetrievalResult> results = searchResults.stream()
                    .filter(r -> r.getSimilarityScore() >= threshold)
                    .map(this::convertVectorToRetrieval)
                    .collect(Collectors.toList());
            
            long duration = System.currentTimeMillis() - startTime;
            if (metrics != null) {
                metrics.recordVectorSearch(duration, true);
            }
            
            return results;
            
        } catch (Exception e) {
            logger.error("Basic retrieval failed: {}", e.getMessage());
            if (metrics != null) {
                metrics.recordVectorSearch(System.currentTimeMillis() - startTime, false);
            }
            return Collections.emptyList();
        }
    }
    
    private List<RetrievalResult> rerankResults(String query, List<RetrievalResult> results, int topK) {
        if (reranker == null || results.isEmpty()) {
            return results;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> candidates = results.stream()
                    .map(RetrievalResult::getContent)
                    .collect(Collectors.toList());
            
            List<RerankResult> reranked = reranker.rerank(query, candidates, topK);
            
            // 根据重排序结果重新排列
            Map<String, RetrievalResult> contentToResult = results.stream()
                    .collect(Collectors.toMap(RetrievalResult::getContent, r -> r, (a, b) -> a));
            
            List<RetrievalResult> rerankedResults = reranked.stream()
                    .map(rr -> {
                        RetrievalResult original = contentToResult.get(rr.getContent());
                        if (original != null) {
                            // 更新分数
                            return RetrievalResult.builder()
                                    .documentId(original.getDocumentId())
                                    .content(original.getContent())
                                    .category(original.getCategory())
                                    .similarityScore(rr.getRelevanceScore())
                                    .metadata(original.getMetadata())
                                    .confidenceScore(rr.getRelevanceScore())
                                    .knowledgeSource(original.getKnowledgeSource())
                                    .build();
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            long duration = System.currentTimeMillis() - startTime;
            if (metrics != null) {
                metrics.recordRerank(duration, true);
            }
            
            logger.debug("Reranked {} results in {}ms", rerankedResults.size(), duration);
            return rerankedResults;
            
        } catch (Exception e) {
            logger.warn("Reranking failed, using original order: {}", e.getMessage());
            if (metrics != null) {
                metrics.recordRerank(System.currentTimeMillis() - startTime, false);
            }
            return results;
        }
    }
    
    private List<RetrievalResult> filterDiscussedDocuments(List<RetrievalResult> results, 
                                                          ConversationContext context) {
        if (context == null || results == null || results.isEmpty()) {
            return results;
        }
        
        // 只过滤在当前会话中最近5轮对话中讨论过的文档
        // 避免过度过滤导致无法检索到相关知识
        Set<String> recentlyDiscussedIds = context.getRecentlyDiscussedDocumentIds(5);
        
        if (recentlyDiscussedIds == null || recentlyDiscussedIds.isEmpty()) {
            logger.debug("No recently discussed documents, returning all {} results", results.size());
            return results;
        }
        
        List<RetrievalResult> filtered = results.stream()
                .filter(r -> {
                    String docId = r.getDocumentId();
                    boolean isDiscussed = recentlyDiscussedIds.contains(docId);
                    if (isDiscussed) {
                        logger.debug("Filtering out recently discussed document: {}", docId);
                    }
                    return !isDiscussed;
                })
                .collect(Collectors.toList());
        
        // 如果过滤后没有结果，返回原始结果的前几个（避免完全没有知识参考）
        if (filtered.isEmpty() && !results.isEmpty()) {
            logger.debug("All results were filtered, returning top 3 original results");
            return results.stream().limit(3).collect(Collectors.toList());
        }
        
        logger.debug("Filtered {} -> {} results", results.size(), filtered.size());
        return filtered;
    }
    
    private RetrievalResult convertHybridToRetrieval(HybridSearchResult hybrid) {
        return RetrievalResult.builder()
                .documentId(hybrid.getDocumentId())
                .content(hybrid.getContent())
                .category(hybrid.getCategory())
                .similarityScore(hybrid.getFusionScore())
                .metadata(null) // HybridSearchResult doesn't have metadata field
                .confidenceScore(hybrid.getFusionScore())
                .knowledgeSource("混合检索: " + hybrid.getSources())
                .build();
    }
    
    private RetrievalResult convertVectorToRetrieval(VectorSearchResult vector) {
        return RetrievalResult.builder()
                .documentId(vector.getDocumentId())
                .content(vector.getContent())
                .category(vector.getCategory())
                .similarityScore(vector.getSimilarityScore())
                .metadata(vector.getMetadata())
                .confidenceScore(vector.getSimilarityScore())
                .knowledgeSource("向量检索")
                .build();
    }
    
    private String formatKnowledgeEntry(RetrievalResult result, int index) {
        StringBuilder entry = new StringBuilder();
        entry.append("---\n");
        entry.append("知识").append(index).append("：\n");
        entry.append(result.getContent()).append("\n");
        entry.append("（相关度：").append(String.format("%.0f%%", result.getSimilarityScore() * 100));
        entry.append("）\n");
        return entry.toString();
    }
    
    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() <= 50 ? text : text.substring(0, 50) + "...";
    }
}
