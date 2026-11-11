package com.learning.silveragepass.service;

import java.util.List;

/**
 * 健康数据授权服务接口
 * 负责验证用户对健康数据的访问权限
 */
public interface HealthAuthorizationService {
    
    /**
     * 检查当前用户是否可以访问目标用户的健康数据
     * 
     * @param currentUserId 当前用户ID
     * @param targetUserId 目标用户ID
     * @return 如果当前用户可以访问目标用户的健康数据则返回true
     */
    boolean canAccessUserHealthData(Long currentUserId, Long targetUserId);
    
    /**
     * 检查两个用户之间是否存在已接受的亲属关系
     * 
     * @param userId 用户ID
     * @param relativeId 亲属用户ID
     * @return 如果存在已接受的亲属关系则返回true
     */
    boolean isAcceptedRelative(Long userId, Long relativeId);
    
    /**
     * 获取当前用户可以访问健康数据的所有用户ID列表
     * 包括用户自己和所有已接受关系的亲属
     * 
     * @param currentUserId 当前用户ID
     * @return 可访问的用户ID列表
     */
    List<Long> getAccessibleUserIds(Long currentUserId);
}
