package com.learning.silveragepass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 捐赠暖龄币响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonateResponseDTO {

    private Integer donatedAmount;      // 本次捐赠金额
    private Integer remainingBalance;   // 用户剩余暖龄币
    private Integer poolBalance;        // 公益池当前余额
    private String message;             // 提示信息
}
