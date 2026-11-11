package com.learning.silveragepass.entity;

/**
 * 组织认证状态枚举
 * 待审核、已通过、已拒绝
 */
public enum CertificationStatus {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已拒绝");

    private final String description;

    CertificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
