package com.learning.silveragepass.dto.video;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 视频列表/详情 DTO
 * 用于返回视频基本信息
 * 
 * Requirements: 1.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDTO {
    
    /**
     * 视频ID
     */
    private Long id;
    
    /**
     * 视频标题
     */
    private String title;
    
    /**
     * 视频描述
     */
    private String description;
    
    /**
     * 封面图URL
     */
    private String thumbnailUrl;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    
    /**
     * 播放次数
     */
    private Long viewCount;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 是否已发布（管理端使用）
     */
    private Boolean published;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
