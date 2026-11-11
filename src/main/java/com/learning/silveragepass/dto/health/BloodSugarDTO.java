package com.learning.silveragepass.dto.health;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.entity.MeasurementPeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 血糖记录响应DTO
 * Requirements: 2.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodSugarDTO {

    private Long id;

    private Long userId;

    private Long creatorId;

    private String creatorName;  // 创建者姓名

    private Double glucoseValue;   // 血糖值 (mmol/L)

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate measurementDate;  // 测量日期

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime measurementTime;  // 测量时间

    private MeasurementPeriod period;   // 测量时段

    private String periodDescription;   // 测量时段中文描述

    private String note;           // 备注

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
