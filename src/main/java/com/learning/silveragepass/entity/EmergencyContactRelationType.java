package com.learning.silveragepass.entity;

/**
 * 紧急联系人关系类型枚举
 */
public enum EmergencyContactRelationType {
    CHILD("子女"),
    PARENT("父母"),
    SPOUSE("配偶"),
    SIBLING("兄弟姐妹"),
    FRIEND("朋友"),
    OTHER("其他");

    private final String description;

    EmergencyContactRelationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
