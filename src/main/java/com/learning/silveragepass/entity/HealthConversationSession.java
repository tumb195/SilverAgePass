package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI健康助手会话实体类
 * Health conversation session entity for AI health assistant
 */
@Data
@Entity
@Table(name = "health_conversation_session", indexes = {
    @Index(name = "idx_user_status", columnList = "user_id, status"),
    @Index(name = "idx_last_activity", columnList = "last_activity_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class HealthConversationSession {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "session_id", columnDefinition = "CHAR(36)")
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 用户ID

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 会话创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt; // 最后活动时间

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status; // 会话状态

    /**
     * 预处理方法，设置默认值
     */
    @PrePersist
    public void prePersist() {
        if (this.sessionId == null) {
            this.sessionId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.lastActivityAt == null) {
            this.lastActivityAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = SessionStatus.ACTIVE;
        }
    }

    /**
     * 更新前处理方法
     */
    @PreUpdate
    public void preUpdate() {
        this.lastActivityAt = LocalDateTime.now();
    }
}
