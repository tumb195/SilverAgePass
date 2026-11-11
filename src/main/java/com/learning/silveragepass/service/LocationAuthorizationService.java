package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.location.LocationAuthorizationDTO;

import java.util.List;

/**
 * 定位授权服务接口
 * 管理亲属获取老人实时位置的授权关系
 * 定位授权独立于代理授权，需要单独申请
 * Requirements: 5.1, 5.3, 5.4, 5.5
 */
public interface LocationAuthorizationService {

    /**
     * 申请定位授权
     * 亲属向老人申请获取实时位置的授权
     * Requirements: 5.1
     *
     * @param relativeId 亲属用户ID
     * @param elderId    老人用户ID
     * @return 授权信息DTO
     */
    LocationAuthorizationDTO requestAuthorization(Long relativeId, Long elderId);

    /**
     * 批准定位授权申请
     * 老人批准亲属的定位授权申请
     * Requirements: 5.3
     *
     * @param elderId         老人用户ID
     * @param authorizationId 授权ID
     * @return 授权信息DTO
     */
    LocationAuthorizationDTO approveAuthorization(Long elderId, Long authorizationId);

    /**
     * 拒绝定位授权申请
     * 老人拒绝亲属的定位授权申请
     * Requirements: 5.4
     *
     * @param elderId         老人用户ID
     * @param authorizationId 授权ID
     * @return 授权信息DTO
     */
    LocationAuthorizationDTO rejectAuthorization(Long elderId, Long authorizationId);

    /**
     * 撤销定位授权
     * 老人撤销已批准的定位授权
     * Requirements: 5.5
     *
     * @param elderId         老人用户ID
     * @param authorizationId 授权ID
     * @return 授权信息DTO
     */
    LocationAuthorizationDTO revokeAuthorization(Long elderId, Long authorizationId);

    /**
     * 检查是否有有效的定位授权
     * 检查亲属是否有获取老人位置的有效授权
     * Requirements: 3.1
     *
     * @param relativeId 亲属用户ID
     * @param elderId    老人用户ID
     * @return 是否有有效授权
     */
    boolean hasValidAuthorization(Long relativeId, Long elderId);

    /**
     * 获取亲属的定位授权列表
     * 亲属查看自己申请的所有定位授权
     *
     * @param relativeId 亲属用户ID
     * @return 授权列表
     */
    List<LocationAuthorizationDTO> getRelativeAuthorizations(Long relativeId);

    /**
     * 获取老人收到的定位授权申请（待审批）
     *
     * @param elderId 老人用户ID
     * @return 待处理的授权申请列表
     */
    List<LocationAuthorizationDTO> getElderAuthorizationRequests(Long elderId);

    /**
     * 获取老人已批准的定位授权列表
     *
     * @param elderId 老人用户ID
     * @return 已批准的授权列表
     */
    List<LocationAuthorizationDTO> getElderApprovedAuthorizations(Long elderId);
}
