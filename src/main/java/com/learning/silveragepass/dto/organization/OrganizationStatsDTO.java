package com.learning.silveragepass.dto.organization;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 组织统计数据DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationStatsDTO {

    private Long totalCount;

    private Long activeCount;

    private Long inactiveCount;
}
