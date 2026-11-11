package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandStatisticsDTO {
    private Long totalCount; // 需求总数
    private Long completedCount; // 已完成数量
    private Long waitingCount; // 待接单数量
    private Long acceptedCount; // 已接单数量
    private Long inProgressCount; // 进行中数量
    private Long cancelledCount; // 已取消数量
    private Double avgResponseTime; // 平均响应时间（小时）
    
    // 需求类型分布统计
    private List<TypeDistribution> typeDistributions;
    
    // 需求状态分布统计
    private List<StatusDistribution> statusDistributions;
    
    // 需求时间分布统计（按月）
    private List<TimeDistribution> timeDistributions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeDistribution {
        private String type;
        private String typeName;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDistribution {
        private String status;
        private String statusName;
        private Long count;
        private Double percentage;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeDistribution {
        private String month; // 格式：yyyy-MM
        private Long count;
    }
} 