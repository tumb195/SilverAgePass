package com.learning.silveragepass.dto.video;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 观看历史 DTO
 * 用于返回用户的观看历史记录
 * 
 * Requirements: 6.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistoryDTO {
    
    /**
     * 视频ID
     */
    private Long videoId;
    
    /**
     * 视频标题
     */
    private String title;
    
    /**
     * 封面图URL
     */
    private String thumbnailUrl;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    
    /**
     * 上次观看位置（秒）
     */
    private Integer lastPosition;
    
    /**
     * 最后观看时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastWatchTime;
}
