-- =====================================================
-- RAG高级检索数据库迁移脚本
-- RAG Advanced Retrieval Database Migration Script
-- Version: 1.0
-- Date: 2026-01-31
-- =====================================================

-- 1. 扩展 knowledge_documents 表，添加元数据增强字段
-- Extend knowledge_documents table with metadata enhancement fields

ALTER TABLE knowledge_documents 
ADD COLUMN IF NOT EXISTS keywords JSON COMMENT '提取的关键词列表';

ALTER TABLE knowledge_documents 
ADD COLUMN IF NOT EXISTS summary VARCHAR(500) COMMENT '自动生成的摘要';

ALTER TABLE knowledge_documents 
ADD COLUMN IF NOT EXISTS medical_entities JSON COMMENT '提取的医学实体';

ALTER TABLE knowledge_documents 
ADD COLUMN IF NOT EXISTS enhanced_at TIMESTAMP NULL COMMENT '元数据增强时间';

ALTER TABLE knowledge_documents 
ADD COLUMN IF NOT EXISTS enhancement_version INT DEFAULT 0 COMMENT '增强版本号';

-- 2. 创建检索性能统计表
-- Create retrieval performance statistics table

CREATE TABLE IF NOT EXISTS retrieval_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL COMMENT '统计日期',
    stat_hour INT NOT NULL COMMENT '统计小时 (0-23)',
    
    -- 检索计数
    vector_search_count BIGINT DEFAULT 0 COMMENT '向量检索次数',
    keyword_search_count BIGINT DEFAULT 0 COMMENT '关键词检索次数',
    hybrid_search_count BIGINT DEFAULT 0 COMMENT '混合检索次数',
    rerank_count BIGINT DEFAULT 0 COMMENT '重排序次数',
    kg_query_count BIGINT DEFAULT 0 COMMENT '知识图谱查询次数',
    
    -- 耗时统计（毫秒）
    vector_search_total_ms BIGINT DEFAULT 0 COMMENT '向量检索总耗时',
    keyword_search_total_ms BIGINT DEFAULT 0 COMMENT '关键词检索总耗时',
    hybrid_search_total_ms BIGINT DEFAULT 0 COMMENT '混合检索总耗时',
    rerank_total_ms BIGINT DEFAULT 0 COMMENT '重排序总耗时',
    
    -- 错误统计
    vector_search_errors BIGINT DEFAULT 0 COMMENT '向量检索错误次数',
    keyword_search_errors BIGINT DEFAULT 0 COMMENT '关键词检索错误次数',
    rerank_errors BIGINT DEFAULT 0 COMMENT '重排序错误次数',
    timeout_count BIGINT DEFAULT 0 COMMENT '超时次数',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_stat_date_hour (stat_date, stat_hour),
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='检索性能统计表';

-- 3. 创建知识图谱实体表（用于持久化）
-- Create knowledge graph entity table (for persistence)

CREATE TABLE IF NOT EXISTS kg_entities (
    id VARCHAR(100) PRIMARY KEY COMMENT '实体ID',
    name VARCHAR(200) NOT NULL COMMENT '实体名称',
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型',
    properties JSON COMMENT '实体属性',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_name (name),
    INDEX idx_entity_type (entity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='知识图谱实体表';

-- 4. 创建知识图谱关系表
-- Create knowledge graph relation table

CREATE TABLE IF NOT EXISTS kg_relations (
    id VARCHAR(200) PRIMARY KEY COMMENT '关系ID',
    source_entity_id VARCHAR(100) NOT NULL COMMENT '源实体ID',
    target_entity_id VARCHAR(100) NOT NULL COMMENT '目标实体ID',
    relation_type VARCHAR(50) NOT NULL COMMENT '关系类型',
    weight DOUBLE DEFAULT 1.0 COMMENT '关系权重',
    properties JSON COMMENT '关系属性',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_source_entity (source_entity_id),
    INDEX idx_target_entity (target_entity_id),
    INDEX idx_relation_type (relation_type),
    
    FOREIGN KEY (source_entity_id) REFERENCES kg_entities(id) ON DELETE CASCADE,
    FOREIGN KEY (target_entity_id) REFERENCES kg_entities(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='知识图谱关系表';

-- 5. 创建检索缓存表（可选，用于持久化缓存）
-- Create retrieval cache table (optional, for persistent cache)

CREATE TABLE IF NOT EXISTS retrieval_cache (
    cache_key VARCHAR(255) PRIMARY KEY COMMENT '缓存键',
    query_hash VARCHAR(64) NOT NULL COMMENT '查询哈希',
    results JSON NOT NULL COMMENT '检索结果',
    hit_count INT DEFAULT 0 COMMENT '命中次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    
    INDEX idx_expires_at (expires_at),
    INDEX idx_last_accessed (last_accessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='检索缓存表';

-- 6. 创建清理过期缓存的事件
-- Create event to clean expired cache

DELIMITER //

CREATE EVENT IF NOT EXISTS clean_expired_retrieval_cache
ON SCHEDULE EVERY 1 HOUR
DO
BEGIN
    DELETE FROM retrieval_cache WHERE expires_at < NOW();
END //

DELIMITER ;

-- 7. 插入初始知识图谱数据（基础医学知识）
-- Insert initial knowledge graph data (basic medical knowledge)

INSERT IGNORE INTO kg_entities (id, name, entity_type, properties) VALUES
('DISEASE_高血压', '高血压', 'DISEASE', '{"description": "血压持续升高的慢性疾病"}'),
('DISEASE_糖尿病', '糖尿病', 'DISEASE', '{"description": "血糖代谢异常的慢性疾病"}'),
('DISEASE_心脏病', '心脏病', 'DISEASE', '{"description": "心脏功能异常的疾病"}'),
('SYMPTOM_头痛', '头痛', 'SYMPTOM', '{"description": "头部疼痛症状"}'),
('SYMPTOM_头晕', '头晕', 'SYMPTOM', '{"description": "头部眩晕症状"}'),
('SYMPTOM_胸闷', '胸闷', 'SYMPTOM', '{"description": "胸部闷胀感"}'),
('MEDICATION_降压药', '降压药', 'MEDICATION', '{"description": "用于降低血压的药物"}'),
('MEDICATION_二甲双胍', '二甲双胍', 'MEDICATION', '{"description": "常用降糖药物"}'),
('LIFESTYLE_低盐饮食', '低盐饮食', 'LIFESTYLE', '{"description": "减少盐分摄入的饮食方式"}'),
('LIFESTYLE_规律作息', '规律作息', 'LIFESTYLE', '{"description": "保持规律的作息时间"}'),
('EXERCISE_散步', '散步', 'EXERCISE', '{"description": "适合老年人的有氧运动"}'),
('EXERCISE_太极拳', '太极拳', 'EXERCISE', '{"description": "传统健身运动"}');

INSERT IGNORE INTO kg_relations (id, source_entity_id, target_entity_id, relation_type, weight) VALUES
('DISEASE_高血压_CAUSES_SYMPTOM_头痛', 'DISEASE_高血压', 'SYMPTOM_头痛', 'CAUSES', 0.8),
('DISEASE_高血压_CAUSES_SYMPTOM_头晕', 'DISEASE_高血压', 'SYMPTOM_头晕', 'CAUSES', 0.9),
('MEDICATION_降压药_TREATS_DISEASE_高血压', 'MEDICATION_降压药', 'DISEASE_高血压', 'TREATS', 0.95),
('LIFESTYLE_低盐饮食_PREVENTS_DISEASE_高血压', 'LIFESTYLE_低盐饮食', 'DISEASE_高血压', 'PREVENTS', 0.7),
('DISEASE_心脏病_CAUSES_SYMPTOM_胸闷', 'DISEASE_心脏病', 'SYMPTOM_胸闷', 'CAUSES', 0.85),
('MEDICATION_二甲双胍_TREATS_DISEASE_糖尿病', 'MEDICATION_二甲双胍', 'DISEASE_糖尿病', 'TREATS', 0.9),
('EXERCISE_散步_PREVENTS_DISEASE_高血压', 'EXERCISE_散步', 'DISEASE_高血压', 'PREVENTS', 0.6),
('EXERCISE_太极拳_PREVENTS_DISEASE_心脏病', 'EXERCISE_太极拳', 'DISEASE_心脏病', 'PREVENTS', 0.5);

-- 8. 添加索引优化查询性能
-- Add indexes to optimize query performance

CREATE INDEX IF NOT EXISTS idx_kd_keywords ON knowledge_documents ((CAST(keywords AS CHAR(1000))));
CREATE INDEX IF NOT EXISTS idx_kd_enhanced_at ON knowledge_documents (enhanced_at);

-- 完成
-- Done
SELECT 'RAG Advanced Retrieval migration completed successfully' AS status;
