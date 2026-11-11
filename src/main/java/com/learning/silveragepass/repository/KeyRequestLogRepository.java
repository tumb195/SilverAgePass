package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.KeyRequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key 请求日志仓库
 * 提供请求日志的持久化和统计查询功能
 */
@Repository
public interface KeyRequestLogRepository extends JpaRepository<KeyRequestLog, Long> {

    /**
     * 查询用户在指定时间范围内的请求数量
     */
    @Query("SELECT COUNT(k) FROM KeyRequestLog k WHERE k.userId = :userId AND k.requestTime >= :startTime")
    long countByUserIdAndRequestTimeAfter(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    /**
     * 查询用户在指定时间范围内对特定 Key 类型的请求数量
     */
    @Query("SELECT COUNT(k) FROM KeyRequestLog k WHERE k.userId = :userId AND k.keyType = :keyType AND k.requestTime >= :startTime")
    long countByUserIdAndKeyTypeAndRequestTimeAfter(
            @Param("userId") Long userId,
            @Param("keyType") String keyType,
            @Param("startTime") LocalDateTime startTime);

    /**
     * 查询用户今天的请求数量
     */
    @Query("SELECT COUNT(k) FROM KeyRequestLog k WHERE k.userId = :userId AND k.requestTime >= :todayStart")
    long countTodayRequestsByUserId(@Param("userId") Long userId, @Param("todayStart") LocalDateTime todayStart);

    /**
     * 查询用户最近一小时的请求数量
     */
    @Query("SELECT COUNT(k) FROM KeyRequestLog k WHERE k.userId = :userId AND k.requestTime >= :oneHourAgo")
    long countLastHourRequestsByUserId(@Param("userId") Long userId, @Param("oneHourAgo") LocalDateTime oneHourAgo);

    /**
     * 查询用户的最后一次请求时间
     */
    @Query("SELECT MAX(k.requestTime) FROM KeyRequestLog k WHERE k.userId = :userId")
    LocalDateTime findLastRequestTimeByUserId(@Param("userId") Long userId);

    /**
     * 查询用户的请求日志（分页）
     */
    Page<KeyRequestLog> findByUserIdOrderByRequestTimeDesc(Long userId, Pageable pageable);

    /**
     * 查询指定时间范围内的所有请求日志
     */
    List<KeyRequestLog> findByRequestTimeBetweenOrderByRequestTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 删除指定时间之前的日志（用于清理旧数据）
     */
    void deleteByRequestTimeBefore(LocalDateTime beforeTime);

    /**
     * 统计指定时间范围内的成功请求数
     */
    @Query("SELECT COUNT(k) FROM KeyRequestLog k WHERE k.success = true AND k.requestTime >= :startTime")
    long countSuccessfulRequestsAfter(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计指定时间范围内的失败请求数
     */
    @Query("SELECT COUNT(k) FROM KeyRequestLog k WHERE k.success = false AND k.requestTime >= :startTime")
    long countFailedRequestsAfter(@Param("startTime") LocalDateTime startTime);

    /**
     * 按 Key 类型统计请求数量
     */
    @Query("SELECT k.keyType, COUNT(k) FROM KeyRequestLog k WHERE k.requestTime >= :startTime GROUP BY k.keyType")
    List<Object[]> countByKeyTypeAfter(@Param("startTime") LocalDateTime startTime);
}
