package com.learning.silveragepass.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子点赞实体类
 */
@Data
@Entity
@Table(name = "post_likes", 
       uniqueConstraints = {
           @UniqueConstraint(name = "unique_post_likes_post_user", columnNames = {"post_id", "user_id"})
       },
       indexes = {
           @Index(name = "idx_post_likes_user_id", columnList = "user_id")
       })
@NoArgsConstructor
@AllArgsConstructor
public class PostLike {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", nullable = false)
    private Long postId; // 帖子ID
    
    @Column(name = "user_id", nullable = false)
    private Long userId; // 点赞用户ID
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
    
    public PostLike(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }
}

