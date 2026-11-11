package com.learning.silveragepass.dto.rag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 混合检索结果
 * Hybrid Search Result
 * 
 * 包含混合检索的融合结果，包括融合分数、来源标注和各检索方式的原始分数。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public class HybridSearchResult {
    
    /** 文档ID */
    private String documentId;
    
    /** 文档内容 */
    private String content;
    
    /** 文档类别 */
    private String category;
    
    /** 融合后的最终分数 */
    private double fusionScore;
    
    /** 向量检索分数（如果有） */
    private Double vectorScore;
    
    /** 关键词检索BM25分数（如果有） */
    private Double keywordScore;
    
    /** 检索来源集合 */
    private Set<SearchSource> sources;
    
    /** 高亮位置列表 */
    private List<HighlightPosition> highlights;
    
    /** 向量检索中的排名（用于RRF计算） */
    private Integer vectorRank;
    
    /** 关键词检索中的排名（用于RRF计算） */
    private Integer keywordRank;
    
    public HybridSearchResult() {
        this.sources = new HashSet<>();
        this.highlights = new ArrayList<>();
    }
    
    public static HybridSearchResult fromVectorResult(VectorSearchResult vectorResult, int rank) {
        HybridSearchResult result = new HybridSearchResult();
        result.setDocumentId(vectorResult.getDocumentId());
        result.setContent(vectorResult.getContent());
        result.setCategory(vectorResult.getCategory());
        result.setVectorScore(vectorResult.getSimilarityScore());
        result.setVectorRank(rank);
        result.getSources().add(SearchSource.VECTOR);
        return result;
    }
    
    public static HybridSearchResult fromKeywordResult(KeywordSearchResult keywordResult, int rank) {
        HybridSearchResult result = new HybridSearchResult();
        result.setDocumentId(keywordResult.getDocumentId());
        result.setContent(keywordResult.getContent());
        result.setCategory(keywordResult.getCategory());
        result.setKeywordScore(keywordResult.getBm25Score());
        result.setKeywordRank(rank);
        result.setHighlights(keywordResult.getHighlights());
        result.getSources().add(SearchSource.KEYWORD);
        return result;
    }
    
    /**
     * 合并另一个结果（用于去重时合并来源）
     */
    public void mergeWith(HybridSearchResult other) {
        this.sources.addAll(other.getSources());
        
        if (other.getVectorScore() != null && this.vectorScore == null) {
            this.vectorScore = other.getVectorScore();
            this.vectorRank = other.getVectorRank();
        }
        
        if (other.getKeywordScore() != null && this.keywordScore == null) {
            this.keywordScore = other.getKeywordScore();
            this.keywordRank = other.getKeywordRank();
            if (other.getHighlights() != null && !other.getHighlights().isEmpty()) {
                this.highlights = other.getHighlights();
            }
        }
        
        // 如果同时来自两个来源，标记为混合
        if (this.sources.contains(SearchSource.VECTOR) && this.sources.contains(SearchSource.KEYWORD)) {
            this.sources.add(SearchSource.HYBRID);
        }
    }
    
    /**
     * 获取主要来源（用于显示）
     */
    public SearchSource getPrimarySource() {
        if (sources.contains(SearchSource.HYBRID)) {
            return SearchSource.HYBRID;
        }
        if (sources.contains(SearchSource.VECTOR)) {
            return SearchSource.VECTOR;
        }
        if (sources.contains(SearchSource.KEYWORD)) {
            return SearchSource.KEYWORD;
        }
        return SearchSource.VECTOR;
    }

    // Getters and Setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public double getFusionScore() { return fusionScore; }
    public void setFusionScore(double fusionScore) { this.fusionScore = fusionScore; }
    
    public Double getVectorScore() { return vectorScore; }
    public void setVectorScore(Double vectorScore) { this.vectorScore = vectorScore; }
    
    public Double getKeywordScore() { return keywordScore; }
    public void setKeywordScore(Double keywordScore) { this.keywordScore = keywordScore; }
    
    public Set<SearchSource> getSources() { return sources; }
    public void setSources(Set<SearchSource> sources) { this.sources = sources; }
    
    public List<HighlightPosition> getHighlights() { return highlights; }
    public void setHighlights(List<HighlightPosition> highlights) { this.highlights = highlights; }
    
    public Integer getVectorRank() { return vectorRank; }
    public void setVectorRank(Integer vectorRank) { this.vectorRank = vectorRank; }
    
    public Integer getKeywordRank() { return keywordRank; }
    public void setKeywordRank(Integer keywordRank) { this.keywordRank = keywordRank; }
}
