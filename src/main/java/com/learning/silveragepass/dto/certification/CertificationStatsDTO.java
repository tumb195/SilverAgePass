package com.learning.silveragepass.dto.certification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证统计数据DTO
 * 用于管理端展示认证统计信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificationStatsDTO {

    private Long totalCount;  // 总数

    private Long pendingCount;  // 待审核数量

    private Long approvedCount;  // 已通过数量

    private Long rejectedCount;  // 已拒绝数量
}
