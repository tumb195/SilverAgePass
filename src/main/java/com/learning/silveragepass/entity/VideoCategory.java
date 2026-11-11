package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 视频分类实体
 * 用于组织和筛选视频内容
 */
@Data
@Entity
@Table(name = "video_categories")
@NoArgsConstructor
@AllArgsConstructor
public class VideoCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 分类名称
     */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String name;
    
    /**
     * 分类描述
     */
    @Size(max = 200)
    @Column(length = 200)
    private String description;
    
    /**
     * 分类图标 URL
     */
    @Size(max = 500)
    @Column(name = "icon_url", length = 500)
    private String iconUrl;
    
    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    /**
     * 创建时设置默认值
     */
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
        if (this.enabled == null) {
            this.enabled = true;
        }
    }
    
    /**
     * 更新时自动更新时间
     */
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
