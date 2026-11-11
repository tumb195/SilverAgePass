package com.learning.silveragepass.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 志愿者活跃度排名DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerRankingDTO {

    private Long userId;           // 用户ID
    private String username;       // 用户名
    private Integer warmCoin;      // 暖龄币
    private Long serviceCount;     // 服务次数（一周内）
    private Integer ranking;       // 排名
}
