package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.KnowledgeDocument;
import com.learning.silveragepass.dto.rag.VectorSearchResult;

import java.util.List;
import java.util.Map;

/**
 * 向量存储接口
 * Vector Store Interface
 * 
 * 封装与Chroma向量数据库的交互，提供：
 * - 文档添加（支持批量）
 * - 语义搜索（支持类别过滤）
 * - 文档删除
 * - 文档更新
 * 
 * Requirements:
 * - 2.1: Vector database stores knowledge embeddings with metadata (source, category, timestamp, version)
 * - 2.2: Vector database supports semantic search using cosine similarity
 * - 2.3: Vector database supports batch insert of knowledge documents
 * - 2.4: Vector database supports update and delete operations
 * - 2.5: Similarity search returns results sorted by relevance score
 * - 2.6: Vector database supports filtering by category
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public interface VectorStore {
    
    /**
     * 添加文档到向量库
     * Add documents to vector store
     * 
     * 支持批量插入，每个文档将：
     * 1. 生成嵌入向量（如果未提供）
     * 2. 存储内容和元数据
     * 3. 建立向量索引
     * 
     * Requirement 2.3: Vector database supports batch insert of knowledge documents
     * 
     * @param documents 文档列表 (list of documents)
     * @throws VectorStoreException 当存储操作失败时抛出 (thrown when storage operation fails)
     */
    void addDocuments(List<KnowledgeDocument> documents);
    
    /**
     * 添加单个文档到向量库
     * Add a single document to vector store
     * 
     * @param document 文档 (document)
     * @throws VectorStoreException 当存储操作失败时抛出 (thrown when storage operation fails)
     */
    default void addDocument(KnowledgeDocument document) {
        addDocuments(List.of(document));
    }
    
    /**
     * 语义搜索
     * Semantic search
     * 
     * 使用余弦相似度进行语义搜索，返回按相关性分数排序的结果。
     * 
     * Requirement 2.2: Vector database supports semantic search using cosine similarity
     * Requirement 2.5: Similarity search returns results sorted by relevance score
     * 
     * @param queryEmbedding 查询向量 (query embedding vector)
     * @param topK 返回数量 (number of results to return)
     * @param filter 过滤条件，支持按类别过滤 (filter conditions, supports category filtering)
     * @return 搜索结果列表，按相似度降序排列 (list of search results, sorted by similarity descending)
     * @throws VectorStoreException 当搜索操作失败时抛出 (thrown when search operation fails)
     */
    List<VectorSearchResult> search(float[] queryEmbedding, int topK, Map<String, Object> filter);
    
    /**
     * 语义搜索（无过滤）
     * Semantic search without filter
     * 
     * @param queryEmbedding 查询向量 (query embedding vector)
     * @param topK 返回数量 (number of results to return)
     * @return 搜索结果列表 (list of search results)
     * @throws VectorStoreException 当搜索操作失败时抛出 (thrown when search operation fails)
     */
    default List<VectorSearchResult> search(float[] queryEmbedding, int topK) {
        return search(queryEmbedding, topK, null);
    }
    
    /**
     * 按类别搜索
     * Search by category
     * 
     * Requirement 2.6: Vector database supports filtering by category
     * 
     * @param queryEmbedding 查询向量 (query embedding vector)
     * @param topK 返回数量 (number of results to return)
     * @param category 类别过滤 (category filter)
     * @return 搜索结果列表 (list of search results)
     * @throws VectorStoreException 当搜索操作失败时抛出 (thrown when search operation fails)
     */
    default List<VectorSearchResult> searchByCategory(float[] queryEmbedding, int topK, String category) {
        return search(queryEmbedding, topK, Map.of("category", category));
    }
    
    /**
     * 删除文档
     * Delete documents
     * 
     * Requirement 2.4: Vector database supports update and delete operations
     * 
     * @param documentIds 文档ID列表 (list of document IDs)
     * @throws VectorStoreException 当删除操作失败时抛出 (thrown when delete operation fails)
     */
    void deleteDocuments(List<String> documentIds);
    
    /**
     * 删除单个文档
     * Delete a single document
     * 
     * @param documentId 文档ID (document ID)
     * @throws VectorStoreException 当删除操作失败时抛出 (thrown when delete operation fails)
     */
    default void deleteDocument(String documentId) {
        deleteDocuments(List.of(documentId));
    }
    
    /**
     * 更新文档
     * Update document
     * 
     * 更新操作将：
     * 1. 删除旧文档
     * 2. 重新生成嵌入向量
     * 3. 存储新文档
     * 
     * Requirement 2.4: Vector database supports update and delete operations
     * 
     * @param document 文档 (document)
     * @throws VectorStoreException 当更新操作失败时抛出 (thrown when update operation fails)
     */
    void updateDocument(KnowledgeDocument document);
    
    /**
     * 检查向量存储是否可用
     * Check if vector store is available
     * 
     * 用于健康检查和回退机制判断。
     * 
     * @return true如果服务可用 (true if service is available)
     */
    boolean isAvailable();
    
    /**
     * 获取集合中的文档数量
     * Get the number of documents in the collection
     * 
     * @return 文档数量 (document count)
     * @throws VectorStoreException 当操作失败时抛出 (thrown when operation fails)
     */
    long getDocumentCount();
    
    /**
     * 按ID获取文档
     * Get document by ID
     * 
     * @param documentId 文档ID (document ID)
     * @return 文档内容，如果不存在返回null (document content, null if not exists)
     * @throws VectorStoreException 当操作失败时抛出 (thrown when operation fails)
     */
    KnowledgeDocument getDocument(String documentId);
    
    /**
     * 检查文档是否存在
     * Check if document exists
     * 
     * @param documentId 文档ID (document ID)
     * @return true如果文档存在 (true if document exists)
     */
    default boolean documentExists(String documentId) {
        return getDocument(documentId) != null;
    }
    
    /**
     * 清空集合
     * Clear the collection
     * 
     * 警告：此操作将删除所有文档，请谨慎使用。
     * Warning: This operation will delete all documents, use with caution.
     * 
     * @throws VectorStoreException 当操作失败时抛出 (thrown when operation fails)
     */
    void clear();
    
    /**
     * 获取集合名称
     * Get collection name
     * 
     * @return 集合名称 (collection name)
     */
    String getCollectionName();
}
