package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.LocationRequest;
import com.learning.silveragepass.entity.LocationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 位置请求数据访问层
 * Requirements: 3.2, 3.4, 3.6
 */
@Repository
public interface LocationRequestRepository extends JpaRepository<LocationRequest, Long> {
    
    /**
     * 根据请求ID查询
     */
    Optional<LocationRequest> findByRequestId(String requestId);
    
    /**
     * 根据请求者ID查询所有请求
     */
    List<LocationRequest> findByRequesterId(Long requesterId);
    
    /**
     * 根据老人ID查询所有请求
     */
    List<LocationRequest> findByElderId(Long elderId);
    
    /**
     * 根据状态查询请求
     */
    List<LocationRequest> findByStatus(LocationRequestStatus status);
    
    /**
     * 查询已过期但仍为PENDING状态的请求
     * 用于超时处理
     */
    List<LocationRequest> findByStatusAndExpireTimeBefore(LocationRequestStatus status, LocalDateTime expireTime);
    
    /**
     * 根据请求者ID和老人ID查询最近的请求
     */
    Optional<LocationRequest> findFirstByRequesterIdAndElderIdOrderByRequestTimeDesc(Long requesterId, Long elderId);
    
    /**
     * 根据请求者ID和状态查询
     */
    List<LocationRequest> findByRequesterIdAndStatus(Long requesterId, LocationRequestStatus status);
}
