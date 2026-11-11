package com.learning.silveragepass.dto;

import lombok.Data;

/**
 * 搜索请求DTO
 */
@Data
public class SearchRequestDTO {
    
    private String keyword; // 搜索关键词
    
    private String type; // 搜索类型：user-用户, post-帖子, all-全部
    
    private Integer page = 0; // 页码，默认第0页
    
    private Integer size = 10; // 每页大小，默认10条
}

