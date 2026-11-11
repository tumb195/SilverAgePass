package com.learning.silveragepass.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 捐赠暖龄币到公益池请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DonateRequestDTO {

    @NotNull(message = "捐赠金额不能为空")
    @Min(value = 1, message = "捐赠金额至少为1个暖龄币")
    private Integer amount;

    private String message;  // 捐赠留言（可选）
}
