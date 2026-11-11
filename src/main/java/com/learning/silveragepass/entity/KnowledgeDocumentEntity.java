package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 知识文档实体类
 * Knowledge Document Entity for RAG retrieval
 * 
 * 存储健康知识文档，用于RAG语义检索。
 * 文档内容会被嵌入到向量数据库（Chroma）中进行语义搜索。
 * 
 * Requirements:
 * - 2.1: Vector database stores knowledge embeddings with metadata (source, category, timestamp, version)
 * - 6.2: Knowledge base supports categorization (elderly_care, chronic_disease, medication, mental_health)
 * - 6.3: Knowledge base supports version control to track updates
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Entity
@Table(name = "knowledge_documents",
    indexes = {
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_is_active", columnList = "is_active"),
        @Index(name = "idx_category_active", columnList = "category, is_active"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_updated_at", columnList = "updated_at"),
        @Index(name = "idx_category_version", columnList = "category, version")
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentEntity {

    /**
     * 文档唯一标识符 (UUID)
     * Document unique identifier, also used as Chroma document ID
     */
    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    /**
     * 文档内容
     * Knowledge document content for RAG retrieval
     * 使用MEDIUMTEXT支持最大16MB的文档内容
     */
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    /**
     * 文档类别
     * Knowledge category for filtered retrieval
     * 
     * 支持的类别 (Supported categories):
     * - elderly_care: 老年护理 (Elderly care)
     * - chronic_disease: 慢病管理 (Chronic disease management)
     * - medication: 用药指导 (Medication guidance)
     * - mental_health: 心理健康 (Mental health)
     * 
     * Requirement 6.2: Knowledge base supports categorization
     */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /**
     * 来源类型
     * Source type: file, api, manual
     */
    @Column(name = "source_type", length = 20)
    private String sourceType;

    /**
     * 来源名称
     * Original source name or file path
     */
    @Column(name = "source_name", length = 255)
    private String sourceName;

    /**
     * 文档版本号
     * Document version number, increments on update
     * 
     * Requirement 6.3: Knowledge base supports version control
     */
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * 创建时间
     * Document creation timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * Last update timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 是否激活
     * Whether document is active (FALSE = archived)
     * 
     * Requirement 6.4: Knowledge base supports deletion and archiving
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * 额外元数据 (JSON格式)
     * Additional metadata in JSON format
     * Can store: author, tags, related_documents, embedding_model_version, etc.
     */
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    /**
     * 预处理方法，设置默认值
     * Pre-persist callback to set default values
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.version == null) {
            this.version = 1;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.sourceType == null) {
            this.sourceType = SourceType.MANUAL.getValue();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        validateCategory();
        validateSourceType();
    }

    /**
     * 更新前处理方法
     * Pre-update callback to update timestamp
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateCategory();
        validateSourceType();
    }

    /**
     * 验证类别值
     * Validate category value
     */
    private void validateCategory() {
        if (category != null) {
            try {
                Category.fromValue(category);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Invalid category: " + category + 
                    ". Must be one of: elderly_care, chronic_disease, medication, mental_health");
            }
        }
    }

    /**
     * 验证来源类型值
     * Validate source type value
     */
    private void validateSourceType() {
        if (sourceType != null) {
            try {
                SourceType.fromValue(sourceType);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Invalid source type: " + sourceType + 
                    ". Must be one of: file, api, manual");
            }
        }
    }

    /**
     * 增加版本号
     * Increment version number for updates
     */
    public void incrementVersion() {
        this.version = (this.version == null ? 1 : this.version) + 1;
    }

    /**
     * 归档文档（软删除）
     * Archive document (soft delete)
     */
    public void archive() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 激活文档
     * Activate document
     */
    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 知识类别枚举
     * Knowledge category enumeration
     * 
     * Requirement 6.2: Knowledge base supports categorization
     */
    public enum Category {
        /**
         * 老年护理
         * Elderly care: General health tips, daily care, fall prevention
         */
        ELDERLY_CARE("elderly_care"),
        
        /**
         * 慢病管理
         * Chronic disease management: Diabetes, hypertension, heart disease
         */
        CHRONIC_DISEASE("chronic_disease"),
        
        /**
         * 用药指导
         * Medication guidance: Drug interactions, dosage guidelines, side effects
         */
        MEDICATION("medication"),
        
        /**
         * 心理健康
         * Mental health: Emotional support, stress management, cognitive health
         */
        MENTAL_HEALTH("mental_health"),
        
        /**
         * 综合照护
         * Comprehensive care: Integrated care approaches, holistic health
         */
        COMPREHENSIVE_CARE("comprehensive_care");

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
        /**
         * 文件导入
         * Imported from file
         */
        FILE("file"),
        
        /**
         * API导入
         * Imported from API
         */
        API("api"),
        
        /**
         * 手动输入
         * Manually entered
         */
        MANUAL("manual");

        private final String value;

        SourceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static SourceType fromValue(String value) {
            for (SourceType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown source type: " + value);
        }
    }

    /**
     * 创建新文档的便捷方法
     * Convenience method to create a new document
     * 
     * @param content 内容 (content)
     * @param category 类别 (category)
     * @return 文档实例 (document instance)
     */
    public static KnowledgeDocumentEntity create(String content, String category) {
        return KnowledgeDocumentEntity.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .category(category)
                .sourceType(SourceType.MANUAL.getValue())
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
     * @param content 内容 (content)
     * @param category 类别 (category)
     * @param sourceType 来源类型 (source type)
     * @param sourceName 来源名称 (source name)
     * @return 文档实例 (document instance)
     */
    public static KnowledgeDocumentEntity create(String content, String category,
                                                  String sourceType, String sourceName) {
        return KnowledgeDocumentEntity.builder()
                .id(UUID.randomUUID().toString())
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
