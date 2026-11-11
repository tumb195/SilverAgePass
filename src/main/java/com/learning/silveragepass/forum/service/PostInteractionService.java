package com.learning.silveragepass.forum.service;

import com.learning.silveragepass.forum.dto.LikeResultDTO;

/**
 * 帖子互动服务接口（点赞、收藏）
 */
public interface PostInteractionService {
    
    /**
     * 点赞/取消点赞帖子
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 点赞结果
     */
    LikeResultDTO toggleLike(Long postId, Long userId);
    
    /**
     * 收藏/取消收藏帖子
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 收藏结果
     */
    LikeResultDTO toggleFavorite(Long postId, Long userId);
}

