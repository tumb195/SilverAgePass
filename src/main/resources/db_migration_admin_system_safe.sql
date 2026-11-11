-- 后台管理系统数据库迁移脚本（安全版本）
-- 此脚本可以重复执行，不会因为表或字段已存在而报错

-- ============================================
-- 1. 创建管理员账户表
-- ============================================
CREATE TABLE IF NOT EXISTS admin_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(120) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20) NOT NULL UNIQUE,
    level VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_time DATETIME,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账户表';

-- ============================================
-- 2. 创建投诉表
-- ============================================
CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    author_id BIGINT,
    images TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    handler_id BIGINT,
    handle_time DATETIME,
    result TEXT,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='投诉表';

-- ============================================
-- 3. 插入默认管理员账户
-- ============================================
INSERT INTO admin_accounts (username, password, real_name, email, phone, level, enabled, create_time, update_time)
VALUES 
('admin', '$10$/j/CJfqFFsnPAzHnD8ZA6eTCgk1ZERQc3b1d5f/2w2RlgC/iD7Q5m', '系统管理员', 'admin@silveragepass.com', '13800138888', 'SUPER_ADMIN', TRUE, NOW(), NOW()),
('admin2', '$10$/j/CJfqFFsnPAzHnD8ZA6eTCgk1ZERQc3b1d5f/2w2RlgC/iD7Q5m', '管理员A', 'admin2@silveragepass.com', '13800138889', 'ADMIN', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;

-- ============================================
-- 4. 插入测试投诉数据
-- ============================================
INSERT INTO complaints (title, description, author_id, images, status, create_time, update_time)
VALUES 
('志愿者服务态度恶劣', '志愿者在服务过程中态度非常差，说话不耐烦，对老人不尊重。希望平台能够严肃处理。', 1, 'https://picsum.photos/400/300?random=101,https://picsum.photos/400/300?random=102', 'pending', NOW(), NOW()),
('服务质量差', '志愿者帮忙买菜，但是买的菜品质量很差，而且价格比市场价贵很多。', 2, '', 'resolved', NOW(), NOW()),
('志愿者迟到', '约定的服务时间是上午9点，志愿者10点半才到，导致老人错过了就医时间。', 3, 'https://picsum.photos/400/300?random=103', 'pending', NOW(), NOW())
ON DUPLICATE KEY UPDATE title=title;

-- ============================================
-- 5. 更新投诉表，添加处理信息
-- ============================================
UPDATE complaints 
SET handler_id = 1, 
    handle_time = NOW(), 
    result = '经调查，志愿者在购买时没有仔细核对。已要求志愿者退还差价，并对其进行批评教育。'
WHERE status = 'resolved' AND handler_id IS NULL;

-- ============================================
-- 6. 为用户表添加禁用相关字段（使用存储过程）
-- ============================================
DELIMITER //

-- 添加 banned 字段
DROP PROCEDURE IF EXISTS add_banned_column//
CREATE PROCEDURE add_banned_column()
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO col_exists 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'banned';
    
    IF col_exists = 0 THEN
        ALTER TABLE users ADD COLUMN banned BOOLEAN DEFAULT FALSE COMMENT '是否被禁用';
    END IF;
END//

-- 添加 ban_reason 字段
DROP PROCEDURE IF EXISTS add_ban_reason_column//
CREATE PROCEDURE add_ban_reason_column()
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO col_exists 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'ban_reason';
    
    IF col_exists = 0 THEN
        ALTER TABLE users ADD COLUMN ban_reason VARCHAR(500) COMMENT '禁用原因';
    END IF;
END//

-- 添加 ban_expire_time 字段
DROP PROCEDURE IF EXISTS add_ban_expire_time_column//
CREATE PROCEDURE add_ban_expire_time_column()
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO col_exists 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND COLUMN_NAME = 'ban_expire_time';
    
    IF col_exists = 0 THEN
        ALTER TABLE users ADD COLUMN ban_expire_time DATETIME COMMENT '禁用到期时间';
    END IF;
END//

DELIMITER ;

-- 执行存储过程
CALL add_banned_column();
CALL add_ban_reason_column();
CALL add_ban_expire_time_column();

-- 删除存储过程
DROP PROCEDURE IF EXISTS add_banned_column;
DROP PROCEDURE IF EXISTS add_ban_reason_column;
DROP PROCEDURE IF EXISTS add_ban_expire_time_column;

-- ============================================
-- 7. 添加索引（使用存储过程）
-- ============================================
DELIMITER //

-- 为 users 表添加索引
DROP PROCEDURE IF EXISTS add_users_indexes//
CREATE PROCEDURE add_users_indexes()
BEGIN
    DECLARE idx_exists INT DEFAULT 0;
    
    -- 添加 idx_banned 索引
    SELECT COUNT(*) INTO idx_exists 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND INDEX_NAME = 'idx_banned';
    
    IF idx_exists = 0 THEN
        ALTER TABLE users ADD INDEX idx_banned (banned);
    END IF;
    
    -- 添加 idx_users_create_time 索引
    SET idx_exists = 0;
    SELECT COUNT(*) INTO idx_exists 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND INDEX_NAME = 'idx_users_create_time';
    
    IF idx_exists = 0 THEN
        ALTER TABLE users ADD INDEX idx_users_create_time (create_time);
    END IF;
    
    -- 添加 idx_users_last_login_time 索引
    SET idx_exists = 0;
    SELECT COUNT(*) INTO idx_exists 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'users' 
    AND INDEX_NAME = 'idx_users_last_login_time';
    
    IF idx_exists = 0 THEN
        ALTER TABLE users ADD INDEX idx_users_last_login_time (last_login_time);
    END IF;
END//

-- 为 demands 表添加索引
DROP PROCEDURE IF EXISTS add_demands_indexes//
CREATE PROCEDURE add_demands_indexes()
BEGIN
    DECLARE idx_exists INT DEFAULT 0;
    
    -- 添加 idx_demands_create_time 索引
    SELECT COUNT(*) INTO idx_exists 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'demands' 
    AND INDEX_NAME = 'idx_demands_create_time';
    
    IF idx_exists = 0 THEN
        ALTER TABLE demands ADD INDEX idx_demands_create_time (create_time);
    END IF;
    
    -- 添加 idx_demands_status 索引
    SET idx_exists = 0;
    SELECT COUNT(*) INTO idx_exists 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'demands' 
    AND INDEX_NAME = 'idx_demands_status';
    
    IF idx_exists = 0 THEN
        ALTER TABLE demands ADD INDEX idx_demands_status (status);
    END IF;
END//

-- 为 warm_coin_records 表添加索引
DROP PROCEDURE IF EXISTS add_warm_coin_records_indexes//
CREATE PROCEDURE add_warm_coin_records_indexes()
BEGIN
    DECLARE idx_exists INT DEFAULT 0;
    
    -- 添加 idx_wcr_user_id 索引
    SELECT COUNT(*) INTO idx_exists 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'warm_coin_records' 
    AND INDEX_NAME = 'idx_wcr_user_id';
    
    IF idx_exists = 0 THEN
        ALTER TABLE warm_coin_records ADD INDEX idx_wcr_user_id (user_id);
    END IF;
    
    -- 添加 idx_wcr_create_time 索引
    SET idx_exists = 0;
    SELECT COUNT(*) INTO idx_exists 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'warm_coin_records' 
    AND INDEX_NAME = 'idx_wcr_create_time';
    
    IF idx_exists = 0 THEN
        ALTER TABLE warm_coin_records ADD INDEX idx_wcr_create_time (create_time);
    END IF;
    
    -- 添加 idx_wcr_type 索引
    SET idx_exists = 0;
    SELECT COUNT(*) INTO idx_exists 
    FROM information_schema.STATISTICS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'warm_coin_records' 
    AND INDEX_NAME = 'idx_wcr_type';
    
    IF idx_exists = 0 THEN
        ALTER TABLE warm_coin_records ADD INDEX idx_wcr_type (type);
    END IF;
END//

DELIMITER ;

-- 执行存储过程
CALL add_users_indexes();
CALL add_demands_indexes();
CALL add_warm_coin_records_indexes();

-- 删除存储过程
DROP PROCEDURE IF EXISTS add_users_indexes;
DROP PROCEDURE IF EXISTS add_demands_indexes;
DROP PROCEDURE IF EXISTS add_warm_coin_records_indexes;

-- ============================================
-- 完成
-- ============================================
SELECT '后台管理系统数据库迁移完成！' AS message;
