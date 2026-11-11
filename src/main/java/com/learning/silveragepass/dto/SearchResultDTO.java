package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一搜索结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    
    private List<UserSearchResultDTO> users; // 用户搜索结果
    
    private Long userTotal; // 用户总数
    
    private List<PostSearchResultDTO> posts; // 帖子搜索结果
    
    private Long postTotal; // 帖子总数
}

