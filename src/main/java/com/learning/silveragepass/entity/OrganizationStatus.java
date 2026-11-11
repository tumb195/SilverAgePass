package com.learning.silveragepass.entity;

/**
 * 组织状态枚举
 */
public enum OrganizationStatus {
    ACTIVE("启用"),
    INACTIVE("禁用");

    private final String description;

    OrganizationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
