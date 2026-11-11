package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户关注实体类（单向关注）
 */
@Data
@Entity
@Table(name = "user_follows", 
       uniqueConstraints = {
           @UniqueConstraint(name = "unique_follower_followed", columnNames = {"follower_id", "followed_id"})
       },
       indexes = {
           @Index(name = "idx_follower_id", columnList = "follower_id"),
           @Index(name = "idx_followed_id", columnList = "followed_id"),
           @Index(name = "idx_follower_create_time", columnList = "follower_id,create_time")
       })
@NoArgsConstructor
@AllArgsConstructor
public class UserFollow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "follower_id", nullable = false)
    private Long followerId; // 关注者ID（谁关注了别人）
    
    @Column(name = "followed_id", nullable = false)
    private Long followedId; // 被关注者ID（被谁关注）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
    
    public UserFollow(Long followerId, Long followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
    }
}

