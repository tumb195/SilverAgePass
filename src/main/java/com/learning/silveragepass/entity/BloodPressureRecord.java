package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 血压记录实体
 */
@Data
@Entity
@Table(name = "blood_pressure_records", indexes = {
    @Index(name = "idx_bp_user_id", columnList = "userId"),
    @Index(name = "idx_bp_measurement_date", columnList = "measurementDate")
})
@NoArgsConstructor
@AllArgsConstructor
public class BloodPressureRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false)
    private Long userId;           // 数据所属用户

    @NotNull
    @Column(nullable = false)
    private Long creatorId;        // 记录创建者（可能是亲属）

    @NotNull
    @Min(60)
    @Max(250)
    @Column(nullable = false)
    private Integer systolicPressure;   // 高压 (收缩压)

    @NotNull
    @Min(40)
    @Max(150)
    @Column(nullable = false)
    private Integer diastolicPressure;  // 低压 (舒张压)

    @Min(30)
    @Max(200)
    private Integer pulse;         // 脉搏（可选）

    @NotNull
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate measurementDate;  // 测量日期

    @NotNull
    @Column(nullable = false)
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime measurementTime;  // 测量时间

    private String note;           // 备注

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.measurementDate == null) {
            this.measurementDate = LocalDate.now();
        }
        if (this.measurementTime == null) {
            this.measurementTime = LocalTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
