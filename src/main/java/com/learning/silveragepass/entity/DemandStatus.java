package com.learning.silveragepass.entity;

/**
 * 需求状态枚举
 * 待接单、已接单、进行中、已完成、已取消
 */
public enum DemandStatus {
    WAITING("待接单"),
    ACCEPTED("已接单"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    DemandStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 