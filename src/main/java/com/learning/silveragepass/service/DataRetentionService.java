package com.learning.silveragepass.service;

/**
 * 数据保留策略服务接口
 * Data retention policy service interface
 * 
 * 实现数据保留策略，删除超过90天的原始消息内容，同时保留聚合统计数据
 * Implements data retention policies to delete raw message content older than 90 days
 * while preserving aggregated statistics
 */
public interface DataRetentionService {

    /**
     * 执行数据保留策略
     * Execute data retention policy
     * 
     * 删除超过配置天数的消息内容，保留情感分析记录用于统计
     * Deletes message content older than configured days while preserving emotion analysis records
     * 
     * @return 删除的消息数量 Number of messages deleted
     */
    int executeRetentionPolicy();

    /**
     * 获取配置的保留期限（天数）
     * Get configured retention period in days
     * 
     * @return 保留期限天数 Retention period in days
     */
    int getRetentionPeriodDays();

    /**
     * 统计超过保留期限的消息数量
     * Count messages older than retention period
     * 
     * @return 超过保留期限的消息数量 Number of messages older than retention period
     */
    long countMessagesOlderThanRetentionPeriod();
}
