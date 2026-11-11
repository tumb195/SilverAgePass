package com.learning.silveragepass.dto.rag;

/**
 * 医学实体
 * Medical Entity
 * 
 * 从文本中提取的医学相关实体，包括疾病、症状、药物等。
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public class MedicalEntity {
    
    /** 实体名称 */
    private String name;
    
    /** 实体类型 */
    private EntityType type;
    
    /** 在文本中的起始位置 */
    private int startPosition;
    
    /** 在文本中的结束位置 */
    private int endPosition;
    
    /** 置信度分数 */
    private double confidence;
    
    /** 标准化名称（如果有） */
    private String normalizedName;
    
    public MedicalEntity() {
        this.confidence = 1.0;
    }
    
    public MedicalEntity(String name, EntityType type) {
        this();
        this.name = name;
        this.type = type;
    }
    
    public MedicalEntity(String name, EntityType type, int startPosition, int endPosition) {
        this(name, type);
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public EntityType getType() { return type; }
    public void setType(EntityType type) { this.type = type; }
    
    public int getStartPosition() { return startPosition; }
    public void setStartPosition(int startPosition) { this.startPosition = startPosition; }
    
    public int getEndPosition() { return endPosition; }
    public void setEndPosition(int endPosition) { this.endPosition = endPosition; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
    
    @Override
    public String toString() {
        return String.format("MedicalEntity{name='%s', type=%s, confidence=%.2f}", 
            name, type, confidence);
    }
}
