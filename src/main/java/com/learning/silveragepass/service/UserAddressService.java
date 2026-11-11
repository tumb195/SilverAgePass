package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.UserAddressCreateDTO;
import com.learning.silveragepass.dto.UserAddressDTO;
import com.learning.silveragepass.dto.UserAddressUpdateDTO;

import java.util.List;

/**
 * 用户地址服务接口
 * Requirements: 1.2, 2.1, 3.1, 4.1, 5.1
 */
public interface UserAddressService {

    /**
     * 获取用户的地址列表
     * 按默认地址优先、最后使用时间降序排列
     * Requirements: 1.2, 5.2
     *
     * @param userId 用户ID
     * @return 地址列表
     */
    List<UserAddressDTO> getAddresses(Long userId);

    /**
     * 根据ID获取地址详情
     * Requirements: 1.2
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     * @return 地址详情
     */
    UserAddressDTO getAddressById(Long userId, Long addressId);

    /**
     * 创建新地址
     * Requirements: 2.1, 2.2, 2.3, 2.4, 5.1
     *
     * @param userId 用户ID
     * @param dto    创建地址请求
     * @return 创建的地址
     */
    UserAddressDTO createAddress(Long userId, UserAddressCreateDTO dto);

    /**
     * 更新地址
     * Requirements: 3.1, 3.2, 3.3, 5.1
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     * @param dto       更新地址请求
     * @return 更新后的地址
     */
    UserAddressDTO updateAddress(Long userId, Long addressId, UserAddressUpdateDTO dto);

    /**
     * 删除地址
     * Requirements: 4.1, 4.2
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 设置默认地址
     * Requirements: 5.1
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     * @return 设置后的地址
     */
    UserAddressDTO setDefaultAddress(Long userId, Long addressId);

    /**
     * 获取用户的默认地址
     * Requirements: 5.3
     *
     * @param userId 用户ID
     * @return 默认地址，如果没有则返回null
     */
    UserAddressDTO getDefaultAddress(Long userId);

    /**
     * 更新地址的最后使用时间
     * Requirements: 1.2
     *
     * @param userId    用户ID
     * @param addressId 地址ID
     */
    void updateLastUsedTime(Long userId, Long addressId);

    /**
     * 检查用户是否已存在相同地址
     *
     * @param userId  用户ID
     * @param address 地址字符串
     * @return 是否存在
     */
    boolean existsByUserIdAndAddress(Long userId, String address);

    /**
     * 根据地址字符串更新地址的最后使用时间
     *
     * @param userId  用户ID
     * @param address 地址字符串
     */
    void updateLastUsedTimeByAddress(Long userId, String address);

    /**
     * 代理添加地址
     * 亲属为老人添加地址到地址簿
     * Requirements: 2.1, 2.2, 2.3
     *
     * @param relativeId 亲属用户ID
     * @param elderId    老人用户ID
     * @param dto        地址创建请求
     * @return 创建的地址
     */
    UserAddressDTO createAddressByProxy(Long relativeId, Long elderId, UserAddressCreateDTO dto);

    /**
     * 从位置信息代理添加地址
     * 将老人当前位置添加到地址簿
     * Requirements: 4.1, 4.2, 4.3
     *
     * @param relativeId 亲属用户ID
     * @param elderId    老人用户ID
     * @param latitude   纬度
     * @param longitude  经度
     * @param address    地址
     * @param label      标签
     * @return 创建的地址
     */
    UserAddressDTO createAddressFromLocation(Long relativeId, Long elderId, 
            Double latitude, Double longitude, String address, String label);
}
