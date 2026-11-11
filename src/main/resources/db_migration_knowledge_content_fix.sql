-- =====================================================
-- 知识库内容字段扩容迁移脚本
-- Knowledge Base Content Field Expansion Migration
-- =====================================================
-- 问题: knowledge_documents表的content字段使用TEXT类型，最大65KB
-- 解决: 将content字段改为MEDIUMTEXT，支持最大16MB
-- =====================================================

-- 1. 修改knowledge_documents表的content字段为MEDIUMTEXT
ALTER TABLE knowledge_documents 
MODIFY COLUMN content MEDIUMTEXT NOT NULL;

-- 2. 修改knowledge_chunks表的content字段为MEDIUMTEXT (如果存在)
ALTER TABLE knowledge_chunks 
MODIFY COLUMN content MEDIUMTEXT NOT NULL;

-- 验证修改
-- SHOW COLUMNS FROM knowledge_documents LIKE 'content';
-- SHOW COLUMNS FROM knowledge_chunks LIKE 'content';
