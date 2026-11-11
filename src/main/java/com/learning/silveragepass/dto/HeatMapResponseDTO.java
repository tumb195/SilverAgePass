package com.learning.silveragepass.dto;

import com.learning.silveragepass.entity.DemandType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 热力图响应DTO
 * 包装热力图查询的完整响应数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatMapResponseDTO {
    
    private Double centerLatitude;   // 查询中心纬度
    
    private Double centerLongitude;  // 查询中心经度
    
    private Integer radius;          // 查询半径（米）
    
    private Integer totalCount;      // 总需求数量
    
    private List<HeatMapDataDTO> points;  // 单点数据列表
    
    private List<HeatMapGridDTO> grids;   // 聚合网格数据列表（可选）
    
    private Map<DemandType, Long> typeDistribution;  // 类型分布统计
}
