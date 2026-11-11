package com.learning.silveragepass.dto.health;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.entity.ReminderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 复诊提醒响应DTO
 * Requirements: 2.1, 4.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowupReminderDTO {

    private Long id;

    private Long userId;

    private Long creatorId;

    private String creatorName;        // 创建者姓名

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate; // 复诊日期

    @JsonFormat(pattern = "HH:mm")
    private LocalTime appointmentTime; // 复诊时间

    private String hospitalName;       // 医院名称

    private String doctorName;         // 医生姓名

    private String department;         // 科室

    private String note;               // 备注

    private ReminderStatus status;     // 提醒状态

    private String statusDescription;  // 状态中文描述

    private Integer daysRemaining;     // 距离复诊还有几天

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
