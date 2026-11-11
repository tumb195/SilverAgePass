package com.learning.silveragepass.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * RAG检索结果DTO
 * RAG Retrieval Result DTO
 * 
 * 封装从知识库检索到的文档信息，用于RAG增强生成。
 * 与VectorSearchResult类似，但专注于RAG检索场景，
 * 包含额外的知识来源和置信度信息。
 * 
 * Requirements:
 * - 1.6: Knowledge retriever includes knowledge source and confidence score in retrieval results
 * - 2.1: Vector database stores knowledge embeddings with metadata (source, category, timestamp, version)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {
    
    /**
     * 文档唯一标识符
     * Document unique identifier
     */
    private String documentId;
    
    /**
     * 文档内容
     * Document content
     * 
     * 检索到的知识文档的完整内容
     */
    private String content;
    
    /**
     * 文档类别
     * Document category
     * 
     * 支持的类别：
     * - elderly_care: 老年护理
     * - chronic_disease: 慢病管理
     * - medication: 用药指导
     * - mental_health: 心理健康
     */
    private String category;
    
    /**
     * 相似度分数
     * Similarity score
     * 
     * 使用余弦相似度计算，范围 [0, 1]
     * 分数越高表示与查询越相关
     * 
     * Requirement 1.4: Knowledge retriever supports configurable similarity threshold (default 0.7)
     */
    private double similarityScore;
    
    /**
     * 文档元数据
     * Document metadata
     * 
     * 包含：
     * - source: 来源类型（file, api, manual）
     * - sourceName: 来源名称
     * - timestamp: 创建/更新时间戳
     * - version: 文档版本
     * - author: 作者（可选）
     * - tags: 标签列表（可选）
     * 
     * Requirement 1.6: Retrieval results include knowledge source and confidence score
     */
    private Map<String, Object> metadata;
    
    /**
     * 知识来源描述
     * Knowledge source description
     * 
     * 用于在回答中标注知识来源
     * Requirement 9.5: Health assistant marks source when providing knowledge base information
     */
    private String knowledgeSource;
    
    /**
     * 置信度分数
     * Confidence score
     * 
     * 表示检索结果的可信程度，范围 [0, 1]
     * 综合考虑相似度、来源可靠性等因素
     * 
     * Requirement 1.6: Retrieval results include confidence score
     */
    private double confidenceScore;
    
    /**
     * 获取来源类型
     * Get source type
     * 
     * @return 来源类型 (source type)
     */
    public String getSourceType() {
        if (metadata == null) {
            return null;
        }
        Object source = metadata.get("source");
        return source != null ? source.toString() : null;
    }
    
    /**
     * 获取来源名称
     * Get source name
     * 
     * @return 来源名称 (source name)
     */
    public String getSourceName() {
        if (metadata == null) {
            return null;
        }
        Object sourceName = metadata.get("sourceName");
        return sourceName != null ? sourceName.toString() : null;
    }
    
    /**
     * 获取文档版本
     * Get document version
     * 
     * @return 文档版本 (document version)
     */
    public Integer getVersion() {
        if (metadata == null) {
            return null;
        }
        Object version = metadata.get("version");
        if (version instanceof Number) {
            return ((Number) version).intValue();
        }
        return null;
    }
    
    /**
     * 获取时间戳
     * Get timestamp
     * 
     * @return 时间戳字符串 (timestamp string)
     */
    public String getTimestamp() {
        if (metadata == null) {
            return null;
        }
        Object timestamp = metadata.get("timestamp");
        return timestamp != null ? timestamp.toString() : null;
    }
    
    /**
     * 检查相似度是否高于阈值
     * Check if similarity is above threshold
     * 
     * @param threshold 相似度阈值 (similarity threshold)
     * @return true如果相似度高于阈值 (true if similarity is above threshold)
     */
    public boolean isAboveThreshold(double threshold) {
        return similarityScore >= threshold;
    }
    
    /**
     * 检查置信度是否足够高
     * Check if confidence is high enough
     * 
     * @param minConfidence 最小置信度 (minimum confidence)
     * @return true如果置信度足够高 (true if confidence is high enough)
     */
    public boolean isConfident(double minConfidence) {
        return confidenceScore >= minConfidence;
    }
    
    /**
     * 获取格式化的来源引用
     * Get formatted source citation
     * 
     * 用于在回答中标注知识来源
     * 
     * @return 格式化的来源引用 (formatted source citation)
     */
    public String getFormattedCitation() {
        if (knowledgeSource != null && !knowledgeSource.isEmpty()) {
            return knowledgeSource;
        }
        
        String sourceName = getSourceName();
        String sourceType = getSourceType();
        
        if (sourceName != null && !sourceName.isEmpty()) {
            return String.format("来源：%s", sourceName);
        } else if (sourceType != null && !sourceType.isEmpty()) {
            return String.format("来源：%s", sourceType);
        } else if (category != null && !category.isEmpty()) {
            return String.format("来源：%s知识库", getCategoryDisplayName());
        }
        
        return "来源：健康知识库";
    }
    
    /**
     * 获取类别显示名称
     * Get category display name
     * 
     * @return 类别的中文显示名称 (Chinese display name of category)
     */
    public String getCategoryDisplayName() {
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
     * 从VectorSearchResult创建RetrievalResult
     * Create RetrievalResult from VectorSearchResult
     * 
     * @param searchResult 向量搜索结果 (vector search result)
     * @return 检索结果实例 (retrieval result instance)
     */
    public static RetrievalResult fromVectorSearchResult(VectorSearchResult searchResult) {
        return RetrievalResult.builder()
                .documentId(searchResult.getDocumentId())
                .content(searchResult.getContent())
                .category(searchResult.getCategory())
                .similarityScore(searchResult.getSimilarityScore())
                .metadata(searchResult.getMetadata())
                .confidenceScore(searchResult.getSimilarityScore()) // 默认使用相似度作为置信度
                .build();
    }
    
    /**
     * 从VectorSearchResult创建RetrievalResult，带自定义置信度
     * Create RetrievalResult from VectorSearchResult with custom confidence
     * 
     * @param searchResult 向量搜索结果 (vector search result)
     * @param confidenceScore 置信度分数 (confidence score)
     * @return 检索结果实例 (retrieval result instance)
     */
    public static RetrievalResult fromVectorSearchResult(VectorSearchResult searchResult, 
                                                         double confidenceScore) {
        return RetrievalResult.builder()
                .documentId(searchResult.getDocumentId())
                .content(searchResult.getContent())
                .category(searchResult.getCategory())
                .similarityScore(searchResult.getSimilarityScore())
                .metadata(searchResult.getMetadata())
                .confidenceScore(confidenceScore)
                .build();
    }
    
    /**
     * 创建带有基本信息的检索结果
     * Create retrieval result with basic information
     * 
     * @param documentId 文档ID (document ID)
     * @param content 内容 (content)
     * @param category 类别 (category)
     * @param similarityScore 相似度分数 (similarity score)
     * @return 检索结果实例 (retrieval result instance)
     */
    public static RetrievalResult of(String documentId, String content, 
                                     String category, double similarityScore) {
        return RetrievalResult.builder()
                .documentId(documentId)
                .content(content)
                .category(category)
                .similarityScore(similarityScore)
                .confidenceScore(similarityScore)
                .build();
    }
    
    /**
     * 创建带有完整信息的检索结果
     * Create retrieval result with full information
     * 
     * @param documentId 文档ID (document ID)
     * @param content 内容 (content)
     * @param category 类别 (category)
     * @param similarityScore 相似度分数 (similarity score)
     * @param metadata 元数据 (metadata)
     * @param knowledgeSource 知识来源 (knowledge source)
     * @param confidenceScore 置信度分数 (confidence score)
     * @return 检索结果实例 (retrieval result instance)
     */
    public static RetrievalResult of(String documentId, String content, 
                                     String category, double similarityScore,
                                     Map<String, Object> metadata,
                                     String knowledgeSource, double confidenceScore) {
        return RetrievalResult.builder()
                .documentId(documentId)
                .content(content)
                .category(category)
                .similarityScore(similarityScore)
                .metadata(metadata)
                .knowledgeSource(knowledgeSource)
                .confidenceScore(confidenceScore)
                .build();
    }
}
