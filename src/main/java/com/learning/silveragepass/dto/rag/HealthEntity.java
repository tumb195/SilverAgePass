package com.learning.silveragepass.dto.rag;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 健康实体
 * Health Entity
 * 
 * @author SilverAgePass Team
 * @version 1.0
 */
public class HealthEntity {
    
    private String id;
    private String name;
    private EntityType type;
    private Map<String, Object> properties;
    
    public HealthEntity() {
        this.properties = new HashMap<>();
    }
    
    public HealthEntity(String id, String name, EntityType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.properties = new HashMap<>();
    }
    
    public static HealthEntity create(String name, EntityType type) {
        return new HealthEntity(generateId(name, type), name, type);
    }
    
    private static String generateId(String name, EntityType type) {
        return type.getCode() + "_" + name.hashCode();
    }
    
    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public EntityType getType() { return type; }
    public void setType(EntityType type) { this.type = type; }
    
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthEntity that = (HealthEntity) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
