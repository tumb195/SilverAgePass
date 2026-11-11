package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "messages")
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title; // 消息标题
    
    @Column(columnDefinition = "TEXT")
    private String content; // 消息内容
    
    @Enumerated(EnumType.STRING)
    private MessageType type; // 消息类型
    
    private Long senderId; // 发送者ID，系统消息为null
    
    private Long receiverId; // 接收者ID
    
    private Boolean isRead = false; // 是否已读
    
    @Column(nullable = true)
    private Long relatedDemandId; // 关联的需求ID（如果有）
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    // 预处理方法，设置默认值
    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
} 