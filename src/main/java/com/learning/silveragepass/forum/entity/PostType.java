package com.learning.silveragepass.forum.entity;

/**
 * 帖子类型枚举
 */
public enum PostType {
    LIFE_SHARING("生活分享"),
    NEIGHBOR_GATHERING("日常娱乐"),
    MUTUAL_HELP("便民互助"),
    COMMUNITY_ACTIVITY("社区优惠");
    
    private final String description;
    
    PostType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

