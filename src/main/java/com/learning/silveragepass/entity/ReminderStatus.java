package com.learning.silveragepass.entity;

/**
 * 复诊提醒状态枚举
 * 待复诊、已完成、已取消
 */
public enum ReminderStatus {
    PENDING("待复诊"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    ReminderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
