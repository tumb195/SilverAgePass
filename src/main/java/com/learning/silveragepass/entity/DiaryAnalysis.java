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
 * 日记分析实体类
 * Diary analysis entity for AI analysis of health diary entries
 */
@Data
@Entity
@Table(name = "diary_analysis", indexes = {
    @Index(name = "idx_status_analyzed", columnList = "analysis_status, analyzed_at"),
    @Index(name = "idx_entry", columnList = "entry_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class DiaryAnalysis {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "analysis_id", columnDefinition = "CHAR(36)")
    private String analysisId;

    @Column(name = "entry_id", nullable = false, columnDefinition = "CHAR(36)")
    private String entryId; // 日记条目ID

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "emotion_triggers", columnDefinition = "JSON")
    private List<String> emotionTriggers; // 情感触发因素

    @Column(name = "sleep_assessment", columnDefinition = "TEXT")
    private String sleepAssessment; // 睡眠质量评估

    @Column(name = "stress_assessment", columnDefinition = "TEXT")
    private String stressAssessment; // 压力水平评估

    @Column(name = "ai_insights", columnDefinition = "TEXT")
    private String aiInsights; // AI生成的洞察

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt; // 分析完成时间

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 20)
    private AnalysisStatus analysisStatus; // 分析状态

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount; // 重试次数

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // 错误消息

    /**
     * 预处理方法，设置默认值
     */
    @PrePersist
    public void prePersist() {
        if (this.analysisId == null) {
            this.analysisId = UUID.randomUUID().toString();
        }
        if (this.analysisStatus == null) {
            this.analysisStatus = AnalysisStatus.PENDING;
        }
        if (this.retryCount == null) {
            this.retryCount = 0;
        }
        // Validate retry count
        if (retryCount != null && (retryCount < 0 || retryCount > 3)) {
            throw new IllegalArgumentException("Retry count must be between 0 and 3");
        }
    }

    /**
     * 更新前验证
     */
    @PreUpdate
    public void preUpdate() {
        // Validate retry count
        if (retryCount != null && (retryCount < 0 || retryCount > 3)) {
            throw new IllegalArgumentException("Retry count must be between 0 and 3");
        }
    }
}
