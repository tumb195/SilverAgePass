package com.learning.silveragepass.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子评论实体类
 */
@Data
@Entity
@Table(name = "post_comments", indexes = {
    @Index(name = "idx_post_comments_post_id_create_time", columnList = "post_id,create_time"),
    @Index(name = "idx_post_comments_user_id", columnList = "user_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class PostComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", nullable = false)
    private Long postId; // 帖子ID
    
    @Column(name = "user_id", nullable = false)
    private Long userId; // 评论用户ID
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 评论内容
    
    @Column(columnDefinition = "TEXT")
    private String images; // 图片URL列表（JSON数组格式存储）
    
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0; // 点赞数
    
    @Column(name = "reply_count", nullable = false)
    private Integer replyCount = 0; // 回复数
    
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
        if (this.replyCount == null) {
            this.replyCount = 0;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}

