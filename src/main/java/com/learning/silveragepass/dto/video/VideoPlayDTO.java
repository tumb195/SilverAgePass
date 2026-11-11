package com.learning.silveragepass.dto.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频播放 DTO
 * 用于返回视频播放信息，包含签名URL和上次观看位置
 * 
 * Requirements: 2.1, 2.2, 6.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPlayDTO {
    
    /**
     * 视频ID
     */
    private Long id;
    
    /**
     * 视频标题
     */
    private String title;
    
    /**
     * 签名URL（带过期时间的临时访问链接）
     */
    private String playUrl;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    
    /**
     * 上次观看位置（秒）
     */
    private Integer lastPosition;
    
    /**
     * URL过期时间戳（毫秒）
     */
    private Long expiresAt;
}
