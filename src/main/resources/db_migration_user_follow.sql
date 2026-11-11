-- 用户关注功能数据库迁移脚本
-- 创建时间: 2025-10-27
-- 功能说明: 添加用户关注功能相关表

-- 创建用户关注表
CREATE TABLE IF NOT EXISTS user_follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    follower_id BIGINT NOT NULL COMMENT '关注者ID（谁关注了别人）',
    followed_id BIGINT NOT NULL COMMENT '被关注者ID（被谁关注）',
    create_time DATETIME NOT NULL COMMENT '关注时间',
    CONSTRAINT unique_follower_followed UNIQUE (follower_id, followed_id),
    INDEX idx_follower_id (follower_id),
    INDEX idx_followed_id (followed_id),
    INDEX idx_follower_create_time (follower_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注表';

-- 为user_follows表添加外键约束（可选，根据实际需求决定是否启用）
-- ALTER TABLE user_follows
-- ADD CONSTRAINT fk_user_follows_follower FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
-- ADD CONSTRAINT fk_user_follows_followed FOREIGN KEY (followed_id) REFERENCES users(id) ON DELETE CASCADE;

-- 如果messages表还不存在USER_FOLLOW消息类型，需要确保MessageType枚举已更新
-- MessageType枚举已在代码中添加了USER_FOLLOW类型，无需数据库迁移

-- 验证表结构
-- SHOW CREATE TABLE user_follows;
-- SELECT COUNT(*) FROM user_follows;

