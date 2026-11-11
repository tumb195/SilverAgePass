package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.dto.rag.RetrievalResult;

import java.util.List;

/**
 * RAG检索服务接口
 * RAG Retrieval Service Interface
 * 
 * 负责从知识库检索相关文档并增强LLM上下文，提供：
 * - 语义检索（支持top-k和相似度阈值配置）
 * - 提示词增强（将检索到的知识融入提示词）
 * - 服务健康检查
 * - 检索结果缓存
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
public interface RAGService {
    
    /**
     * 检索与查询相关的知识文档
     * Retrieve knowledge documents relevant to the query
     * 
     * 使用语义搜索从知识库检索相关文档，支持：
     * - 配置检索数量（top-k）
     * - 配置相似度阈值
     * - 对话上下文增强查询
     * - 过滤已讨论的文档
     * 
     * Requirement 1.1: Health assistant retrieves relevant knowledge documents
     * Requirement 1.3: Knowledge retriever supports configurable retrieval count
     * Requirement 1.4: Knowledge retriever supports configurable similarity threshold
     * 
     * @param query 用户查询 (user query)
     * @param context 对话上下文，可为null (conversation context, can be null)
     * @param topK 返回文档数量 (number of documents to return)
     * @param threshold 相似度阈值 (similarity threshold)
     * @return 检索结果列表，按相似度降序排列 (list of retrieval results, sorted by similarity descending)
     */
    List<RetrievalResult> retrieve(String query, ConversationContext context, 
                                   int topK, double threshold);
    
    /**
     * 使用默认配置检索知识文档
     * Retrieve knowledge documents with default configuration
     * 
     * 使用配置文件中的默认top-k和阈值设置。
     * Uses default top-k and threshold settings from configuration.
     * 
     * @param query 用户查询 (user query)
     * @param context 对话上下文，可为null (conversation context, can be null)
     * @return 检索结果列表 (list of retrieval results)
     */
    List<RetrievalResult> retrieve(String query, ConversationContext context);
    
    /**
     * 按类别检索知识文档
     * Retrieve knowledge documents by category
     * 
     * @param query 用户查询 (user query)
     * @param context 对话上下文，可为null (conversation context, can be null)
     * @param category 知识类别 (knowledge category)
     * @param topK 返回文档数量 (number of documents to return)
     * @param threshold 相似度阈值 (similarity threshold)
     * @return 检索结果列表 (list of retrieval results)
     */
    List<RetrievalResult> retrieveByCategory(String query, ConversationContext context,
                                             String category, int topK, double threshold);
    
    /**
     * 增强提示词，融入检索到的知识
     * Augment prompt by incorporating retrieved knowledge
     * 
     * 将检索到的知识文档内容融入原始提示词，生成增强后的提示词。
     * 增强后的提示词包含：
     * - 原始用户查询
     * - 相关知识内容
     * - 知识来源标注
     * 
     * Requirement 1.2: Health assistant incorporates knowledge content into response context
     * 
     * @param originalPrompt 原始提示词 (original prompt)
     * @param retrievalResults 检索结果列表 (list of retrieval results)
     * @return 增强后的提示词 (augmented prompt)
     */
    String augmentPrompt(String originalPrompt, List<RetrievalResult> retrievalResults);
    
    /**
     * 增强提示词，带上下文长度限制
     * Augment prompt with context length limit
     * 
     * @param originalPrompt 原始提示词 (original prompt)
     * @param retrievalResults 检索结果列表 (list of retrieval results)
     * @param maxContextLength 最大上下文长度 (maximum context length)
     * @return 增强后的提示词 (augmented prompt)
     */
    String augmentPrompt(String originalPrompt, List<RetrievalResult> retrievalResults, 
                         int maxContextLength);
    
    /**
     * 检查RAG服务是否可用
     * Check if RAG service is available
     * 
     * 检查嵌入服务和向量存储是否都可用。
     * Checks if both embedding service and vector store are available.
     * 
     * @return true如果服务可用 (true if service is available)
     */
    boolean isAvailable();
    
    /**
     * 检查RAG功能是否启用
     * Check if RAG functionality is enabled
     * 
     * @return true如果RAG功能已启用 (true if RAG functionality is enabled)
     */
    boolean isEnabled();
    
    /**
     * 清除检索结果缓存
     * Clear retrieval result cache
     */
    void clearCache();
    
    /**
     * 获取缓存统计信息
     * Get cache statistics
     * 
     * @return 缓存统计信息 (cache statistics)
     */
    CacheStats getCacheStats();
    
    /**
     * 缓存统计信息
     * Cache statistics
     */
    record CacheStats(
            long hitCount,
            long missCount,
            long size,
            long maxSize,
            double hitRate
    ) {
        /**
         * 创建空的缓存统计
         * Create empty cache statistics
         */
        public static CacheStats empty() {
            return new CacheStats(0, 0, 0, 0, 0.0);
        }
    }
}
