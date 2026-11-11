package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.CertificationStatus;
import com.learning.silveragepass.entity.OrganizationCertification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 组织认证Repository接口
 * 提供组织认证数据的持久化操作
 */
@Repository
public interface OrganizationCertificationRepository extends JpaRepository<OrganizationCertification, Long> {

    // ==================== 按用户ID查询方法 ====================

    /**
     * 根据用户ID查询认证记录
     * 
     * @param userId 用户ID
     * @return 认证记录（可能为空）
     */
    Optional<OrganizationCertification> findByUserId(Long userId);

    /**
     * 根据用户ID查询所有认证记录（包括历史记录）
     * 
     * @param userId 用户ID
     * @return 认证记录列表
     */
    List<OrganizationCertification> findAllByUserId(Long userId);

    /**
     * 根据用户ID和状态查询认证记录
     * 
     * @param userId 用户ID
     * @param status 认证状态
     * @return 认证记录（可能为空）
     */
    Optional<OrganizationCertification> findByUserIdAndStatus(Long userId, CertificationStatus status);

    /**
     * 检查用户是否存在指定状态的认证记录
     * 
     * @param userId 用户ID
     * @param status 认证状态
     * @return 是否存在
     */
    boolean existsByUserIdAndStatus(Long userId, CertificationStatus status);

    /**
     * 检查用户是否存在指定状态列表中的认证记录
     * 
     * @param userId 用户ID
     * @param statuses 认证状态列表
     * @return 是否存在
     */
    boolean existsByUserIdAndStatusIn(Long userId, List<CertificationStatus> statuses);

    /**
     * 根据用户ID查询最新的认证记录（按创建时间倒序）
     * 
     * @param userId 用户ID
     * @return 最新的认证记录（可能为空）
     */
    Optional<OrganizationCertification> findFirstByUserIdOrderByCreateTimeDesc(Long userId);

    // ==================== 按状态统计方法 ====================

    /**
     * 统计指定状态的认证数量
     * 
     * @param status 认证状态
     * @return 数量
     */
    long countByStatus(CertificationStatus status);

    /**
     * 统计所有认证记录数量
     * 
     * @return 总数量
     */
    @Query("SELECT COUNT(c) FROM OrganizationCertification c")
    long countAll();

    // ==================== 分页查询方法 ====================

    /**
     * 根据状态分页查询认证记录
     * 
     * @param status 认证状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<OrganizationCertification> findByStatus(CertificationStatus status, Pageable pageable);

    /**
     * 根据状态和关键词分页查询认证记录
     * 关键词匹配组织名称
     * 
     * @param status 认证状态（可为null，表示查询所有状态）
     * @param keyword 关键词（可为null，表示不过滤）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT c FROM OrganizationCertification c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:keyword IS NULL OR :keyword = '' OR c.organizationName LIKE CONCAT('%', :keyword, '%'))")
    Page<OrganizationCertification> findByStatusAndKeyword(
            @Param("status") CertificationStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 根据状态和关键词分页查询认证记录（关键词匹配组织名称或证书编号）
     * 
     * @param status 认证状态（可为null，表示查询所有状态）
     * @param keyword 关键词（可为null，表示不过滤）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT c FROM OrganizationCertification c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "c.organizationName LIKE CONCAT('%', :keyword, '%') OR " +
           "c.certificateNumber LIKE CONCAT('%', :keyword, '%'))")
    Page<OrganizationCertification> findByStatusAndKeywordExtended(
            @Param("status") CertificationStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 分页查询所有认证记录（按创建时间倒序）
     * 
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<OrganizationCertification> findAllByOrderByCreateTimeDesc(Pageable pageable);

    /**
     * 根据审核员ID分页查询认证记录
     * 
     * @param reviewerId 审核员ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<OrganizationCertification> findByReviewerId(Long reviewerId, Pageable pageable);

    // ==================== 按组织ID查询方法 ====================

    /**
     * 根据组织ID统计认证数量
     * 
     * @param organizationId 组织ID
     * @return 认证数量
     */
    long countByOrganizationId(Long organizationId);

    /**
     * 根据组织ID和状态统计认证数量
     * 
     * @param organizationId 组织ID
     * @param status 认证状态
     * @return 认证数量
     */
    long countByOrganizationIdAndStatus(Long organizationId, CertificationStatus status);

    /**
     * 根据组织ID分页查询认证记录
     * 
     * @param organizationId 组织ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<OrganizationCertification> findByOrganizationId(Long organizationId, Pageable pageable);

    /**
     * 根据组织ID和状态分页查询认证记录
     * 
     * @param organizationId 组织ID
     * @param status 认证状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<OrganizationCertification> findByOrganizationIdAndStatus(
            Long organizationId, CertificationStatus status, Pageable pageable);
}
