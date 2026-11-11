-- 知识分块表迁移脚本
-- Knowledge Chunks Migration Script
-- 执行时间: 2026-01-31

-- 创建知识分块表
CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id VARCHAR(36) PRIMARY KEY COMMENT '分块ID',
    document_id VARCHAR(36) NOT NULL COMMENT '关联文档ID',
    content TEXT NOT NULL COMMENT '分块内容',
    chunk_index INT NOT NULL COMMENT '分块序号',
    section_path VARCHAR(500) COMMENT '章节路径，如"糖尿病管理/饮食指南"',
    heading_level INT DEFAULT 0 COMMENT '标题层级(1-6)',
    parent_chunk_id VARCHAR(36) COMMENT '父分块ID',
    keywords JSON COMMENT '关键词列表',
    start_offset INT COMMENT '在原文中的起始位置',
    end_offset INT COMMENT '在原文中的结束位置',
    token_count INT COMMENT 'Token数量估算',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_chunk_document_id (document_id),
    INDEX idx_chunk_section_path (section_path),
    INDEX idx_chunk_is_active (is_active),
    INDEX idx_chunk_index (document_id, chunk_index),
    
    CONSTRAINT fk_chunk_document 
        FOREIGN KEY (document_id) 
        REFERENCES knowledge_documents(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='知识库分块表';

-- 添加全文索引（可选，用于关键词搜索）
-- ALTER TABLE knowledge_chunks ADD FULLTEXT INDEX ft_content (content);
