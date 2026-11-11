package com.learning.silveragepass.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    
    private Long totalUsers;  // 总用户数
    private Long totalVolunteers;  // 总志愿者数
    private Long totalDemands;  // 总任务数
    private Long completedDemands;  // 已完成任务数
    private Long pendingComplaints;  // 待处理投诉数
    private Long activeVolunteers;  // 活跃志愿者数
    private Long todayNewUsers;  // 今日新增用户
    private Long todayNewDemands;  // 今日新增任务
}
