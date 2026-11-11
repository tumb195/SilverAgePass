package com.learning.silveragepass.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 向量搜索结果DTO
 * Vector Search Result DTO
 * 
 * 封装从向量数据库检索到的文档信息，包括：
 * - 文档ID和内容
 * - 类别信息
 * - 相似度分数
 * - 元数据（来源、时间戳、版本等）
 * 
 * Requirements:
 * - 2.1: Vector database stores knowledge embeddings with metadata (source, category, timestamp, version)
 * - 2.2: Vector database supports semantic search using cosine similarity
 * - 2.5: Similarity search returns results sorted by relevance score
 * - 2.6: Vector database supports filtering by category
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchResult {
    
    /**
     * 文档唯一标识符
     * Document unique identifier
     */
    private String documentId;
    
    /**
     * 文档内容
     * Document content
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
     * Requirement 2.2: Vector database supports semantic search using cosine similarity
     */
    private double similarityScore;
    
    /**
     * 文档元数据
     * Document metadata
     * 
     * 包含：
     * - source: 来源（file, api, manual）
     * - sourceName: 来源名称
     * - timestamp: 创建/更新时间戳
     * - version: 文档版本
     * 
     * Requirement 2.1: Vector database stores knowledge embeddings with metadata
     */
    private Map<String, Object> metadata;
    
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
     * 创建带有基本信息的搜索结果
     * Create search result with basic information
     * 
     * @param documentId 文档ID (document ID)
     * @param content 内容 (content)
     * @param category 类别 (category)
     * @param similarityScore 相似度分数 (similarity score)
     * @return 搜索结果实例 (search result instance)
     */
    public static VectorSearchResult of(String documentId, String content, 
                                        String category, double similarityScore) {
        return VectorSearchResult.builder()
                .documentId(documentId)
                .content(content)
                .category(category)
                .similarityScore(similarityScore)
                .build();
    }
    
    /**
     * 创建带有完整信息的搜索结果
     * Create search result with full information
     * 
     * @param documentId 文档ID (document ID)
     * @param content 内容 (content)
     * @param category 类别 (category)
     * @param similarityScore 相似度分数 (similarity score)
     * @param metadata 元数据 (metadata)
     * @return 搜索结果实例 (search result instance)
     */
    public static VectorSearchResult of(String documentId, String content, 
                                        String category, double similarityScore,
                                        Map<String, Object> metadata) {
        return VectorSearchResult.builder()
                .documentId(documentId)
                .content(content)
                .category(category)
                .similarityScore(similarityScore)
                .metadata(metadata)
                .build();
    }
}
