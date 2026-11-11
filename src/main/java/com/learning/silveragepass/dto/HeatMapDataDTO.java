package com.learning.silveragepass.dto;

import com.learning.silveragepass.entity.DemandType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 热力图单点数据DTO
 * 用于返回单个需求点的热力图数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatMapDataDTO {
    private Long id;
    private Double latitude;
    private Double longitude;
    private Integer weight;          // 权重 (0-100)，根据需求数量或紧急程度设置
    private DemandType type;
    private String title;
    private Double distance;         // 与用户的距离（米）
    private LocalDate serviceDate;   // 服务日期
    private Integer urgencyLevel;    // 紧急程度 (1-5)
} 