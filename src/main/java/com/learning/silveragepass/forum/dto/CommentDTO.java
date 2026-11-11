package com.learning.silveragepass.forum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论详情DTO
 */
@Data
public class CommentDTO {
    
    private Long id;
    
    private Long postId; // 帖子ID
    
    private Long userId; // 评论用户ID
    
    private String username; // 评论用户名
    
    private String avatar; // 用户头像
    
    private String content; // 评论内容
    
    private List<String> images; // 图片URL列表
    
    private Integer likeCount; // 点赞数
    
    private Integer replyCount; // 回复数
    
    private Boolean isLiked; // 当前用户是否已点赞
    
    private List<ReplyDTO> replies; // 回复列表
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间
}

