package com.learning.silveragepass.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 点赞结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeResultDTO {
    
    private Boolean isLiked; // 是否已点赞
    
    private Integer likeCount; // 点赞数
}

