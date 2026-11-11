package com.learning.silveragepass.dto.rag;

/**
 * 重排序结果
 * Rerank Result
 * 
 * 包含重排序后的文档信息和相关性分数。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public class RerankResult {
    
    /** 文档ID */
    private String documentId;
    
    /** 文档内容 */
    private String content;
    
    /** 文档类别 */
    private String category;
    
    /** 重排序相关性分数 (0-1) */
    private double relevanceScore;
    
    /** 原始排名 */
    private int originalRank;
    
    /** 重排序后排名 */
    private int newRank;
    
    /** 原始检索分数 */
    private Double originalScore;
    
    public RerankResult() {}
    
    public RerankResult(String documentId, String content, double relevanceScore) {
        this.documentId = documentId;
        this.content = content;
        this.relevanceScore = relevanceScore;
    }
    
    public static RerankResult from(HybridSearchResult hybridResult, int originalRank) {
        RerankResult result = new RerankResult();
        result.setDocumentId(hybridResult.getDocumentId());
        result.setContent(hybridResult.getContent());
        result.setCategory(hybridResult.getCategory());
        result.setOriginalRank(originalRank);
        result.setOriginalScore(hybridResult.getFusionScore());
        return result;
    }
    
    public static RerankResult from(VectorSearchResult vectorResult, int originalRank) {
        RerankResult result = new RerankResult();
        result.setDocumentId(vectorResult.getDocumentId());
        result.setContent(vectorResult.getContent());
        result.setCategory(vectorResult.getCategory());
        result.setOriginalRank(originalRank);
        result.setOriginalScore(vectorResult.getSimilarityScore());
        return result;
    }

    // Getters and Setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
    
    public int getOriginalRank() { return originalRank; }
    public void setOriginalRank(int originalRank) { this.originalRank = originalRank; }
    
    public int getNewRank() { return newRank; }
    public void setNewRank(int newRank) { this.newRank = newRank; }
    
    public Double getOriginalScore() { return originalScore; }
    public void setOriginalScore(Double originalScore) { this.originalScore = originalScore; }
}
