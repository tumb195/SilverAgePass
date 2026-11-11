package com.learning.silveragepass.dto.health;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 用药提醒创建DTO
 * Requirements: 3.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationReminderCreateDTO {

    @NotBlank(message = "药品名称不能为空")
    private String medicationName;

    private String dosage;         // 剂量

    @NotEmpty(message = "提醒时间不能为空")
    private List<String> reminderTimes;  // 提醒时间列表，如 ["08:00", "12:00", "18:00"]

    private String frequency;      // 频率（每天/隔天等）

    private LocalDate startDate;   // 开始日期

    private LocalDate endDate;     // 结束日期

    private String note;           // 备注
}
