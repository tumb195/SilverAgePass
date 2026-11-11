package com.learning.silveragepass.forum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.forum.entity.PostType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子详情DTO
 */
@Data
public class PostDTO {
    
    private Long id;
    
    private Long userId; // 发帖用户ID
    
    private String username; // 发帖用户名
    
    private String avatar; // 用户头像
    
    private PostType type; // 帖子类型
    
    private String title; // 帖子标题
    
    private String content; // 帖子内容
    
    private List<String> images; // 图片URL列表
    
    private String videoUrl; // 视频URL
    
    private Integer likeCount; // 点赞数
    
    private Integer commentCount; // 评论数
    
    private Integer favoriteCount; // 收藏数
    
    private Integer viewCount; // 浏览数
    
    private Boolean isLiked; // 当前用户是否已点赞
    
    private Boolean isFavorited; // 当前用户是否已收藏
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间
}

