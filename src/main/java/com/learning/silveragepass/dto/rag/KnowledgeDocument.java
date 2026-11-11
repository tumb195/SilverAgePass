package com.learning.silveragepass.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识文档DTO
 * Knowledge Document DTO
 * 
 * 用于向量存储操作的知识文档数据传输对象。
 * 包含文档内容、类别、来源和元数据信息。
 * 
 * 注意：这是一个DTO类，用于VectorStore操作。
 * 实际的JPA实体将在Task 5.1中创建。
 * 
 * Requirements:
 * - 2.1: Vector database stores knowledge embeddings with metadata (source, category, timestamp, version)
 * - 2.3: Vector database supports batch insert of knowledge documents
 * - 2.4: Vector database supports update and delete operations
 * - 6.2: Knowledge base supports categorization (elderly_care, chronic_disease, medication, mental_health)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {
    
    /**
     * 文档唯一标识符
     * Document unique identifier
     */
    private String id;
    
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
     * 
     * Requirement 6.2: Knowledge base supports categorization
     */
    private String category;
    
    /**
     * 来源类型
     * Source type
     * 
     * 支持的类型：
     * - file: 文件导入
     * - api: API导入
     * - manual: 手动输入
     */
    private String sourceType;
    
    /**
     * 来源名称
     * Source name
     */
    private String sourceName;
    
    /**
     * 文档版本
     * Document version
     * 
     * Requirement 6.3: Knowledge base supports version control
     */
    private Integer version;
    
    /**
     * 创建时间
     * Created timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     * Updated timestamp
     */
    private LocalDateTime updatedAt;
    
    /**
     * 是否激活
     * Is active flag
     */
    private Boolean isActive;
    
    /**
     * 额外元数据
     * Additional metadata
     */
    private Map<String, Object> metadata;
    
    /**
     * 预计算的嵌入向量（可选）
     * Pre-computed embedding vector (optional)
     * 
     * 如果提供，VectorStore将直接使用此向量；
     * 如果为null，VectorStore将调用EmbeddingService生成向量。
     */
    private float[] embedding;
    
    /**
     * 知识类别枚举
     * Knowledge category enumeration
     */
    public enum Category {
        /**
         * 老年护理
         * Elderly care
         */
        ELDERLY_CARE("elderly_care"),
        
        /**
         * 慢病管理
         * Chronic disease management
         */
        CHRONIC_DISEASE("chronic_disease"),
        
        /**
         * 用药指导
         * Medication guidance
         */
        MEDICATION("medication"),
        
        /**
         * 心理健康
         * Mental health
         */
        MENTAL_HEALTH("mental_health");
        
        private final String value;
        
        Category(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static Category fromValue(String value) {
            for (Category category : values()) {
                if (category.value.equals(value)) {
                    return category;
                }
            }
            throw new IllegalArgumentException("Unknown category: " + value);
        }
    }
    
    /**
     * 来源类型枚举
     * Source type enumeration
     */
    public enum SourceType {
        FILE("file"),
        API("api"),
        MANUAL("manual");
        
        private final String value;
        
        SourceType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    /**
     * 构建元数据Map
     * Build metadata map
     * 
     * @return 包含所有元数据的Map (map containing all metadata)
     */
    public Map<String, Object> buildMetadata() {
        Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("category", category);
        meta.put("source", sourceType);
        meta.put("sourceName", sourceName);
        meta.put("version", version);
        meta.put("createdAt", createdAt != null ? createdAt.toString() : null);
        meta.put("updatedAt", updatedAt != null ? updatedAt.toString() : null);
        meta.put("isActive", isActive);
        
        if (metadata != null) {
            meta.putAll(metadata);
        }
        
        return meta;
    }
    
    /**
     * 创建新文档的便捷方法
     * Convenience method to create a new document
     * 
     * @param id 文档ID (document ID)
     * @param content 内容 (content)
     * @param category 类别 (category)
     * @return 文档实例 (document instance)
     */
    public static KnowledgeDocument create(String id, String content, String category) {
        return KnowledgeDocument.builder()
                .id(id)
                .content(content)
                .category(category)
                .version(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建带来源信息的文档
     * Create document with source information
     * 
     * @param id 文档ID (document ID)
     * @param content 内容 (content)
     * @param category 类别 (category)
     * @param sourceType 来源类型 (source type)
     * @param sourceName 来源名称 (source name)
     * @return 文档实例 (document instance)
     */
    public static KnowledgeDocument create(String id, String content, String category,
                                           String sourceType, String sourceName) {
        return KnowledgeDocument.builder()
                .id(id)
                .content(content)
                .category(category)
                .sourceType(sourceType)
                .sourceName(sourceName)
                .version(1)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
