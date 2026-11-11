package com.learning.silveragepass.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 血压统计数据DTO
 * Requirements: 5.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodPressureStatisticsDTO {

    private Double avgSystolic;     // 平均收缩压

    private Double avgDiastolic;    // 平均舒张压

    private Integer maxSystolic;    // 最高收缩压

    private Integer maxDiastolic;   // 最高舒张压

    private Integer minSystolic;    // 最低收缩压

    private Integer minDiastolic;   // 最低舒张压

    private Long recordCount;       // 记录总数
}
