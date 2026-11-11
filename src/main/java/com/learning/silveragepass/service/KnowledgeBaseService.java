package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.rag.KnowledgeDocument;
import com.learning.silveragepass.entity.KnowledgeDocumentEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 知识库管理服务接口
 * Knowledge Base Management Service Interface
 * 
 * 负责健康知识库的管理，包括：
 * - 知识导入（支持文本、JSON、Markdown格式）
 * - 知识分类管理
 * - 版本控制
 * - 删除和归档
 * - 统计信息查询
 * 
 * Requirements:
 * - 6.1: Import knowledge from multiple formats (text files, JSON, Markdown)
 * - 6.2: Support categorization (elderly_care, chronic_disease, medication, mental_health)
 * - 6.3: Support version control to track updates
 * - 6.4: Support deletion and archiving
 * - 6.5: When knowledge is updated, regenerate embeddings for affected documents
 * - 6.6: Provide statistics (document count, category distribution, update time)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public interface KnowledgeBaseService {

    // ==========================================
    // Knowledge Import Methods (Requirement 6.1)
    // ==========================================

    /**
     * 导入纯文本知识
     * Import knowledge from plain text
     * 
     * @param content 文本内容 (text content)
     * @param category 知识类别 (knowledge category)
     * @param sourceName 来源名称 (source name)
     * @return 创建的文档实体 (created document entity)
     */
    KnowledgeDocumentEntity importText(String content, String category, String sourceName);

    /**
     * 导入JSON格式知识
     * Import knowledge from JSON format
     * 
     * JSON格式应包含以下字段：
     * - content: 知识内容（必需）
     * - category: 类别（可选，默认使用参数中的category）
     * - metadata: 额外元数据（可选）
     * 
     * @param jsonContent JSON内容 (JSON content)
     * @param defaultCategory 默认类别 (default category)
     * @param sourceName 来源名称 (source name)
     * @return 创建的文档实体 (created document entity)
     */
    KnowledgeDocumentEntity importJson(String jsonContent, String defaultCategory, String sourceName);

    /**
     * 导入Markdown格式知识
     * Import knowledge from Markdown format
     * 
     * Markdown内容将被解析并存储，保留格式信息。
     * 
     * @param markdownContent Markdown内容 (Markdown content)
     * @param category 知识类别 (knowledge category)
     * @param sourceName 来源名称 (source name)
     * @return 创建的文档实体 (created document entity)
     */
    KnowledgeDocumentEntity importMarkdown(String markdownContent, String category, String sourceName);

    /**
     * 批量导入知识文档
     * Batch import knowledge documents
     * 
     * @param documents 文档列表 (list of documents)
     * @return 创建的文档实体列表 (list of created document entities)
     */
    List<KnowledgeDocumentEntity> importBatch(List<KnowledgeDocument> documents);

    // ==========================================
    // Knowledge Query Methods
    // ==========================================

    /**
     * 根据ID获取知识文档
     * Get knowledge document by ID
     * 
     * @param id 文档ID (document ID)
     * @return 文档实体（如果存在） (document entity if exists)
     */
    Optional<KnowledgeDocumentEntity> getById(String id);

    /**
     * 获取所有激活的知识文档
     * Get all active knowledge documents
     * 
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> getAllActive();

    /**
     * 根据类别获取知识文档
     * Get knowledge documents by category
     * 
     * Requirement 6.2: Support categorization
     * 
     * @param category 类别 (category)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> getByCategory(String category);

    /**
     * 根据关键词搜索知识文档（全文搜索回退）
     * Search knowledge documents by keyword (full-text search fallback)
     * 
     * @param keyword 关键词 (keyword)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> searchByKeyword(String keyword);

    /**
     * 根据关键词和类别搜索知识文档
     * Search knowledge documents by keyword and category
     * 
     * @param keyword 关键词 (keyword)
     * @param category 类别 (category)
     * @return 文档列表 (list of documents)
     */
    List<KnowledgeDocumentEntity> searchByKeywordAndCategory(String keyword, String category);

    // ==========================================
    // Knowledge Update Methods (Requirement 6.3, 6.5)
    // ==========================================

    /**
     * 更新知识文档内容
     * Update knowledge document content
     * 
     * 更新时将：
     * 1. 递增版本号
     * 2. 重新生成嵌入向量
     * 3. 更新向量数据库
     * 
     * Requirement 6.3: Support version control
     * Requirement 6.5: Regenerate embeddings when knowledge is updated
     * 
     * @param id 文档ID (document ID)
     * @param newContent 新内容 (new content)
     * @return 更新后的文档实体 (updated document entity)
     */
    KnowledgeDocumentEntity updateContent(String id, String newContent);

    /**
     * 更新知识文档类别
     * Update knowledge document category
     * 
     * @param id 文档ID (document ID)
     * @param newCategory 新类别 (new category)
     * @return 更新后的文档实体 (updated document entity)
     */
    KnowledgeDocumentEntity updateCategory(String id, String newCategory);

    /**
     * 更新知识文档元数据
     * Update knowledge document metadata
     * 
     * @param id 文档ID (document ID)
     * @param metadata 新元数据 (new metadata)
     * @return 更新后的文档实体 (updated document entity)
     */
    KnowledgeDocumentEntity updateMetadata(String id, Map<String, Object> metadata);

    // ==========================================
    // Knowledge Delete/Archive Methods (Requirement 6.4)
    // ==========================================

    /**
     * 归档知识文档（软删除）
     * Archive knowledge document (soft delete)
     * 
     * 归档后文档仍保留在数据库中，但不会被检索。
     * 
     * @param id 文档ID (document ID)
     * @return 归档后的文档实体 (archived document entity)
     */
    KnowledgeDocumentEntity archive(String id);

    /**
     * 批量归档知识文档
     * Batch archive knowledge documents
     * 
     * @param ids 文档ID列表 (list of document IDs)
     * @return 归档的文档数量 (number of archived documents)
     */
    int archiveBatch(List<String> ids);

    /**
     * 按类别归档知识文档
     * Archive knowledge documents by category
     * 
     * @param category 类别 (category)
     * @return 归档的文档数量 (number of archived documents)
     */
    int archiveByCategory(String category);

    /**
     * 恢复归档的知识文档
     * Restore archived knowledge document
     * 
     * @param id 文档ID (document ID)
     * @return 恢复后的文档实体 (restored document entity)
     */
    KnowledgeDocumentEntity restore(String id);

    /**
     * 永久删除知识文档（硬删除）
     * Permanently delete knowledge document (hard delete)
     * 
     * 警告：此操作不可逆，将同时删除MySQL和向量数据库中的数据。
     * 
     * @param id 文档ID (document ID)
     */
    void delete(String id);

    /**
     * 批量永久删除知识文档
     * Batch permanently delete knowledge documents
     * 
     * @param ids 文档ID列表 (list of document IDs)
     */
    void deleteBatch(List<String> ids);

    /**
     * 清理过期的归档文档
     * Clean up expired archived documents
     * 
     * @param daysOld 归档天数阈值 (days threshold)
     * @return 删除的文档数量 (number of deleted documents)
     */
    int cleanupArchivedDocuments(int daysOld);

    // ==========================================
    // Statistics Methods (Requirement 6.6)
    // ==========================================

    /**
     * 获取知识库统计信息
     * Get knowledge base statistics
     * 
     * 返回的统计信息包括：
     * - totalDocuments: 总文档数
     * - activeDocuments: 激活文档数
     * - archivedDocuments: 归档文档数
     * - categoryDistribution: 类别分布
     * - sourceTypeDistribution: 来源类型分布
     * - latestUpdateTime: 最近更新时间
     * - latestCreationTime: 最近创建时间
     * 
     * @return 统计信息 (statistics)
     */
    KnowledgeBaseStatistics getStatistics();

    /**
     * 获取指定类别的文档数量
     * Get document count by category
     * 
     * @param category 类别 (category)
     * @return 文档数量 (document count)
     */
    long getCountByCategory(String category);

    /**
     * 检查知识库服务是否可用
     * Check if knowledge base service is available
     * 
     * @return true如果服务可用 (true if service is available)
     */
    boolean isAvailable();

    // ==========================================
    // Sync Methods
    // ==========================================

    /**
     * 同步MySQL和向量数据库
     * Synchronize MySQL and vector database
     * 
     * 用于修复数据不一致的情况。
     * 
     * @return 同步的文档数量 (number of synchronized documents)
     */
    int syncWithVectorStore();

    /**
     * 重建所有文档的嵌入向量
     * Rebuild embeddings for all documents
     * 
     * 用于嵌入模型更新后重新生成向量。
     * 
     * @return 重建的文档数量 (number of rebuilt documents)
     */
    int rebuildAllEmbeddings();

    // ==========================================
    // Chunking Methods (分块相关方法)
    // ==========================================

    /**
     * 导入Markdown并自动分块
     * Import Markdown with automatic chunking
     * 
     * @param markdownContent Markdown内容
     * @param category 知识类别
     * @param sourceName 来源名称
     * @return 创建的文档实体（包含分块信息）
     */
    KnowledgeDocumentEntity importMarkdownWithChunking(String markdownContent, String category, String sourceName);

    /**
     * 获取文档的所有分块
     * Get all chunks of a document
     * 
     * @param documentId 文档ID
     * @return 分块列表
     */
    List<com.learning.silveragepass.dto.rag.DocumentChunk> getChunksByDocumentId(String documentId);

    /**
     * 获取分块的上下文（前后N个分块）
     * Get context chunks around a specific chunk
     * 
     * @param chunkId 分块ID
     * @param contextSize 上下文大小（前后各取多少个）
     * @return 上下文分块列表
     */
    List<com.learning.silveragepass.dto.rag.DocumentChunk> getChunkContext(String chunkId, int contextSize);

    /**
     * 重建文档的分块
     * Rebuild chunks for a document
     * 
     * @param documentId 文档ID
     * @return 新的分块数量
     */
    int rebuildChunks(String documentId);

    /**
     * 重建所有文档的分块
     * Rebuild chunks for all documents
     * 
     * @return 处理的文档数量
     */
    int rebuildAllChunks();

    // ==========================================
    // Inner Classes
    // ==========================================

    /**
     * 知识库统计信息
     * Knowledge Base Statistics
     */
    record KnowledgeBaseStatistics(
            long totalDocuments,
            long activeDocuments,
            long archivedDocuments,
            Map<String, Long> categoryDistribution,
            Map<String, Long> sourceTypeDistribution,
            String latestUpdateTime,
            String latestCreationTime
    ) {
        /**
         * 创建空的统计信息
         * Create empty statistics
         */
        public static KnowledgeBaseStatistics empty() {
            return new KnowledgeBaseStatistics(
                    0, 0, 0,
                    Map.of(), Map.of(),
                    null, null
            );
        }
    }
}
