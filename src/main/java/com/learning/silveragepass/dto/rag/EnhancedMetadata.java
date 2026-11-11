package com.learning.silveragepass.dto.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 增强元数据
 * Enhanced Metadata
 * 
 * 包含从文档中提取的关键词、摘要、医学实体等增强信息。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public class EnhancedMetadata {
    
    /** 文档ID */
    private String documentId;
    
    /** 提取的关键词列表 */
    private List<String> keywords;
    
    /** 自动生成的摘要 */
    private String summary;
    
    /** 提取的医学实体 */
    private List<MedicalEntity> medicalEntities;
    
    /** 主题分类 */
    private String category;
    
    /** 置信度分数 */
    private double confidence;
    
    public EnhancedMetadata() {
        this.keywords = new ArrayList<>();
        this.medicalEntities = new ArrayList<>();
        this.confidence = 1.0;
    }
    
    public EnhancedMetadata(String documentId) {
        this();
        this.documentId = documentId;
    }

    // Getters and Setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public List<MedicalEntity> getMedicalEntities() { return medicalEntities; }
    public void setMedicalEntities(List<MedicalEntity> medicalEntities) { this.medicalEntities = medicalEntities; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.isEmpty() && !keywords.contains(keyword)) {
            keywords.add(keyword);
        }
    }
    
    public void addMedicalEntity(MedicalEntity entity) {
        if (entity != null) {
            medicalEntities.add(entity);
        }
    }
}
