package com.learning.silveragepass.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarmCoinDistributionDTO {
    @NotNull(message = "需求ID不能为空")
    private Long demandId;
    
    @Min(value = 0, message = "自己保留比例不能小于0")
    @Max(value = 100, message = "自己保留比例不能大于100")
    private Integer selfPercentage = 100; // 默认全部自己保留
    
    @Min(value = 0, message = "转赠父母比例不能小于0")
    @Max(value = 100, message = "转赠父母比例不能大于100")
    private Integer parentPercentage = 0;
    
    @Min(value = 0, message = "捐赠公益池比例不能小于0")
    @Max(value = 100, message = "捐赠公益池比例不能大于100")
    private Integer donatePercentage = 0;
    
    // 转赠父母的用户ID
    private Long parentUserId;
} 