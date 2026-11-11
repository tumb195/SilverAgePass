package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.KeyRequestStatsDTO;
import com.learning.silveragepass.entity.KeyRequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * API Key 审计服务接口
 * 负责记录和统计 API Key 请求日志
 */
public interface KeyAuditService {

    /**
     * 记录成功的 Key 请求
     *
     * @param userId   用户ID
     * @param keyType  Key类型
     * @param clientIp 客户端IP
     */
    void logSuccessfulRequest(Long userId, String keyType, String clientIp);

    /**
     * 记录失败的 Key 请求
     *
     * @param userId        用户ID
     * @param keyType       Key类型
     * @param clientIp      客户端IP
     * @param failureReason 失败原因
     */
    void logFailedRequest(Long userId, String keyType, String clientIp, String failureReason);

    /**
     * 获取用户的请求统计信息
     *
     * @param userId 用户ID
     * @return 请求统计DTO
     */
    KeyRequestStatsDTO getRequestStats(Long userId);

    /**
     * 获取用户的请求统计信息（按Key类型）
     *
     * @param userId  用户ID
     * @param keyType Key类型
     * @return 请求统计DTO
     */
    KeyRequestStatsDTO getRequestStats(Long userId, String keyType);

    /**
     * 获取用户的请求日志（分页）
     *
     * @param userId   用户ID
     * @param pageable 分页参数
     * @return 请求日志分页结果
     */
    Page<KeyRequestLog> getUserRequestLogs(Long userId, Pageable pageable);

    /**
     * 获取指定时间范围内的请求日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 请求日志列表
     */
    List<KeyRequestLog> getRequestLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取系统级别的请求统计（最近30天）
     *
     * @return 统计信息Map，包含总请求数、成功数、失败数等
     */
    Map<String, Object> getSystemStats();

    /**
     * 按Key类型获取请求统计
     *
     * @param startTime 开始时间
     * @return Key类型到请求数的映射
     */
    Map<String, Long> getStatsByKeyType(LocalDateTime startTime);

    /**
     * 清理指定天数之前的日志
     *
     * @param daysToKeep 保留天数
     * @return 删除的记录数
     */
    long cleanupOldLogs(int daysToKeep);
}
