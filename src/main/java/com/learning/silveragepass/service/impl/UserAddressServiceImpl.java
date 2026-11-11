package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.UserAddressCreateDTO;
import com.learning.silveragepass.dto.UserAddressDTO;
import com.learning.silveragepass.dto.UserAddressUpdateDTO;
import com.learning.silveragepass.entity.UserAddress;
import com.learning.silveragepass.exception.AddressException;
import com.learning.silveragepass.exception.AddressException.ErrorCode;
import com.learning.silveragepass.repository.UserAddressRepository;
import com.learning.silveragepass.service.UserAddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户地址服务实现类
 * Requirements: 1.2, 2.1, 3.1, 4.1, 5.1
 */
@Service
public class UserAddressServiceImpl implements UserAddressService {

    private static final Logger logger = LoggerFactory.getLogger(UserAddressServiceImpl.class);
    private static final int MAX_ADDRESSES_PER_USER = 20;

    private final UserAddressRepository userAddressRepository;

    @Autowired
    public UserAddressServiceImpl(UserAddressRepository userAddressRepository) {
        this.userAddressRepository = userAddressRepository;
    }

    /**
     * 将实体转换为DTO
     */
    private UserAddressDTO convertToDTO(UserAddress entity) {
        UserAddressDTO dto = new UserAddressDTO();
        dto.setId(entity.getId());
        dto.setLabel(entity.getLabel());
        dto.setAddress(entity.getAddress());
        dto.setLatitude(entity.getLatitude());
        dto.setLongitude(entity.getLongitude());
        dto.setIsDefault(entity.getIsDefault());
        dto.setLastUsedTime(entity.getLastUsedTime());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }

    // ==================== Address Retrieval Methods ====================
    // Requirements: 1.2, 5.2

    @Override
    public List<UserAddressDTO> getAddresses(Long userId) {
        logger.debug("获取用户 {} 的地址列表", userId);
        List<UserAddress> addresses = userAddressRepository.findByUserIdOrderByIsDefaultDescLastUsedTimeDesc(userId);
        return addresses.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserAddressDTO getAddressById(Long userId, Long addressId) {
        logger.debug("获取用户 {} 的地址 {}", userId, addressId);
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressException(ErrorCode.ADDRESS_NOT_FOUND,
                        "地址不存在或您无权访问该地址"));
        return convertToDTO(address);
    }

    @Override
    public UserAddressDTO getDefaultAddress(Long userId) {
        logger.debug("获取用户 {} 的默认地址", userId);
        return userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    // ==================== Address Creation Methods ====================
    // Requirements: 2.1, 2.2, 2.3, 2.4, 5.1

    @Override
    @Transactional
    public UserAddressDTO createAddress(Long userId, UserAddressCreateDTO dto) {
        logger.debug("为用户 {} 创建新地址: {}", userId, dto.getAddress());

        // 检查地址数量是否达到上限 (Requirements: 2.4)
        int currentCount = userAddressRepository.countByUserId(userId);
        if (currentCount >= MAX_ADDRESSES_PER_USER) {
            throw new AddressException(ErrorCode.ADDRESS_LIMIT_EXCEEDED,
                    "您已保存" + currentCount + "个地址，已达上限(最多20个)，请删除不常用的地址后再添加");
        }

        // 检查地址是否重复 (Requirements: 2.3)
        if (userAddressRepository.existsByUserIdAndAddress(userId, dto.getAddress())) {
            throw new AddressException(ErrorCode.ADDRESS_DUPLICATE,
                    "该地址已存在，如需修改请前往地址列表编辑");
        }

        // 验证经纬度坐标（如果提供）
        if (dto.getLatitude() != null || dto.getLongitude() != null) {
            validateCoordinates(dto.getLatitude(), dto.getLongitude());
        }

        // 创建新地址实体
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setAddress(dto.getAddress());
        address.setLabel(dto.getLabel());
        address.setLatitude(dto.getLatitude());
        address.setLongitude(dto.getLongitude());

        // 处理默认地址设置 (Requirements: 5.1)
        Boolean isDefault = dto.getIsDefault();
        if (Boolean.TRUE.equals(isDefault)) {
            // 清除之前的默认地址
            clearDefaultAddress(userId);
            address.setIsDefault(true);
        } else {
            address.setIsDefault(false);
        }

        UserAddress savedAddress = userAddressRepository.save(address);
        logger.info("用户 {} 成功创建地址 {}", userId, savedAddress.getId());
        return convertToDTO(savedAddress);
    }

    // ==================== Address Update Methods ====================
    // Requirements: 3.1, 3.2, 3.3, 5.1

    @Override
    @Transactional
    public UserAddressDTO updateAddress(Long userId, Long addressId, UserAddressUpdateDTO dto) {
        logger.debug("更新用户 {} 的地址 {}", userId, addressId);

        // 查找地址并验证所有权
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressException(ErrorCode.ADDRESS_NOT_FOUND,
                        "地址不存在或您无权修改该地址"));

        // 如果更新了地址字符串，检查是否与其他地址重复 (Requirements: 3.2)
        if (dto.getAddress() != null && !dto.getAddress().equals(address.getAddress())) {
            if (dto.getAddress().trim().isEmpty()) {
                throw new AddressException(ErrorCode.ADDRESS_EMPTY);
            }
            if (userAddressRepository.existsByUserIdAndAddressAndIdNot(userId, dto.getAddress(), addressId)) {
                throw new AddressException(ErrorCode.ADDRESS_DUPLICATE,
                        "该地址已存在于您的地址列表中，请勿重复添加");
            }
            address.setAddress(dto.getAddress());
        }

        // 验证经纬度坐标（如果提供）
        if (dto.getLatitude() != null || dto.getLongitude() != null) {
            Double lat = dto.getLatitude() != null ? dto.getLatitude() : address.getLatitude();
            Double lng = dto.getLongitude() != null ? dto.getLongitude() : address.getLongitude();
            validateCoordinates(lat, lng);
        }

        // 更新其他字段（部分更新）
        if (dto.getLabel() != null) {
            address.setLabel(dto.getLabel());
        }
        if (dto.getLatitude() != null) {
            address.setLatitude(dto.getLatitude());
        }
        if (dto.getLongitude() != null) {
            address.setLongitude(dto.getLongitude());
        }

        // 处理默认地址变更 (Requirements: 5.1)
        if (dto.getIsDefault() != null) {
            if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
                // 设置为默认地址，清除之前的默认
                clearDefaultAddress(userId);
                address.setIsDefault(true);
            } else if (Boolean.FALSE.equals(dto.getIsDefault())) {
                address.setIsDefault(false);
            }
        }

        // createTime 由 @PrePersist 设置且 updatable=false，会自动保留 (Requirements: 3.3)
        UserAddress savedAddress = userAddressRepository.save(address);
        logger.info("用户 {} 成功更新地址 {}", userId, addressId);
        return convertToDTO(savedAddress);
    }

    // ==================== Address Deletion Methods ====================
    // Requirements: 4.1, 4.2

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        logger.debug("删除用户 {} 的地址 {}", userId, addressId);

        // 查找地址并验证所有权
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressException(ErrorCode.ADDRESS_NOT_FOUND,
                        "地址不存在或您无权删除该地址"));

        // 直接删除，不自动重新分配默认地址 (Requirements: 4.2)
        userAddressRepository.delete(address);
        logger.info("用户 {} 成功删除地址 {}", userId, addressId);
    }

    // ==================== Default Address Methods ====================
    // Requirements: 5.1

    @Override
    @Transactional
    public UserAddressDTO setDefaultAddress(Long userId, Long addressId) {
        logger.debug("设置用户 {} 的默认地址为 {}", userId, addressId);

        // 查找地址并验证所有权
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressException(ErrorCode.ADDRESS_NOT_FOUND,
                        "地址不存在或您无权设置该地址为默认"));

        // 清除之前的默认地址
        clearDefaultAddress(userId);

        // 设置新的默认地址
        address.setIsDefault(true);
        UserAddress savedAddress = userAddressRepository.save(address);
        logger.info("用户 {} 成功设置默认地址 {}", userId, addressId);
        return convertToDTO(savedAddress);
    }

    /**
     * 清除用户的默认地址标记
     * Requirements: 5.1 - 确保单一默认地址不变量
     */
    private void clearDefaultAddress(Long userId) {
        userAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(defaultAddress -> {
                    defaultAddress.setIsDefault(false);
                    userAddressRepository.save(defaultAddress);
                    logger.debug("清除用户 {} 的原默认地址 {}", userId, defaultAddress.getId());
                });
    }

    // ==================== Last Used Time Methods ====================
    // Requirements: 1.2

    @Override
    @Transactional
    public void updateLastUsedTime(Long userId, Long addressId) {
        logger.debug("更新用户 {} 地址 {} 的最后使用时间", userId, addressId);

        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressException(ErrorCode.ADDRESS_NOT_FOUND));

        address.setLastUsedTime(LocalDateTime.now());
        userAddressRepository.save(address);
        logger.debug("用户 {} 地址 {} 的最后使用时间已更新", userId, addressId);
    }

    @Override
    public boolean existsByUserIdAndAddress(Long userId, String address) {
        return userAddressRepository.existsByUserIdAndAddress(userId, address);
    }

    @Override
    @Transactional
    public void updateLastUsedTimeByAddress(Long userId, String address) {
        logger.debug("更新用户 {} 地址 {} 的最后使用时间", userId, address);

        UserAddress userAddress = userAddressRepository.findByUserIdAndAddress(userId, address)
                .orElseThrow(() -> new AddressException(ErrorCode.ADDRESS_NOT_FOUND));

        userAddress.setLastUsedTime(LocalDateTime.now());
        userAddressRepository.save(userAddress);
        logger.debug("用户 {} 地址 {} 的最后使用时间已更新", userId, address);
    }

    /**
     * 验证经纬度坐标的有效性
     * 纬度范围: -90 到 90
     * 经度范围: -180 到 180
     */
    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new AddressException(ErrorCode.INVALID_COORDINATES,
                    "纬度值无效，有效范围为 -90 到 90");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new AddressException(ErrorCode.INVALID_COORDINATES,
                    "经度值无效，有效范围为 -180 到 180");
        }
        // 如果只提供了一个坐标，提示需要同时提供
        if ((latitude != null && longitude == null) || (latitude == null && longitude != null)) {
            throw new AddressException(ErrorCode.INVALID_COORDINATES,
                    "请同时提供经度和纬度坐标");
        }
    }

    // ==================== Proxy Address Methods ====================
    // Requirements: 2.1, 2.2, 2.3, 4.1, 4.2, 4.3

    @Autowired
    private com.learning.silveragepass.service.ProxyAuthorizationService proxyAuthorizationService;

    @Autowired
    private com.learning.silveragepass.service.MessageService messageService;

    @Autowired
    private com.learning.silveragepass.repository.UserRepository userRepository;

    @Override
    @Transactional
    public UserAddressDTO createAddressByProxy(Long relativeId, Long elderId, UserAddressCreateDTO dto) {
        logger.debug("亲属 {} 为老人 {} 代理添加地址: {}", relativeId, elderId, dto.getAddress());

        // 验证代理授权
        if (!proxyAuthorizationService.hasValidAuthorization(relativeId, elderId)) {
            throw new AddressException(ErrorCode.ADDRESS_NOT_FOUND, "您没有代理授权，无法为该老人添加地址");
        }

        // 创建地址
        UserAddressDTO createdAddress = createAddress(elderId, dto);

        // 发送通知给老人
        sendAddressNotification(relativeId, elderId, dto.getAddress(), dto.getLabel());

        logger.info("亲属 {} 成功为老人 {} 添加地址 {}", relativeId, elderId, createdAddress.getId());
        return createdAddress;
    }

    @Override
    @Transactional
    public UserAddressDTO createAddressFromLocation(Long relativeId, Long elderId,
            Double latitude, Double longitude, String address, String label) {
        logger.debug("亲属 {} 从位置为老人 {} 添加地址: lat={}, lng={}", relativeId, elderId, latitude, longitude);

        // 验证代理授权
        if (!proxyAuthorizationService.hasValidAuthorization(relativeId, elderId)) {
            throw new AddressException(ErrorCode.ADDRESS_NOT_FOUND, "您没有代理授权，无法为该老人添加地址");
        }

        // 创建地址DTO
        UserAddressCreateDTO dto = new UserAddressCreateDTO();
        dto.setAddress(address);
        dto.setLabel(label != null ? label : "当前位置");
        dto.setLatitude(latitude);
        dto.setLongitude(longitude);

        // 创建地址
        UserAddressDTO createdAddress = createAddress(elderId, dto);

        // 发送通知给老人
        sendAddressNotification(relativeId, elderId, address, label);

        logger.info("亲属 {} 成功从位置为老人 {} 添加地址 {}", relativeId, elderId, createdAddress.getId());
        return createdAddress;
    }

    /**
     * 发送地址添加通知给老人
     */
    private void sendAddressNotification(Long relativeId, Long elderId, String address, String label) {
        try {
            com.learning.silveragepass.entity.User relative = userRepository.findById(relativeId).orElse(null);
            String relativeName = relative != null ?
                    (relative.getRealName() != null ? relative.getRealName() : relative.getUsername()) : "您的亲属";

            String title = "地址簿更新通知";
            String labelInfo = label != null && !label.isEmpty() ? "（" + label + "）" : "";
            String content = String.format("%s 为您添加了新地址%s：%s", relativeName, labelInfo, address);

            messageService.sendSystemMessage(title, content, elderId);
        } catch (Exception e) {
            logger.warn("发送地址添加通知失败: {}", e.getMessage());
        }
    }
}
