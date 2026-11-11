CREATE TABLE IF NOT EXISTS `reply_likes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `reply_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `unique_reply_user` (`reply_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
