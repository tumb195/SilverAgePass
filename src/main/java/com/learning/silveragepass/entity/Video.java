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
 * 视频实体
 * 存储视频元数据，视频文件存储在阿里云 OSS
 */
@Data
@Entity
@Table(name = "videos")
@NoArgsConstructor
@AllArgsConstructor
public class Video {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 视频标题
     */
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;
    
    /**
     * 视频描述
     */
    @Size(max = 1000)
    @Column(length = 1000)
    private String description;
    
    /**
     * OSS 对象键（视频文件在 OSS 中的路径）
     */
    @NotBlank
    @Size(max = 500)
    @Column(name = "oss_key", nullable = false, length = 500)
    private String ossKey;
    
    /**
     * 封面图 URL
     */
    @Size(max = 500)
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
    
    /**
     * 视频时长（秒）
     */
    @Column
    private Integer duration;
    
    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * 所属分类
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private VideoCategory category;
    
    /**
     * 播放次数
     */
    @Column(name = "view_count")
    private Long viewCount = 0L;
    
    /**
     * 独立观众数
     */
    @Column(name = "unique_viewers")
    private Long uniqueViewers = 0L;
    
    /**
     * 是否发布
     */
    @Column(nullable = false)
    private Boolean published = false;
    
    /**
     * 是否删除（软删除）
     */
    @Column(nullable = false)
    private Boolean deleted = false;
    
    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
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
        if (this.viewCount == null) {
            this.viewCount = 0L;
        }
        if (this.uniqueViewers == null) {
            this.uniqueViewers = 0L;
        }
        if (this.published == null) {
            this.published = false;
        }
        if (this.deleted == null) {
            this.deleted = false;
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }
    
    /**
     * 更新时自动更新时间
     */
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 增加播放次数
     */
    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0L : this.viewCount) + 1;
    }
    
    /**
     * 增加独立观众数
     */
    public void incrementUniqueViewers() {
        this.uniqueViewers = (this.uniqueViewers == null ? 0L : this.uniqueViewers) + 1;
    }
}
