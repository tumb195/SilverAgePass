package com.learning.silveragepass.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建回复DTO
 */
@Data
public class ReplyCreateDTO {
    
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 200, message = "回复内容不能超过200个字符")
    private String content;
    
    @NotNull(message = "被回复用户ID不能为空")
    private Long replyToUserId; // 被回复的用户ID
}

