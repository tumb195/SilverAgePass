package com.learning.silveragepass.forum.dto;

import com.learning.silveragepass.forum.entity.PostType;
import lombok.Data;

/**
 * 帖子查询DTO
 */
@Data
public class PostQueryDTO {
    
    private PostType type; // 帖子类型（可选）
    
    private String keyword; // 搜索关键词（可选）
    
    private Integer page = 0; // 页码，默认0
    
    private Integer size = 10; // 每页数量，默认10
}

