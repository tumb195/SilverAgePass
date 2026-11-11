-- ========================================
-- 银龄通 - 小区论坛模块数据库迁移脚本
-- 创建日期：2025-10-26
-- ========================================

-- 1. 创建帖子表
CREATE TABLE IF NOT EXISTS `posts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '发帖用户ID',
    `type` VARCHAR(50) NOT NULL COMMENT '帖子类型（LIFE_SHARING/NEIGHBOR_GATHERING/MUTUAL_HELP/COMMUNITY_ACTIVITY）',
    `title` VARCHAR(200) NOT NULL COMMENT '帖子标题',
    `content` TEXT NOT NULL COMMENT '帖子内容',
    `images` TEXT COMMENT '图片URL列表（JSON数组）',
    `video_url` VARCHAR(500) COMMENT '视频URL',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
    `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏数',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态（NORMAL/DELETED/HIDDEN）',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_type_create_time` (`type`, `create_time`),
    INDEX `idx_status_create_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- 2. 创建帖子点赞表
CREATE TABLE IF NOT EXISTS `post_likes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME NOT NULL COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `unique_post_user` (`post_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表';

-- 3. 创建帖子评论表
CREATE TABLE IF NOT EXISTS `post_comments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `images` TEXT COMMENT '图片URL列表（JSON数组）',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态（NORMAL/DELETED）',
    `create_time` DATETIME NOT NULL COMMENT '评论时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_post_id_create_time` (`post_id`, `create_time`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子评论表';

-- 4. 创建评论点赞表
CREATE TABLE IF NOT EXISTS `comment_likes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME NOT NULL COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `unique_comment_user` (`comment_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

-- 5. 创建评论回复表
CREATE TABLE IF NOT EXISTS `comment_replies` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `user_id` BIGINT NOT NULL COMMENT '回复用户ID',
    `reply_to_user_id` BIGINT NOT NULL COMMENT '被回复用户ID',
    `content` VARCHAR(500) NOT NULL COMMENT '回复内容',
    `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态（NORMAL/DELETED）',
    `create_time` DATETIME NOT NULL COMMENT '回复时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_comment_id_create_time` (`comment_id`, `create_time`),
    INDEX `idx_reply_to_user_id` (`reply_to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论回复表';

-- 6. 创建帖子收藏表
CREATE TABLE IF NOT EXISTS `post_favorites` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '收藏用户ID',
    `create_time` DATETIME NOT NULL COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `unique_post_user` (`post_id`, `user_id`),
    INDEX `idx_user_id_create_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子收藏表';

-- 7. 创建帖子浏览记录表
CREATE TABLE IF NOT EXISTS `post_views` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT COMMENT '浏览用户ID（可为空，支持匿名浏览）',
    `create_time` DATETIME NOT NULL COMMENT '浏览时间',
    PRIMARY KEY (`id`),
    INDEX `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子浏览记录表';

-- ========================================
-- 插入测试数据（可选，生产环境请删除）
-- ========================================

-- 注意：以下测试数据假设已有用户ID为1和2的用户

-- 插入测试帖子数据
INSERT INTO `posts` (`user_id`, `type`, `title`, `content`, `like_count`, `comment_count`, `favorite_count`, `view_count`, `status`, `create_time`, `update_time`) 
VALUES 
(1, 'LIFE_SHARING', '今天的晚霞真美', '在小区花园拍到的晚霞，真的太美了！分享给大家看看。', 5, 3, 2, 50, 'NORMAL', NOW(), NOW()),
(2, 'MUTUAL_HELP', '寻找会修电脑的邻居', '家里电脑突然开不了机了，有懂电脑的邻居能帮忙看看吗？', 2, 5, 1, 30, 'NORMAL', NOW(), NOW()),
(1, 'COMMUNITY_ACTIVITY', '本周六小区组织太极拳活动', '本周六上午9点在小区广场，欢迎大家参加！', 10, 8, 5, 100, 'NORMAL', NOW(), NOW()),
(2, 'NEIGHBOR_GATHERING', '周末一起去爬山吧', '有没有喜欢爬山的邻居，周末一起去附近的山上走走？', 3, 4, 2, 40, 'NORMAL', NOW(), NOW());

-- ========================================
-- 数据库迁移完成
-- ========================================

