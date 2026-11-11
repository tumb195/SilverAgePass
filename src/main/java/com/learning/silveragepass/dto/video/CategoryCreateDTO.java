package com.learning.silveragepass.dto.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类创建 DTO
 * 用于管理员创建视频分类
 * 
 * Requirements: 3.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateDTO {
    
    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String name;
    
    /**
     * 分类描述
     */
    @Size(max = 200, message = "分类描述不能超过200个字符")
    private String description;
    
    /**
     * 分类图标URL
     */
    @Size(max = 500, message = "分类图标URL不能超过500个字符")
    private String iconUrl;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
}
