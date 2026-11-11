package com.learning.silveragepass.dto;

import com.learning.silveragepass.entity.DemandType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 热力图网格聚合数据DTO
 * 用于返回按网格聚合的需求密度数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatMapGridDTO {
    
    private Double centerLatitude;   // 网格中心纬度
    
    private Double centerLongitude;  // 网格中心经度
    
    private Integer demandCount;     // 需求数量
    
    private Integer weight;          // 权重值 (0-100)
    
    private Map<DemandType, Integer> typeBreakdown;  // 按类型分布
    
    private Double avgDistance;      // 平均距离（米）
}
