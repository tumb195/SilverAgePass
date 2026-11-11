package com.learning.silveragepass.forum.entity;

/**
 * 帖子状态枚举
 */
public enum PostStatus {
    NORMAL("正常"),
    DELETED("已删除"),
    HIDDEN("已隐藏");  // 预留，用于管理员隐藏违规内容
    
    private final String description;
    
    PostStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

