package com.learning.silveragepass.dto.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药提醒更新DTO
 * Requirements: 3.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReminderUpdateDTO {

    private String medicationName; // 药品名称

    private String dosage;         // 剂量

    private List<String> reminderTimes;  // 提醒时间列表

    private String frequency;      // 频率（每天/隔天等）

    private LocalDate startDate;   // 开始日期

    private LocalDate endDate;     // 结束日期

    private String note;           // 备注

    private Boolean active;        // 是否启用
}
