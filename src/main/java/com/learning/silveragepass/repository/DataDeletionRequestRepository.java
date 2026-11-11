package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.DataDeletionRequest;
import com.learning.silveragepass.entity.DeletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 数据删除请求仓库接口
 * Data deletion request repository interface
 * 
 * 提供数据删除请求的数据访问操作
 * Provides data access operations for data deletion requests
 */
@Repository
public interface DataDeletionRequestRepository extends JpaRepository<DataDeletionRequest, UUID> {

    /**
     * 根据用户ID查找最新的删除请求
     * Find the latest deletion request by user ID
     * 
     * @param userId 用户ID User ID
     * @return 删除请求 Deletion request
     */
    Optional<DataDeletionRequest> findFirstByUserIdOrderByRequestedAtDesc(Long userId);

    /**
     * 根据状态查找删除请求
     * Find deletion requests by status
     * 
     * @param status 删除状态 Deletion status
     * @return 删除请求列表 List of deletion requests
     */
    List<DataDeletionRequest> findByStatus(DeletionStatus status);

    /**
     * 查找待处理且已到期的删除请求
     * Find pending deletion requests that are due for processing
     * 
     * @param status 删除状态 Deletion status
     * @param scheduledDate 计划删除日期 Scheduled deletion date
     * @return 删除请求列表 List of deletion requests
     */
    List<DataDeletionRequest> findByStatusAndScheduledDeletionDateBefore(
        DeletionStatus status, 
        LocalDateTime scheduledDate
    );

    /**
     * 检查用户是否有待处理的删除请求
     * Check if user has a pending deletion request
     * 
     * @param userId 用户ID User ID
     * @param status 删除状态 Deletion status
     * @return 是否存在 Whether exists
     */
    boolean existsByUserIdAndStatus(Long userId, DeletionStatus status);
}
