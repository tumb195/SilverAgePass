package com.learning.silveragepass.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子实体类
 */
@Data
@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_posts_user_id", columnList = "user_id"),
    @Index(name = "idx_posts_type_create_time", columnList = "type,create_time"),
    @Index(name = "idx_posts_status_create_time", columnList = "status,create_time")
})
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId; // 发帖用户ID
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PostType type; // 帖子类型
    
    @Column(nullable = false, length = 200)
    private String title; // 帖子标题
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 帖子内容
    
    @Column(columnDefinition = "TEXT")
    private String images; // 图片URL列表（JSON数组格式存储）
    
    @Column(name = "video_url", length = 500)
    private String videoUrl; // 视频URL
    
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0; // 点赞数
    
    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0; // 评论数
    
    @Column(name = "favorite_count", nullable = false)
    private Integer favoriteCount = 0; // 收藏数
    
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0; // 浏览数
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.NORMAL; // 状态
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = PostStatus.NORMAL;
        }
        if (this.likeCount == null) {
            this.likeCount = 0;
        }
        if (this.commentCount == null) {
            this.commentCount = 0;
        }
        if (this.favoriteCount == null) {
            this.favoriteCount = 0;
        }
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}

