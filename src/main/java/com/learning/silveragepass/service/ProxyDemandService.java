package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.DemandDTO;
import com.learning.silveragepass.dto.proxy.ProxyAddressListDTO;
import com.learning.silveragepass.dto.proxy.ProxyDemandCreateDTO;

/**
 * 代发需求服务接口
 * 管理联系人代替老人发布需求的功能
 * Requirements: 3.1, 4.1
 */
public interface ProxyDemandService {

    /**
     * 代发需求
     * 已授权的联系人代替老人发布需求
     * Requirements: 3.1, 3.2, 3.3, 3.4, 4.2, 4.3, 4.4
     *
     * @param relativeId 联系人用户ID
     * @param elderId    老人用户ID
     * @param dto        代发需求创建请求
     * @return 创建的需求DTO
     */
    DemandDTO createProxyDemand(Long relativeId, Long elderId, ProxyDemandCreateDTO dto);

    /**
     * 获取可用地址列表
     * 返回老人和联系人双方的地址簿
     * Requirements: 4.1, 4.2, 4.3
     *
     * @param relativeId 联系人用户ID
     * @param elderId    老人用户ID
     * @return 可用地址列表（包含老人和联系人的地址）
     */
    ProxyAddressListDTO getAvailableAddresses(Long relativeId, Long elderId);
}
