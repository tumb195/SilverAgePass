package com.learning.silveragepass.service.rag;

import com.learning.silveragepass.dto.rag.KnowledgeDocument;
import com.learning.silveragepass.dto.rag.KeywordSearchResult;

import java.util.List;
import java.util.Map;

/**
 * 关键词检索引擎接口
 * Keyword Search Engine Interface
 * 
 * 基于BM25算法实现精确关键词匹配。
 * 支持中文分词和医学术语识别。
 * 
 * Requirements:
 * - 1.1: 使用BM25算法检索包含医学术语的文档
 * - 1.2: 返回按BM25分数降序排列的结果列表
 * - 1.3: 在结果中包含匹配的关键词高亮位置信息
 * - 1.4: 支持中文分词和医学术语识别
 * - 1.5: 支持可配置的最大返回数量（默认top-k=10）
 * - 1.6: 服务不可用时支持降级
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public interface KeywordSearchEngine {
    
    /**
     * 执行关键词检索
     * Execute keyword search
     * 
     * @param query 查询文本 (query text)
     * @param topK 返回数量 (number of results to return)
     * @param filter 过滤条件 (filter conditions, e.g., category)
     * @return 检索结果列表，按BM25分数降序排列 (search results sorted by BM25 score descending)
     */
    List<KeywordSearchResult> search(String query, int topK, Map<String, Object> filter);
    
    /**
     * 使用默认topK执行关键词检索
     * Execute keyword search with default topK
     * 
     * @param query 查询文本 (query text)
     * @return 检索结果列表 (search results)
     */
    default List<KeywordSearchResult> search(String query) {
        return search(query, 10, null);
    }
    
    /**
     * 索引文档
     * Index documents
     * 
     * @param documents 文档列表 (list of documents to index)
     */
    void indexDocuments(List<KnowledgeDocument> documents);
    
    /**
     * 索引单个文档
     * Index a single document
     * 
     * @param document 文档 (document to index)
     */
    default void indexDocument(KnowledgeDocument document) {
        indexDocuments(List.of(document));
    }
    
    /**
     * 删除文档索引
     * Delete document indices
     * 
     * @param documentIds 文档ID列表 (list of document IDs to delete)
     */
    void deleteDocuments(List<String> documentIds);
    
    /**
     * 删除单个文档索引
     * Delete a single document index
     * 
     * @param documentId 文档ID (document ID to delete)
     */
    default void deleteDocument(String documentId) {
        deleteDocuments(List.of(documentId));
    }
    
    /**
     * 检查服务是否可用
     * Check if the service is available
     * 
     * @return true if service is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * 获取索引中的文档数量
     * Get the number of documents in the index
     * 
     * @return 文档数量 (document count)
     */
    long getDocumentCount();
    
    /**
     * 刷新索引
     * Refresh the index to make recent changes searchable
     */
    void refreshIndex();
}
