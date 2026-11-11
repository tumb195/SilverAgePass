package com.learning.silveragepass.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.forum.entity.PostType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子搜索结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchResultDTO {
    
    private Long id; // 帖子ID
    
    private String title; // 帖子标题
    
    private String content; // 帖子内容（摘要，最多200字符）
    
    private PostType type; // 帖子类型
    
    private Long userId; // 发帖用户ID
    
    private String username; // 发帖用户名
    
    private String userAvatar; // 发帖用户头像
    
    private Integer likeCount; // 点赞数
    
    private Integer commentCount; // 评论数
    
    private Integer favoriteCount; // 收藏数
    
    private Integer viewCount; // 浏览数
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间
}

