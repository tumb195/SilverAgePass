package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.proxy.ProxyAuthorizationDTO;

import java.util.List;

/**
 * 代发授权服务接口
 * 管理联系人代替老人发布需求的授权关系
 * Requirements: 1.1, 2.1, 2.2, 2.3
 */
public interface ProxyAuthorizationService {

    /**
     * 申请代发授权
     * 联系人向老人申请代发需求的授权
     * Requirements: 1.1, 1.2, 1.3
     *
     * @param relativeId 联系人用户ID
     * @param elderId    老人用户ID
     * @return 授权信息DTO
     */
    ProxyAuthorizationDTO requestAuthorization(Long relativeId, Long elderId);

    /**
     * 批准授权申请
     * 老人批准联系人的代发授权申请
     * Requirements: 2.1
     *
     * @param elderId         老人用户ID
     * @param authorizationId 授权ID
     * @return 授权信息DTO
     */
    ProxyAuthorizationDTO approveAuthorization(Long elderId, Long authorizationId);

    /**
     * 拒绝授权申请
     * 老人拒绝联系人的代发授权申请
     * Requirements: 2.2
     *
     * @param elderId         老人用户ID
     * @param authorizationId 授权ID
     * @return 授权信息DTO
     */
    ProxyAuthorizationDTO rejectAuthorization(Long elderId, Long authorizationId);

    /**
     * 撤销授权
     * 老人撤销已批准的代发授权
     * Requirements: 2.3
     *
     * @param elderId         老人用户ID
     * @param authorizationId 授权ID
     * @return 授权信息DTO
     */
    ProxyAuthorizationDTO revokeAuthorization(Long elderId, Long authorizationId);

    /**
     * 检查是否有有效授权
     * 检查联系人是否有代发老人需求的有效授权
     * Requirements: 1.1, 2.1
     *
     * @param relativeId 联系人用户ID
     * @param elderId    老人用户ID
     * @return 是否有有效授权
     */
    boolean hasValidAuthorization(Long relativeId, Long elderId);

    /**
     * 获取联系人的授权列表
     * 联系人查看自己申请的所有授权
     * Requirements: 1.1
     *
     * @param relativeId 联系人用户ID
     * @return 授权列表
     */
    List<ProxyAuthorizationDTO> getRelativeAuthorizations(Long relativeId);

    /**
     * 获取老人收到的授权申请
     * 老人查看待处理的授权申请
     * Requirements: 2.1
     *
     * @param elderId 老人用户ID
     * @return 待处理的授权申请列表
     */
    List<ProxyAuthorizationDTO> getElderAuthorizationRequests(Long elderId);

    /**
     * 获取老人已批准的授权列表
     * 老人查看已批准的授权
     * Requirements: 2.1
     *
     * @param elderId 老人用户ID
     * @return 已批准的授权列表
     */
    List<ProxyAuthorizationDTO> getElderApprovedAuthorizations(Long elderId);
}
