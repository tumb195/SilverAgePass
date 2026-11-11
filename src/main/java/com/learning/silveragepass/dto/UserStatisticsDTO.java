package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户统计信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDTO {
    
    private Long postCount; // 发帖数
    
    private Long followerCount; // 粉丝数
    
    private Long followingCount; // 关注数
    
    private Long favoriteCount; // 收藏帖子数
}

