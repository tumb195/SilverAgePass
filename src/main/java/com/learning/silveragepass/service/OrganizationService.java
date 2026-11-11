package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.organization.*;
import com.learning.silveragepass.entity.Organization;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 组织管理服务接口
 * 提供管理端和用户端的组织相关操作
 */
public interface OrganizationService {

    // ==================== 管理端方法 ====================

    /**
     * 创建组织
     *
     * @param dto 创建组织请求
     * @return 创建的组织
     * @throws com.learning.silveragepass.exception.BusinessException 如果组织名称已存在
     *
     * Requirements: 1.1, 1.2, 1.3, 1.4
     */
    Organization createOrganization(OrganizationCreateDTO dto);

    /**
     * 更新组织信息
     *
     * @param id 组织ID
     * @param dto 更新组织请求
     * @return 更新后的组织
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException 如果组织不存在
     * @throws com.learning.silveragepass.exception.BusinessException 如果组织名称已存在
     *
     * Requirements: 3.1, 3.2, 3.3, 3.4
     */
    Organization updateOrganization(Long id, OrganizationUpdateDTO dto);

    /**
     * 启用组织
     *
     * @param id 组织ID
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException 如果组织不存在
     *
     * Requirements: 4.2
     */
    void enableOrganization(Long id);

    /**
     * 禁用组织
     *
     * @param id 组织ID
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException 如果组织不存在
     *
     * Requirements: 4.1
     */
    void disableOrganization(Long id);

    /**
     * 删除组织
     *
     * @param id 组织ID
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException 如果组织不存在
     * @throws com.learning.silveragepass.exception.BusinessException 如果组织有关联的认证记录
     *
     * Requirements: 4.4
     */
    void deleteOrganization(Long id);

    /**
     * 分页查询组织列表
     *
     * @param queryDTO 查询条件
     * @return 组织列表分页结果
     *
     * Requirements: 2.1, 2.2, 2.3, 2.4
     */
    Page<OrganizationListDTO> queryOrganizations(OrganizationQueryDTO queryDTO);

    /**
     * 获取组织详情（含认证统计）
     *
     * @param id 组织ID
     * @return 组织详情
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException 如果组织不存在
     *
     * Requirements: 7.1
     */
    OrganizationDetailDTO getOrganizationDetail(Long id);

    /**
     * 获取组织统计数据
     *
     * @return 组织统计
     *
     * Requirements: 2.5
     */
    OrganizationStatsDTO getOrganizationStats();

    // ==================== 用户端方法 ====================

    /**
     * 获取可选组织列表（仅ACTIVE状态）
     *
     * @param keyword 搜索关键词（可选）
     * @return 组织选择列表
     *
     * Requirements: 5.1, 5.2, 5.3, 5.4
     */
    List<OrganizationSelectDTO> getActiveOrganizations(String keyword);

    /**
     * 根据ID获取组织
     *
     * @param id 组织ID
     * @return 组织实体
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException 如果组织不存在
     */
    Organization getOrganizationById(Long id);

    /**
     * 检查组织是否为ACTIVE状态
     *
     * @param id 组织ID
     * @return true表示组织存在且为ACTIVE状态
     *
     * Requirements: 6.2
     */
    boolean isOrganizationActive(Long id);
}
