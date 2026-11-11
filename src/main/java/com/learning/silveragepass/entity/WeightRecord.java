package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 体重记录实体
 * Weight Record Entity
 * 
 * 记录用户的体重数据，支持：
 * - 体重值（kg）
 * - 测量日期和时间
 * - 备注信息
 * 
 * Requirements:
 * - 8.2: Agent framework supports health data recording tool (blood pressure, blood sugar, weight)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Entity
@Table(name = "weight_records", indexes = {
    @Index(name = "idx_weight_user_id", columnList = "userId"),
    @Index(name = "idx_weight_measurement_date", columnList = "measurementDate")
})
@NoArgsConstructor
@AllArgsConstructor
public class WeightRecord {

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
    @DecimalMin("20.0")
    @DecimalMax("300.0")
    @Column(nullable = false)
    private Double weightValue;    // 体重值 (kg)

    @DecimalMin("100.0")
    @DecimalMax("250.0")
    private Double height;         // 身高 (cm)，可选，用于计算BMI

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
    
    /**
     * 计算BMI（体质指数）
     * Calculate BMI (Body Mass Index)
     * 
     * @return BMI值，如果身高未设置则返回null
     */
    public Double calculateBMI() {
        if (height == null || height <= 0 || weightValue == null || weightValue <= 0) {
            return null;
        }
        // BMI = 体重(kg) / 身高(m)^2
        double heightInMeters = height / 100.0;
        return weightValue / (heightInMeters * heightInMeters);
    }
    
    /**
     * 获取BMI等级描述
     * Get BMI level description
     * 
     * @return BMI等级描述
     */
    public String getBMILevel() {
        Double bmi = calculateBMI();
        if (bmi == null) {
            return "未知";
        }
        if (bmi < 18.5) {
            return "偏瘦";
        } else if (bmi < 24.0) {
            return "正常";
        } else if (bmi < 28.0) {
            return "偏胖";
        } else {
            return "肥胖";
        }
    }
}
