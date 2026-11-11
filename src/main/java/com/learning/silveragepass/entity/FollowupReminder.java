package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 复诊提醒实体
 */
@Data
@Entity
@Table(name = "followup_reminders", indexes = {
    @Index(name = "idx_fr_user_id", columnList = "userId"),
    @Index(name = "idx_fr_status", columnList = "status"),
    @Index(name = "idx_fr_appointment_date", columnList = "appointmentDate")
})
@NoArgsConstructor
@AllArgsConstructor
public class FollowupReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long userId;           // 提醒所属用户

    @NotNull
    @Column(nullable = false)
    private Long creatorId;        // 创建者（可能是亲属）

    @NotNull
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;  // 复诊日期

    @NotNull
    @Column(nullable = false)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime appointmentTime;  // 复诊时间

    private String hospitalName;   // 医院名称

    private String doctorName;     // 医生姓名

    private String department;     // 科室

    private String note;           // 备注

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderStatus status = ReminderStatus.PENDING;  // 提醒状态

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReminderStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
