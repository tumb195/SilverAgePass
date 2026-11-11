package com.learning.silveragepass.dto.health;

import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 复诊提醒更新DTO
 * Requirements: 3.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowupReminderUpdateDTO {

    @Future(message = "复诊日期必须是未来日期")
    private LocalDate appointmentDate;  // 复诊日期（可选更新）

    private LocalTime appointmentTime;  // 复诊时间（可选更新）

    private String hospitalName;        // 医院名称（可选更新）

    private String doctorName;          // 医生姓名（可选更新）

    private String department;          // 科室（可选更新）

    private String note;                // 备注（可选更新）
}
