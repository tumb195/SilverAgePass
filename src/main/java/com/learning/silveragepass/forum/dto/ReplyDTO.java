package com.learning.silveragepass.forum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回复详情DTO
 */
@Data
public class ReplyDTO {
    
    private Long id;
    
    private Long commentId; // 评论ID
    
    private Long userId; // 回复用户ID
    
    private String username; // 回复用户名
    
    private String avatar; // 用户头像
    
    private Long replyToUserId; // 被回复用户ID
    
    private String replyToUsername; // 被回复用户名
    
    private String content; // 回复内容
    
    private Integer likeCount; // 点赞数
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间
}

