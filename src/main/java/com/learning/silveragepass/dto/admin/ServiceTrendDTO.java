package com.learning.silveragepass.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务量趋势DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTrendDTO {

    private String date;            // 日期，格式：MM-dd
    private String dayOfWeek;       // 星期几
    private Long serviceCount;      // 服务总数
    private Long completedCount;    // 已完成数
    private Long inProgressCount;   // 进行中数
    private Long waitingCount;      // 待接单数
}
