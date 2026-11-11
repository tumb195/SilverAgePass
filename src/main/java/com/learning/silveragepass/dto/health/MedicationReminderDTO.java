package com.learning.silveragepass.dto.health;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用药提醒响应DTO
 * Requirements: 3.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReminderDTO {

    private Long id;

    private Long userId;

    private Long creatorId;

    private String creatorName;    // 创建者姓名

    private String medicationName; // 药品名称

    private String dosage;         // 剂量

    private List<String> reminderTimes;  // 提醒时间列表

    private String frequency;      // 频率（每天/隔天等）

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;   // 开始日期

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;     // 结束日期

    private String note;           // 备注

    private Boolean active;        // 是否启用

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
