package com.learning.silveragepass.entity;

/**
 * 血糖测量时段枚举
 * 空腹、餐前、餐后、睡前、随机
 */
public enum MeasurementPeriod {
    FASTING("空腹"),
    BEFORE_MEAL("餐前"),
    AFTER_MEAL("餐后"),
    BEFORE_SLEEP("睡前"),
    RANDOM("随机");

    private final String description;

    MeasurementPeriod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
