package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用药提醒实体
 */
@Data
@Entity
@Table(name = "medication_reminders", indexes = {
    @Index(name = "idx_mr_user_id", columnList = "userId"),
    @Index(name = "idx_mr_active", columnList = "active")
})
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long userId;           // 提醒所属用户

    @NotNull
    @Column(nullable = false)
    private Long creatorId;        // 创建者（可能是亲属）

    @NotBlank
    @Column(nullable = false)
    private String medicationName; // 药品名称

    private String dosage;         // 剂量

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reminderTimes;  // 提醒时间（JSON数组格式，如 ["08:00", "12:00", "18:00"]）

    private String frequency;      // 频率（每天/隔天等）

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;   // 开始日期

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;     // 结束日期

    private String note;           // 备注

    @Column(nullable = false)
    private Boolean active = true; // 是否启用

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
