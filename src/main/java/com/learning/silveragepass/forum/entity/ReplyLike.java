package com.learning.silveragepass.forum.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 回复点赞实体类
 */
@Data
@Entity
@Table(name = "reply_likes", 
       uniqueConstraints = {
           @UniqueConstraint(name = "unique_reply_likes_reply_user", columnNames = {"reply_id", "user_id"})
       })
@NoArgsConstructor
@AllArgsConstructor
public class ReplyLike {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "reply_id", nullable = false)
    private Long replyId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
    
    public ReplyLike(Long replyId, Long userId) {
        this.replyId = replyId;
        this.userId = userId;
    }
}

