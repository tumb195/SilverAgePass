package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知实体
 * 用于存储系统中所有类型的通知消息
 */
@Data
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_receiver", columnList = "receiverId"),
    @Index(name = "idx_notification_type", columnList = "type"),
    @Index(name = "idx_notification_read", columnList = "isRead"),
    @Index(name = "idx_notification_create_time", columnList = "createTime")
})
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long receiverId;           // 接收者ID
    
    private Long senderId;             // 发送者ID（系统消息为null）
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;     // 通知类型
    
    @Column(nullable = false)
    private String title;              // 标题
    
    @Column(columnDefinition = "TEXT")
    private String content;            // 内容
    
    @Column(columnDefinition = "JSON")
    private String extraData;          // 扩展数据（JSON格式）
    
    private Long relatedId;            // 关联业务ID
    
    @Column(nullable = false)
    private Boolean isRead = false;    // 是否已读
    
    @Column(nullable = false)
    private Integer priority = 0;      // 优先级（0-普通，1-重要，2-紧急）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;
    
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        if (this.isRead == null) {
            this.isRead = false;
        }
        if (this.priority == null) {
            this.priority = 0;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        // 如果标记为已读且之前未设置readTime，则设置readTime
        if (Boolean.TRUE.equals(this.isRead) && this.readTime == null) {
            this.readTime = LocalDateTime.now();
        }
    }
}
