package com.learning.silveragepass.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论回复实体类
 */
@Data
@Entity
@Table(name = "comment_replies", indexes = {
    @Index(name = "idx_comment_id_create_time", columnList = "comment_id,create_time"),
    @Index(name = "idx_reply_to_user_id", columnList = "reply_to_user_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class CommentReply {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "comment_id", nullable = false)
    private Long commentId; // 评论ID
    
    @Column(name = "user_id", nullable = false)
    private Long userId; // 回复用户ID
    
    @Column(name = "reply_to_user_id", nullable = false)
    private Long replyToUserId; // 被回复用户ID
    
    @Column(nullable = false, length = 500)
    private String content; // 回复内容
    
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0; // 点赞数
    
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
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}

