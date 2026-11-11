package com.learning.silveragepass.forum.dto;

import com.learning.silveragepass.forum.entity.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建帖子DTO
 */
@Data
public class PostCreateDTO {
    
    @NotBlank(message = "帖子标题不能为空")
    @Size(max = 200, message = "帖子标题不能超过200个字符")
    private String title;
    
    @NotBlank(message = "帖子内容不能为空")
    @Size(max = 10000, message = "帖子内容不能超过10000个字符")
    private String content;
    
    @NotNull(message = "帖子类型不能为空")
    private PostType type;
}

