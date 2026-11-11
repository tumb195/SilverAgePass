package com.learning.silveragepass.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 血糖统计数据DTO
 * Requirements: 5.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodSugarStatisticsDTO {

    private Double avgGlucose;     // 平均血糖值

    private Double maxGlucose;     // 最高血糖值

    private Double minGlucose;     // 最低血糖值

    private Long recordCount;      // 记录总数
}
