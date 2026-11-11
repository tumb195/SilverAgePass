-- 双Token认证系统 - Refresh Token表迁移脚本
-- 用于存储Refresh Token的哈希值和相关元数据

-- 创建refresh_tokens表
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE COMMENT 'Token的SHA-256哈希值',
    user_id BIGINT NOT NULL COMMENT '关联的用户ID',
    expiry_date TIMESTAMP NOT NULL COMMENT 'Token过期时间',
    used BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已被使用（用于检测重放攻击）',
    revoked BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已被撤销',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_expiry (expiry_date),
    INDEX idx_refresh_tokens_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌表';

-- 清理过期Token的存储过程（可选，用于定期清理）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS cleanup_expired_refresh_tokens()
BEGIN
    DELETE FROM refresh_tokens WHERE expiry_date < NOW() OR revoked = TRUE;
END //
DELIMITER ;
