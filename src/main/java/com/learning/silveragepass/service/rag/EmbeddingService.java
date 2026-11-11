package com.learning.silveragepass.service.rag;

import java.util.List;

/**
 * 嵌入服务接口
 * Embedding Service Interface
 * 
 * 负责文本向量化，将文本转换为向量嵌入用于语义搜索。
 * Responsible for text vectorization, converting text to vector embeddings for semantic search.
 * 
 * 支持的功能：
 * - 单个文本嵌入生成
 * - 批量文本嵌入生成
 * - 嵌入缓存机制
 * - 中文文本处理（老年用户主要语言）
 * 
 * Requirements:
 * - 3.1: Embedding model converts text queries to vector embeddings
 * - 3.2: Embedding model converts documents to vector embeddings during ingestion
 * - 3.3: Embedding model supports Chinese text processing (primary language for elderly users)
 * - 3.5: Embedding model supports batch embedding generation for efficiency
 * - 11.1: RAG system caches common knowledge embeddings to reduce computation
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public interface EmbeddingService {
    
    /**
     * 生成单个文本的嵌入向量
     * Generate embedding vector for a single text
     * 
     * @param text 文本内容 (text content)
     * @return 嵌入向量 (embedding vector)
     * @throws EmbeddingException 当嵌入生成失败时抛出 (thrown when embedding generation fails)
     */
    float[] embed(String text);
    
    /**
     * 批量生成嵌入向量
     * Generate embedding vectors for multiple texts in batch
     * 
     * 批量处理可以提高效率，减少API调用次数。
     * Batch processing improves efficiency by reducing API calls.
     * 
     * @param texts 文本列表 (list of texts)
     * @return 嵌入向量列表，顺序与输入文本对应 (list of embedding vectors, in same order as input texts)
     * @throws EmbeddingException 当嵌入生成失败时抛出 (thrown when embedding generation fails)
     */
    List<float[]> embedBatch(List<String> texts);
    
    /**
     * 获取嵌入维度
     * Get the embedding dimension
     * 
     * 不同的嵌入模型有不同的向量维度：
     * - BGE-M3: 1024
     * - OpenAI text-embedding-3-small: 1536
     * 
     * @return 向量维度 (vector dimension)
     */
    int getDimension();
    
    /**
     * 检查嵌入服务是否可用
     * Check if the embedding service is available
     * 
     * 用于健康检查和回退机制判断。
     * Used for health check and fallback mechanism decisions.
     * 
     * @return true如果服务可用，false如果不可用 (true if service is available, false otherwise)
     */
    boolean isAvailable();
    
    /**
     * 获取嵌入模型名称
     * Get the embedding model name
     * 
     * @return 模型名称 (model name)
     */
    String getModelName();
    
    /**
     * 清除嵌入缓存
     * Clear the embedding cache
     * 
     * 用于缓存管理和测试场景。
     * Used for cache management and testing scenarios.
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
