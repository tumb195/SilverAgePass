package com.learning.silveragepass.entity;

/**
 * 数据删除状态枚举
 * Data deletion status enum
 * 
 * 表示数据删除请求的处理状态
 * Represents the processing status of a data deletion request
 */
public enum DeletionStatus {
    /**
     * 待处理 - 请求已创建，等待处理
     * Pending - Request created, waiting for processing
     */
    PENDING,

    /**
     * 处理中 - 正在删除数据
     * Processing - Data deletion in progress
     */
    PROCESSING,

    /**
     * 已完成 - 数据删除成功完成
     * Completed - Data deletion successfully completed
     */
    COMPLETED,

    /**
     * 失败 - 数据删除失败
     * Failed - Data deletion failed
     */
    FAILED
}
