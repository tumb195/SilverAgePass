package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 公益池信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharityPoolDTO {

    private Integer balance;          // 当前余额
    private Integer totalDonated;     // 累计捐赠总额
    private Integer totalDistributed; // 累计发放总额
    private LocalDateTime updateTime; // 最后更新时间
}
