package com.learning.silveragepass.dto;

import com.learning.silveragepass.entity.DemandType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 热力图请求DTO
 * 用于接收热力图查询参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeatMapRequestDTO {
    
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度必须在-90到90之间")
    private Double latitude;
    
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度必须在-180到180之间")
    private Double longitude;
    
    @Min(value = 1, message = "搜索半径必须为正数")
    @Max(value = 50000, message = "搜索半径不能超过50公里")
    private Integer radius = 5000;  // 默认5公里
    
    private List<DemandType> types;  // 需求类型过滤
    
    @Min(value = 100, message = "网格大小最小为100米")
    @Max(value = 5000, message = "网格大小最大为5000米")
    private Integer gridSize = 500;  // 网格大小（米）
    
    private Boolean aggregated = false;  // 是否聚合
    
    @Min(value = 1, message = "最大返回数量必须为正数")
    @Max(value = 1000, message = "最大返回数量不能超过1000")
    private Integer maxResults = 500;  // 最大返回数量
}
