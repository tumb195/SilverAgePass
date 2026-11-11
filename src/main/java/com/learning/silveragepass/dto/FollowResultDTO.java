package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注操作结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowResultDTO {
    
    private Boolean isFollowing; // 是否已关注
    
    private Long followerCount; // 被关注者的粉丝数量
}

