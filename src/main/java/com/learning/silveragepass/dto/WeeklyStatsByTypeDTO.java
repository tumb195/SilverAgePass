package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 按类型的周统计数据DTO
 * 用于展示各变动原因的统计信息和占比
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStatsByTypeDTO {
    /**
     * 变动类型（枚举名称）
     */
    private String type;
    
    /**
     * 类型中文描述
     */
    private String typeDescription;
    
    /**
     * 该类型的总金额（暖心币数量）
     */
    private Integer totalAmount;
    
    /**
     * 该类型的记录数量
     */
    private Long recordCount;
    
    /**
     * 该类型占所有类型的百分比（基于记录数量）
     * 例如：25.5 表示 25.5%
     */
    private Double percentage;
    
    /**
     * 所有类型的总记录数（用于前端验证）
     */
    private Long totalRecordCount;
}
