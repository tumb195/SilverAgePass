package com.learning.silveragepass.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 关键词检索结果
 * Keyword Search Result
 * 
 * 包含BM25分数和高亮位置信息的检索结果。
 * 
 * Requirements:
 * - 1.2: 返回按BM25分数降序排列的结果列表
 * - 1.3: 在结果中包含匹配的关键词高亮位置信息
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordSearchResult {
    
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
     */
    private String category;
    
    /**
     * BM25分数
     * BM25 score
     * 
     * 表示查询与文档的相关性分数，分数越高表示越相关。
     */
    private double bm25Score;
    
    /**
     * 高亮位置列表
     * List of highlight positions
     * 
     * 标记文档中匹配查询关键词的位置。
     */
    private List<HighlightPosition> highlights;
    
    /**
     * 额外元数据
     * Additional metadata
     */
    private Map<String, Object> metadata;
    
    /**
     * 来源名称
     * Source name
     */
    private String sourceName;
    
    /**
     * 文档版本
     * Document version
     */
    private Integer version;
    
    /**
     * 创建带基本信息的检索结果
     * Create search result with basic information
     * 
     * @param documentId 文档ID
     * @param content 内容
     * @param bm25Score BM25分数
     * @return 检索结果实例
     */
    public static KeywordSearchResult of(String documentId, String content, double bm25Score) {
        return KeywordSearchResult.builder()
                .documentId(documentId)
                .content(content)
                .bm25Score(bm25Score)
                .build();
    }
    
    /**
     * 创建带高亮信息的检索结果
     * Create search result with highlight information
     * 
     * @param documentId 文档ID
     * @param content 内容
     * @param bm25Score BM25分数
     * @param highlights 高亮位置列表
     * @return 检索结果实例
     */
    public static KeywordSearchResult of(String documentId, String content, double bm25Score,
                                         List<HighlightPosition> highlights) {
        return KeywordSearchResult.builder()
                .documentId(documentId)
                .content(content)
                .bm25Score(bm25Score)
                .highlights(highlights)
                .build();
    }
}
