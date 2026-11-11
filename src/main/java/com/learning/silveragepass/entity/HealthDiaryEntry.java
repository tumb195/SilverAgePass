package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 健康日记条目实体类
 * Health diary entry entity for daily health and emotion records
 */
@Data
@Entity
@Table(name = "health_diary_entry", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_date", columnNames = {"user_id", "entry_date"})
    },
    indexes = {
        @Index(name = "idx_user_date", columnList = "user_id, entry_date")
    }
)
@NoArgsConstructor
@AllArgsConstructor
public class HealthDiaryEntry {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "entry_id", columnDefinition = "CHAR(36)")
    private String entryId;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 用户ID

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate; // 日记日期

    @Column(name = "emotion_score", nullable = false)
    private Integer emotionScore; // 情绪评分（1-10）

    @Column(name = "sleep_quality", nullable = false)
    private Integer sleepQuality; // 睡眠质量（1-5）

    @Column(name = "stress_level", nullable = false)
    private Integer stressLevel; // 压力水平（1-5）

    @Column(name = "diary_content", columnDefinition = "TEXT")
    private String diaryContent; // 日记内容

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 更新时间

    /**
     * 预处理方法，设置默认值和验证
     */
    @PrePersist
    public void prePersist() {
        if (this.entryId == null) {
            this.entryId = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        validateScores();
    }

    /**
     * 更新前处理方法
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateScores();
    }

    /**
     * 验证评分范围
     */
    private void validateScores() {
        if (emotionScore != null && (emotionScore < 1 || emotionScore > 10)) {
            throw new IllegalArgumentException("Emotion score must be between 1 and 10");
        }
        if (sleepQuality != null && (sleepQuality < 1 || sleepQuality > 5)) {
            throw new IllegalArgumentException("Sleep quality must be between 1 and 5");
        }
        if (stressLevel != null && (stressLevel < 1 || stressLevel > 5)) {
            throw new IllegalArgumentException("Stress level must be between 1 and 5");
        }
    }
}
