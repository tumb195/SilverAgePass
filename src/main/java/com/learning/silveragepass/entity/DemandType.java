package com.learning.silveragepass.entity;

/**
 * 需求类型枚举
 * 生活照料类、健康服务类、政务协助类、精神慰藉类、出行辅助类、其他
 */
public enum DemandType {
    LIFE_CARE("生活照料类"),
    HEALTH_SERVICE("健康服务类"),
    GOVERNMENT_ASSISTANCE("政务协助类"),
    MENTAL_COMFORT("精神慰藉类"),
    TRAVEL_ASSISTANCE("出行辅助类"),
    OTHER("其他");

    private final String description;

    DemandType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
} 