package com.learning.silveragepass.forum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 我的评论DTO
 */
@Data
public class MyCommentDTO {
    
    private Long id;
    
    private Long postId; // 帖子ID
    
    private String postTitle; // 帖子标题
    
    private String content; // 评论内容
    
    private List<String> images; // 图片URL列表
    
    private Integer likeCount; // 点赞数
    
    private Integer replyCount; // 回复数
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间
}

