package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.UserPublicDTO;
import com.learning.silveragepass.dto.UserStatisticsDTO;
import com.learning.silveragepass.dto.UserUpdateDTO;
import com.learning.silveragepass.entity.User;

import java.util.Optional;

public interface UserService {
    
    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户
     */
    User getUserById(Long id);
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户
     */
    Optional<User> getUserByUsername(String username);
    
    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户
     */
    Optional<User> getUserByPhone(String phone);
    
    /**
     * 创建用户
     * @param user 用户信息
     * @return 创建的用户
     */
    User createUser(User user);
    
    /**
     * 更新用户信息（旧方法，保留用于内部调用）
     * @param id 用户ID
     * @param user 用户信息
     * @return 更新后的用户
     */
    User updateUser(Long id, User user);
    
    /**
     * 更新用户信息（使用DTO）
     * @param id 用户ID
     * @param userUpdateDTO 用户更新信息
     * @return 更新后的用户
     */
    User updateUserInfo(Long id, UserUpdateDTO userUpdateDTO);
    
    /**
     * 更新用户暖龄币
     * @param userId 用户ID
     * @param amount 暖龄币数量（正数增加，负数减少）
     * @return 更新后的用户
     */
    User updateWarmCoin(Long userId, Integer amount);
    
    /**
     * 获取用户公开信息（不包含敏感信息）
     * @param userId 用户ID
     * @return 用户公开信息
     */
    UserPublicDTO getUserPublicInfo(Long userId);
    
    /**
     * 获取用户统计信息
     * @param userId 用户ID
     * @return 用户统计信息（发帖数、粉丝数、关注数、收藏数）
     */
    UserStatisticsDTO getUserStatistics(Long userId);
} 