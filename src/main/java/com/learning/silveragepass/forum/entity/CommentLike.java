package com.learning.silveragepass.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论点赞实体类
 */
@Data
@Entity
@Table(name = "comment_likes", 
       uniqueConstraints = {
           @UniqueConstraint(name = "unique_comment_likes_comment_user", columnNames = {"comment_id", "user_id"})
       })
@NoArgsConstructor
@AllArgsConstructor
public class CommentLike {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "comment_id", nullable = false)
    private Long commentId; // 评论ID
    
    @Column(name = "user_id", nullable = false)
    private Long userId; // 点赞用户ID
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
    
    public CommentLike(Long commentId, Long userId) {
        this.commentId = commentId;
        this.userId = userId;
    }
}

