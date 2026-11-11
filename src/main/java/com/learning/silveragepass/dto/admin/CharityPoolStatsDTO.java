package com.learning.silveragepass.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 公益池统计信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharityPoolStatsDTO {
    
    /**
     * 当前余额
     */
    private Integer currentBalance;
    
    /**
     * 本季度累计捐赠
     */
    private Integer quarterlyDonations;
    
    /**
     * 累计发放
     */
    private Integer totalDistributed;
    
    /**
     * 最近捐赠时间
     */
    private LocalDateTime lastDonationTime;
}
