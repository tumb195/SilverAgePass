package com.learning.silveragepass.entity;

/**
 * 亲人关系状态枚举
 */
public enum RelativeStatus {
    PENDING("待确认"),
    ACCEPTED("已接受"),
    REJECTED("已拒绝");
    
    private final String description;
    
    RelativeStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 