package com.learning.silveragepass.entity;

/**
 * 消息类型枚举
 */
public enum MessageType {
    SYSTEM("系统消息"),
    ADMIN("管理员消息"),
    DEMAND("需求消息"),
    RELATIVE("亲人消息"),
    FORUM_COMMENT("论坛评论通知"),
    FORUM_REPLY("论坛回复通知"),
    FORUM_LIKE("论坛点赞通知"),
    USER_FOLLOW("用户关注通知");
    
    private final String description;
    
    MessageType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 