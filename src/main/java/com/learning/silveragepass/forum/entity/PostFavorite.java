package com.learning.silveragepass.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子收藏实体类
 */
@Data
@Entity
@Table(name = "post_favorites", 
       uniqueConstraints = {
           @UniqueConstraint(name = "unique_post_favorites_post_user", columnNames = {"post_id", "user_id"})
       },
       indexes = {
           @Index(name = "idx_post_favorites_user_id_create_time", columnList = "user_id,create_time")
       })
@NoArgsConstructor
@AllArgsConstructor
public class PostFavorite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", nullable = false)
    private Long postId; // 帖子ID
    
    @Column(name = "user_id", nullable = false)
    private Long userId; // 收藏用户ID
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
    
    public PostFavorite(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }
}

