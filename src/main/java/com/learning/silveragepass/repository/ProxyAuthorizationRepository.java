package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.ProxyAuthorization;
import com.learning.silveragepass.entity.ProxyAuthorizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 代发授权数据访问层
 * 提供代发授权的CRUD操作和查询方法
 */
@Repository
public interface ProxyAuthorizationRepository extends JpaRepository<ProxyAuthorization, Long> {
    
    /**
     * 根据老人ID和联系人ID查询授权关系
     * 用于检查是否已存在授权关系
     * Requirements: 1.3 - 检查重复授权
     */
    Optional<ProxyAuthorization> findByElderIdAndRelativeId(Long elderId, Long relativeId);
    
    /**
     * 根据联系人ID查询所有授权关系
     * 用于联系人查看自己的授权列表
     * Requirements: 1.1 - 联系人申请授权
     */
    List<ProxyAuthorization> findByRelativeId(Long relativeId);
    
    /**
     * 根据老人ID查询所有授权关系
     * 用于老人查看所有授权申请
     * Requirements: 2.1, 2.2, 2.3 - 老人管理授权
     */
    List<ProxyAuthorization> findByElderId(Long elderId);
    
    /**
     * 根据老人ID和状态查询授权关系
     * 用于老人查看特定状态的授权（如待审批、已批准）
     * Requirements: 2.1, 2.2, 2.3 - 老人管理授权
     */
    List<ProxyAuthorization> findByElderIdAndStatus(Long elderId, ProxyAuthorizationStatus status);
    
    /**
     * 根据老人ID、联系人ID和状态查询授权关系
     * 用于检查是否存在特定状态的授权
     * Requirements: 1.2, 1.3 - 验证授权状态
     */
    Optional<ProxyAuthorization> findByElderIdAndRelativeIdAndStatus(Long elderId, Long relativeId, ProxyAuthorizationStatus status);
    
    /**
     * 根据老人ID、联系人ID和多个状态查询授权关系
     * 用于检查是否存在待审批或已批准的授权
     * Requirements: 1.3 - 检查重复授权
     */
    List<ProxyAuthorization> findByElderIdAndRelativeIdAndStatusIn(Long elderId, Long relativeId, List<ProxyAuthorizationStatus> statuses);
    
    /**
     * 根据联系人ID和状态查询授权关系
     * 用于联系人查看特定状态的授权
     * Requirements: 1.1 - 联系人查看授权
     */
    List<ProxyAuthorization> findByRelativeIdAndStatus(Long relativeId, ProxyAuthorizationStatus status);
}
