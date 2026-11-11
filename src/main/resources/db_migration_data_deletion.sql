-- Database migration for data deletion request feature
-- 数据删除请求功能的数据库迁移脚本

-- Create data_deletion_request table
-- 创建数据删除请求表
CREATE TABLE IF NOT EXISTS data_deletion_request (
    request_id BINARY(16) PRIMARY KEY COMMENT '删除请求ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '删除状态: PENDING, PROCESSING, COMPLETED, FAILED',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '请求时间',
    scheduled_deletion_date TIMESTAMP NOT NULL COMMENT '计划删除日期',
    processed_at TIMESTAMP NULL COMMENT '处理时间',
    records_deleted INT NULL COMMENT '删除的记录数',
    error_message TEXT NULL COMMENT '错误消息',
    
    CONSTRAINT chk_deletion_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_scheduled_deletion_date (scheduled_deletion_date),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据删除请求表';

-- Add comments for better documentation
-- 添加注释以便更好地记录
ALTER TABLE data_deletion_request COMMENT = '数据删除请求表 - 记录用户的数据删除请求，跟踪删除状态和进度';

-- Create audit log for data deletion requests
-- 为数据删除请求创建审计日志
-- Note: This is optional and depends on your audit logging strategy
-- 注意：这是可选的，取决于您的审计日志策略

-- Example query to check pending deletion requests
-- 查询待处理的删除请求示例
-- SELECT * FROM data_deletion_request 
-- WHERE status = 'PENDING' 
-- AND scheduled_deletion_date <= NOW()
-- ORDER BY scheduled_deletion_date ASC;

-- Example query to check user's deletion request status
-- 查询用户的删除请求状态示例
-- SELECT * FROM data_deletion_request 
-- WHERE user_id = ? 
-- ORDER BY requested_at DESC 
-- LIMIT 1;
