package com.learning.silveragepass.dto.video;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 观看进度 DTO
 * 用于更新用户的观看进度
 * 
 * Requirements: 6.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchProgressDTO {
    
    /**
     * 观看位置（秒）
     */
    @NotNull(message = "观看位置不能为空")
    @Min(value = 0, message = "观看位置不能为负数")
    private Integer position;
}
