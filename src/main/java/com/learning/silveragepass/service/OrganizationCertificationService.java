package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.certification.*;
import com.learning.silveragepass.entity.OrganizationCertification;
import org.springframework.data.domain.Page;

/**
 * 组织认证服务接口
 * 提供用户端和管理端的认证相关操作
 */
public interface OrganizationCertificationService {

    // ==================== 用户端方法 ====================

    /**
     * 提交认证申请
     * 
     * @param userId 用户ID
     * @param dto 认证提交信息
     * @return 创建的认证记录
     * @throws com.learning.silveragepass.exception.BusinessException 如果已有待审核或已通过的认证
     * 
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
     */
    OrganizationCertification submitCertification(Long userId, CertificationSubmitDTO dto);

    /**
     * 查询用户认证状态
     * 
     * @param userId 用户ID
     * @return 认证信息DTO，如果没有认证记录则返回null
     * 
     * Requirements: 2.1, 2.2, 2.3
     */
    OrganizationCertificationDTO getCertificationByUserId(Long userId);

    /**
     * 检查用户是否已通过认证
     * 
     * @param userId 用户ID
     * @return true表示已通过认证，false表示未通过
     * 
     * Requirements: 5.1
     */
    boolean isUserCertified(Long userId);

    // ==================== 管理端方法 ====================

    /**
     * 分页查询认证列表
     * 支持按状态和关键词过滤
     * 
     * @param queryDTO 查询条件
     * @return 认证列表分页结果（包含用户信息）
     * 
     * Requirements: 3.3, 4.1, 4.2, 4.3, 4.4
     */
    Page<CertificationListDTO> queryCertifications(CertificationQueryDTO queryDTO);

    /**
     * 根据ID查询认证详情
     * 
     * @param certificationId 认证ID
     * @return 认证列表DTO（包含用户信息）
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException 如果认证不存在
     * 
     * Requirements: 4.4
     */
    CertificationListDTO getCertificationById(Long certificationId);

    /**
     * 审核通过认证申请
     * 
     * @param certificationId 认证ID
     * @param adminId 审核管理员ID
     * @return 更新后的认证记录
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException 如果认证不存在
     * @throws com.learning.silveragepass.exception.BusinessException 如果认证状态不是待审核
     * 
     * Requirements: 3.1, 3.4
     */
    OrganizationCertification approveCertification(Long certificationId, Long adminId);

    /**
     * 审核拒绝认证申请
     * 
     * @param certificationId 认证ID
     * @param adminId 审核管理员ID
     * @param reason 拒绝原因
     * @return 更新后的认证记录
     * @throws com.learning.silveragepass.exception.ResourceNotFoundException 如果认证不存在
     * @throws com.learning.silveragepass.exception.BusinessException 如果认证状态不是待审核
     * 
     * Requirements: 3.2, 3.4
     */
    OrganizationCertification rejectCertification(Long certificationId, Long adminId, String reason);

    /**
     * 获取认证统计数据
     * 
     * @return 认证统计DTO
     * 
     * Requirements: 4.1
     */
    CertificationStatsDTO getCertificationStats();
}
