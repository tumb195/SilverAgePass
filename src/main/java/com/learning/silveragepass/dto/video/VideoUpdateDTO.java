package com.learning.silveragepass.dto.video;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频更新 DTO
 * 用于管理员更新视频信息
 * 
 * Requirements: 4.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUpdateDTO {
    
    /**
     * 视频标题
     */
    @Size(max = 200, message = "视频标题不能超过200个字符")
    private String title;
    
    /**
     * 视频描述
     */
    @Size(max = 1000, message = "视频描述不能超过1000个字符")
    private String description;
    
    /**
     * OSS对象键（视频文件在OSS中的路径）
     */
    @Size(max = 500, message = "OSS对象键不能超过500个字符")
    private String ossKey;
    
    /**
     * 封面图URL
     */
    @Size(max = 500, message = "封面图URL不能超过500个字符")
    private String thumbnailUrl;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 分类ID
     */
    private Long categoryId;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
}
