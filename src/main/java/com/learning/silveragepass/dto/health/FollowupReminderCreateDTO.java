package com.learning.silveragepass.dto.health;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 复诊提醒创建DTO
 * Requirements: 1.1, 1.4, 1.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowupReminderCreateDTO {

    @NotNull(message = "复诊日期不能为空")
    @Future(message = "复诊日期必须是未来日期")
    private LocalDate appointmentDate;

    @NotNull(message = "复诊时间不能为空")
    private LocalTime appointmentTime;

    private String hospitalName;   // 医院名称（可选）

    private String doctorName;     // 医生姓名（可选）

    private String department;     // 科室（可选）

    private String note;           // 备注（可选）
}
