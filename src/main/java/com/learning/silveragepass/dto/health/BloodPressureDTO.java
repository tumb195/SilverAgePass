package com.learning.silveragepass.dto.health;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 血压记录响应DTO
 * Requirements: 1.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodPressureDTO {

    private Long id;

    private Long userId;

    private Long creatorId;

    private String creatorName;  // 创建者姓名

    private Integer systolicPressure;   // 高压 (收缩压)

    private Integer diastolicPressure;  // 低压 (舒张压)

    private Integer pulse;         // 脉搏

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate measurementDate;  // 测量日期

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime measurementTime;  // 测量时间

    private String note;           // 备注

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
