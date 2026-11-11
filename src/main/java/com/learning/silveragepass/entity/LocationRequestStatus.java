package com.learning.silveragepass.entity;

/**
 * 位置请求状态枚举
 * Requirements: 3.2, 3.4, 3.6
 */
public enum LocationRequestStatus {
    /**
     * 待处理 - 请求已发送，等待老人App响应
     */
    PENDING,
    
    /**
     * 成功 - 已成功获取位置
     */
    SUCCESS,
    
    /**
     * 超时 - 请求超时（30秒内未响应）
     */
    TIMEOUT,
    
    /**
     * 失败 - 定位失败（权限问题、定位服务异常等）
     */
    FAILED,
    
    /**
     * 离线 - 老人设备离线
     */
    OFFLINE
}
