package com.learning.silveragepass.entity;

/**
 * 代发授权状态枚举
 * 待审批、已批准、已拒绝、已撤销
 */
public enum ProxyAuthorizationStatus {
    PENDING("待审批"),
    APPROVED("已批准"),
    REJECTED("已拒绝"),
    REVOKED("已撤销");

    private final String description;

    ProxyAuthorizationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
