package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.converter.StringEncryptionConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI健康助手消息实体类
 * Health conversation message entity for AI health assistant
 */
@Data
@Entity
@Table(name = "health_conversation_message", indexes = {
    @Index(name = "idx_session_created", columnList = "session_id, created_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class HealthConversationMessage {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "message_id", columnDefinition = "CHAR(36)")
    private String messageId;

    @Column(name = "session_id", nullable = false, columnDefinition = "CHAR(36)")
    private String sessionId; // 会话ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role; // 消息角色

    @Convert(converter = StringEncryptionConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 消息内容（加密存储）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 消息时间戳

    @Column(name = "emotion_analysis_id", columnDefinition = "CHAR(36)")
    private String emotionAnalysisId; // 情感分析ID（可为空）

    @Transient
    private EmotionAnalysis emotionAnalysis; // 关联的情感分析对象（非持久化）

    /**
     * 获取关联的情感分析对象
     */
    public EmotionAnalysis getEmotionAnalysis() {
        return emotionAnalysis;
    }

    /**
     * 设置关联的情感分析对象
     */
    public void setEmotionAnalysis(EmotionAnalysis emotionAnalysis) {
        this.emotionAnalysis = emotionAnalysis;
    }

    /**
     * 预处理方法，设置默认值
     */
    @PrePersist
    public void prePersist() {
        if (this.messageId == null) {
            this.messageId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
