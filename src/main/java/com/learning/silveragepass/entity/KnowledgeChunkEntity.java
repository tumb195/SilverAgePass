package com.learning.silveragepass.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 知识分块实体
 * Knowledge Chunk Entity
 */
@Entity
@Table(name = "knowledge_chunks", indexes = {
        @Index(name = "idx_chunk_document_id", columnList = "document_id"),
        @Index(name = "idx_chunk_section_path", columnList = "section_path"),
        @Index(name = "idx_chunk_is_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkEntity {
    
    @Id
    @Column(length = 36)
    private String id;
    
    /** 关联的文档ID */
    @Column(name = "document_id", length = 36, nullable = false)
    private String documentId;
    
    /** 分块内容 - 使用MEDIUMTEXT支持较大分块 */
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;
    
    /** 分块序号 */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    
    /** 章节路径 */
    @Column(name = "section_path", length = 500)
    private String sectionPath;
    
    /** 标题层级 */
    @Column(name = "heading_level")
    @Builder.Default
    private Integer headingLevel = 0;
    
    /** 父分块ID */
    @Column(name = "parent_chunk_id", length = 36)
    private String parentChunkId;
    
    /** 关键词JSON */
    @Column(columnDefinition = "JSON")
    private String keywords;
    
    /** 在原文中的起始位置 */
    @Column(name = "start_offset")
    private Integer startOffset;
    
    /** 在原文中的结束位置 */
    @Column(name = "end_offset")
    private Integer endOffset;
    
    /** Token数量 */
    @Column(name = "token_count")
    private Integer tokenCount;
    
    /** 是否激活 */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    /** 创建时间 */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    /** 关联的文档实体 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    @ToString.Exclude
    private KnowledgeDocumentEntity document;
}
