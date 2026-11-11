package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 情感分析实体类
 * Emotion analysis entity for messages and diary entries
 */
@Data
@Entity
@Table(name = "emotion_analysis", indexes = {
    @Index(name = "idx_risk_analyzed", columnList = "risk_level, analyzed_at"),
    @Index(name = "idx_emotion_time", columnList = "primary_emotion, analyzed_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class EmotionAnalysis {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "analysis_id", columnDefinition = "CHAR(36)")
    private String analysisId;

    @Column(name = "message_id", columnDefinition = "CHAR(36)")
    private String messageId; // 消息ID（可为空）

    @Column(name = "diary_entry_id", columnDefinition = "CHAR(36)")
    private String diaryEntryId; // 日记条目ID（可为空）

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_emotion", nullable = false, length = 50)
    private EmotionType primaryEmotion; // 主要情感类型

    @Column(name = "intensity_score", nullable = false)
    private Integer intensityScore; // 情感强度（0-100）

    @Column(name = "is_positive", nullable = false)
    private Boolean isPositive; // 是否为积极情感

    @Column(name = "risk_level", nullable = false)
    private Integer riskLevel; // 心理风险等级（0-3）

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<String> keywords; // 情感相关关键词

    @Column(columnDefinition = "TEXT")
    private String suggestions; // 个性化改善建议

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "analyzed_at", nullable = false, updatable = false)
    private LocalDateTime analyzedAt; // 分析时间戳

    /**
     * 预处理方法，设置默认值
     */
    @PrePersist
    public void prePersist() {
        if (this.analysisId == null) {
            this.analysisId = UUID.randomUUID().toString();
        }
        if (this.analyzedAt == null) {
            this.analyzedAt = LocalDateTime.now();
        }
        // Validate that only one source is set
        if ((messageId != null && diaryEntryId != null) || 
            (messageId == null && diaryEntryId == null)) {
            throw new IllegalStateException("Exactly one of messageId or diaryEntryId must be set");
        }
        // Validate score ranges
        if (intensityScore != null && (intensityScore < 0 || intensityScore > 100)) {
            throw new IllegalArgumentException("Intensity score must be between 0 and 100");
        }
        if (riskLevel != null && (riskLevel < 0 || riskLevel > 3)) {
            throw new IllegalArgumentException("Risk level must be between 0 and 3");
        }
    }
}
