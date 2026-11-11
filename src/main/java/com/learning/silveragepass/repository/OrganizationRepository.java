package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.Organization;
import com.learning.silveragepass.entity.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 组织数据访问接口
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    /**
     * 根据名称查询组织
     */
    Optional<Organization> findByName(String name);

    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查名称是否存在（排除指定ID）
     */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * 根据状态查询组织列表
     */
    List<Organization> findByStatusOrderByNameAsc(OrganizationStatus status);

    /**
     * 根据状态分页查询
     */
    Page<Organization> findByStatus(OrganizationStatus status, Pageable pageable);

    /**
     * 关键词搜索（名称或描述包含关键词）
     */
    @Query("SELECT o FROM Organization o WHERE " +
           "(LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Organization> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 关键词搜索 + 状态过滤
     */
    @Query("SELECT o FROM Organization o WHERE o.status = :status AND " +
           "(LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Organization> searchByKeywordAndStatus(
            @Param("keyword") String keyword,
            @Param("status") OrganizationStatus status,
            Pageable pageable);

    /**
     * 用户端：搜索启用的组织
     */
    @Query("SELECT o FROM Organization o WHERE o.status = 'ACTIVE' AND " +
           "(LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY o.name ASC")
    List<Organization> searchActiveByKeyword(@Param("keyword") String keyword);

    /**
     * 统计各状态数量
     */
    long countByStatus(OrganizationStatus status);
}
