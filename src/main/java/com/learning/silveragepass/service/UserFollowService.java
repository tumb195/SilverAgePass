package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.FollowDTO;
import com.learning.silveragepass.dto.FollowResultDTO;
import com.learning.silveragepass.dto.FollowStatisticsDTO;
import com.learning.silveragepass.dto.PageResponse;

/**
 * 用户关注服务接口
 */
public interface UserFollowService {
    
    /**
     * 关注/取消关注用户
     * @param followerId 关注者ID
     * @param followedId 被关注者ID
     * @return 关注操作结果
     */
    FollowResultDTO toggleFollow(Long followerId, Long followedId);
    
    /**
     * 获取用户的关注列表（我关注的人）
     * @param userId 用户ID
     * @param currentUserId 当前登录用户ID（用于判断关注状态）
     * @param page 页码
     * @param size 每页大小
     * @return 关注列表
     */
    PageResponse<FollowDTO> getFollowingList(Long userId, Long currentUserId, int page, int size);
    
    /**
     * 获取用户的粉丝列表（关注我的人）
     * @param userId 用户ID
     * @param currentUserId 当前登录用户ID（用于判断关注状态）
     * @param page 页码
     * @param size 每页大小
     * @return 粉丝列表
     */
    PageResponse<FollowDTO> getFollowersList(Long userId, Long currentUserId, int page, int size);
    
    /**
     * 检查是否已关注某用户
     * @param followerId 关注者ID
     * @param followedId 被关注者ID
     * @return 是否已关注
     */
    boolean isFollowing(Long followerId, Long followedId);
    
    /**
     * 获取用户的关注统计信息
     * @param userId 用户ID
     * @return 关注统计信息
     */
    FollowStatisticsDTO getFollowStatistics(Long userId);
}

