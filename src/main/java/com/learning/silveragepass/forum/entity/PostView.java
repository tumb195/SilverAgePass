package com.learning.silveragepass.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子浏览记录实体类
 */
@Data
@Entity
@Table(name = "post_views", indexes = {
    @Index(name = "idx_post_id", columnList = "post_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class PostView {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", nullable = false)
    private Long postId; // 帖子ID
    
    @Column(name = "user_id")
    private Long userId; // 浏览用户ID（可为空，支持匿名浏览）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
    
    public PostView(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }
}

