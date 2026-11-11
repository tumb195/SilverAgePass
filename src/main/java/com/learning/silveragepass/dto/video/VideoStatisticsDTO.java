package com.learning.silveragepass.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 视频统计 DTO
 * 用于返回视频统计信息
 * 
 * Requirements: 7.3, 7.4, 7.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoStatisticsDTO {
    
    /**
     * 视频总数
     */
    private Long totalVideos;
    
    /**
     * 总播放次数
     */
    private Long totalViews;
    
    /**
     * 总独立观众数
     */
    private Long totalUniqueViewers;
    
    /**
     * 各分类统计
     */
    private List<CategoryStatDTO> categoryStats;
    
    /**
     * 分类统计 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStatDTO {
        
        /**
         * 分类ID
         */
        private Long categoryId;
        
        /**
         * 分类名称
         */
        private String categoryName;
        
        /**
         * 视频数量
         */
        private Long videoCount;
        
        /**
         * 播放次数
         */
        private Long viewCount;
        
        /**
         * 独立观众数
         */
        private Long uniqueViewers;
    }
}
