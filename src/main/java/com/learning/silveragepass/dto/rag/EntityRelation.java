package com.learning.silveragepass.dto.rag;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 实体关系
 * Entity Relation
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public class EntityRelation {
    
    private String id;
    private String sourceEntityId;
    private String targetEntityId;
    private RelationType type;
    private double weight;
    private Map<String, Object> properties;
    
    public EntityRelation() {
        this.properties = new HashMap<>();
        this.weight = 1.0;
    }
    
    public EntityRelation(String sourceEntityId, String targetEntityId, RelationType type) {
        this.id = generateId(sourceEntityId, targetEntityId, type);
        this.sourceEntityId = sourceEntityId;
        this.targetEntityId = targetEntityId;
        this.type = type;
        this.weight = 1.0;
        this.properties = new HashMap<>();
    }
    
    public static EntityRelation create(HealthEntity source, HealthEntity target, RelationType type) {
        return new EntityRelation(source.getId(), target.getId(), type);
    }
    
    private static String generateId(String sourceId, String targetId, RelationType type) {
        return sourceId + "_" + type.getCode() + "_" + targetId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSourceEntityId() { return sourceEntityId; }
    public void setSourceEntityId(String sourceEntityId) { this.sourceEntityId = sourceEntityId; }
    
    public String getTargetEntityId() { return targetEntityId; }
    public void setTargetEntityId(String targetEntityId) { this.targetEntityId = targetEntityId; }
    
    public RelationType getType() { return type; }
    public void setType(RelationType type) { this.type = type; }
    
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityRelation that = (EntityRelation) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
