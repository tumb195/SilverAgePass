package com.learning.silveragepass.service;

import java.util.UUID;

/**
 * 数据删除请求服务接口
 * Data deletion request service interface
 * 
 * 处理用户数据删除请求，在30天内永久删除所有用户相关数据
 * Handles user data deletion requests and permanently removes all user-related data within 30 days
 * 
 * 符合GDPR和数据隐私法规要求
 * Complies with GDPR and data privacy regulations
 */
public interface DataDeletionService {

    /**
     * 创建数据删除请求
     * Create a data deletion request
     * 
     * 用户请求删除其所有健康助手数据
     * User requests deletion of all their health assistant data
     * 
     * @param userId 用户ID User ID
     * @return 删除请求ID Deletion request ID
     */
    UUID createDeletionRequest(Long userId);

    /**
     * 执行数据删除
     * Execute data deletion
     * 
     * 删除用户的所有健康助手数据：
     * - 会话记录 (Sessions)
     * - 消息记录 (Messages)
     * - 情感分析 (Emotion analyses)
     * - 日记条目 (Diary entries)
     * - 日记分析 (Diary analyses)
     * 
     * Deletes all user health assistant data:
     * - Conversation sessions
     * - Messages
     * - Emotion analyses
     * - Diary entries
     * - Diary analyses
     * 
     * @param userId 用户ID User ID
     * @return 删除的记录总数 Total number of records deleted
     */
    int executeDataDeletion(Long userId);

    /**
     * 处理待处理的删除请求
     * Process pending deletion requests
     * 
     * 定期检查并处理队列中的删除请求
     * Periodically checks and processes deletion requests in the queue
     * 
     * @return 处理的请求数量 Number of requests processed
     */
    int processPendingDeletionRequests();

    /**
     * 获取用户的删除请求状态
     * Get user's deletion request status
     * 
     * @param userId 用户ID User ID
     * @return 删除请求状态，如果没有请求则返回null Status of deletion request, null if no request exists
     */
    String getDeletionRequestStatus(Long userId);

    /**
     * 统计用户的健康助手数据量
     * Count user's health assistant data
     * 
     * @param userId 用户ID User ID
     * @return 数据记录总数 Total number of data records
     */
    long countUserData(Long userId);
}
