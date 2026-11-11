package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.LocationAuthorization;
import com.learning.silveragepass.entity.LocationAuthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 定位授权数据访问层
 * 提供定位授权的CRUD操作和查询方法
 * Requirements: 5.1, 5.3, 5.4, 5.5
 */
@Repository
public interface LocationAuthorizationRepository extends JpaRepository<LocationAuthorization, Long> {
    
    /**
     * 根据老人ID和亲属ID查询授权关系
     * 用于检查是否已存在授权关系
     */
    Optional<LocationAuthorization> findByElderIdAndRelativeId(Long elderId, Long relativeId);
    
    /**
     * 根据亲属ID查询所有授权关系
     * 用于亲属查看自己的定位授权列表
     */
    List<LocationAuthorization> findByRelativeId(Long relativeId);
    
    /**
     * 根据老人ID查询所有授权关系
     * 用于老人查看所有定位授权申请
     */
    List<LocationAuthorization> findByElderId(Long elderId);
    
    /**
     * 根据老人ID和状态查询授权关系
     * 用于老人查看特定状态的授权（如待审批、已批准）
     */
    List<LocationAuthorization> findByElderIdAndStatus(Long elderId, LocationAuthStatus status);
    
    /**
     * 根据老人ID、亲属ID和状态查询授权关系
     * 用于检查是否存在特定状态的授权
     */
    Optional<LocationAuthorization> findByElderIdAndRelativeIdAndStatus(Long elderId, Long relativeId, LocationAuthStatus status);
    
    /**
     * 根据老人ID、亲属ID和多个状态查询授权关系
     * 用于检查是否存在待审批或已批准的授权
     */
    List<LocationAuthorization> findByElderIdAndRelativeIdAndStatusIn(Long elderId, Long relativeId, List<LocationAuthStatus> statuses);
    
    /**
     * 根据亲属ID和状态查询授权关系
     * 用于亲属查看特定状态的授权
     */
    List<LocationAuthorization> findByRelativeIdAndStatus(Long relativeId, LocationAuthStatus status);
}
