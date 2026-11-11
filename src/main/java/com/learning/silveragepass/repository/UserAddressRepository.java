package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户地址仓库接口
 * Requirements: 1.2, 2.3, 2.4, 5.1, 5.2
 */
@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    /**
     * 获取用户的地址列表，按默认地址优先、最后使用时间降序排列
     * Requirements: 1.2, 5.2
     * 
     * @param userId 用户ID
     * @return 排序后的地址列表
     */
    List<UserAddress> findByUserIdOrderByIsDefaultDescLastUsedTimeDesc(Long userId);

    /**
     * 根据地址ID和用户ID查找地址（确保用户只能访问自己的地址）
     * 
     * @param id     地址ID
     * @param userId 用户ID
     * @return 地址（如果存在且属于该用户）
     */
    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    /**
     * 检查用户是否已存在相同地址（用于创建时的重复检查）
     * Requirements: 2.3
     * 
     * @param userId  用户ID
     * @param address 地址字符串
     * @return 是否存在
     */
    boolean existsByUserIdAndAddress(Long userId, String address);

    /**
     * 根据用户ID和地址字符串查找地址
     * 
     * @param userId  用户ID
     * @param address 地址字符串
     * @return 地址（如果存在）
     */
    Optional<UserAddress> findByUserIdAndAddress(Long userId, String address);

    /**
     * 检查用户是否已存在相同地址（排除指定ID，用于更新时的重复检查）
     * Requirements: 2.3
     * 
     * @param userId  用户ID
     * @param address 地址字符串
     * @param id      要排除的地址ID
     * @return 是否存在
     */
    boolean existsByUserIdAndAddressAndIdNot(Long userId, String address, Long id);

    /**
     * 统计用户的地址数量（用于检查是否达到上限）
     * Requirements: 2.4
     * 
     * @param userId 用户ID
     * @return 地址数量
     */
    int countByUserId(Long userId);

    /**
     * 查找用户的默认地址
     * Requirements: 5.1
     * 
     * @param userId 用户ID
     * @return 默认地址（如果存在）
     */
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);
}
