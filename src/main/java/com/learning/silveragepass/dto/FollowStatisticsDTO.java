package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatisticsDTO {
    
    private Long userId; // 用户ID
    
    private Long followingCount; // 关注数量
    
    private Long followerCount; // 粉丝数量
    
    private Long mutualFollowCount; // 互相关注数量
}

