package com.learning.silveragepass.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 关注信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowDTO {
    
    private Long userId; // 用户ID
    
    private String username; // 用户名
    
    private String avatar; // 头像
    
    private String realName; // 真实姓名（可选）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime followTime; // 关注时间
    
    private Boolean isFollowing; // 当前用户是否已关注此用户
    
    private Boolean isFollower; // 此用户是否关注了当前用户（是否互相关注）
    
    private Long followerCount; // 粉丝数量
    
    private Long followingCount; // 关注数量
}

