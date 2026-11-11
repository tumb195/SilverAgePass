package com.learning.silveragepass.dto.rag;

/**
 * 相关实体（带关系信息）
 * Related Entity with Relation Info
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public class RelatedEntity {
    
    private HealthEntity entity;
    private RelationType relationType;
    private double relationWeight;
    private int hops; // 跳数（距离原始实体的步数）
    
    public RelatedEntity() {}
    
    public RelatedEntity(HealthEntity entity, RelationType relationType, double relationWeight, int hops) {
        this.entity = entity;
        this.relationType = relationType;
        this.relationWeight = relationWeight;
        this.hops = hops;
    }

    // Getters and Setters
    public HealthEntity getEntity() { return entity; }
    public void setEntity(HealthEntity entity) { this.entity = entity; }
    
    public RelationType getRelationType() { return relationType; }
    public void setRelationType(RelationType relationType) { this.relationType = relationType; }
    
    public double getRelationWeight() { return relationWeight; }
    public void setRelationWeight(double relationWeight) { this.relationWeight = relationWeight; }
    
    public int getHops() { return hops; }
    public void setHops(int hops) { this.hops = hops; }
}
