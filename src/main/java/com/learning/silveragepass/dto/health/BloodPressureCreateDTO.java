package com.learning.silveragepass.dto.health;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 血压记录创建DTO
 * Requirements: 1.1, 1.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BloodPressureCreateDTO {

    @NotNull(message = "收缩压不能为空")
    @Min(value = 60, message = "收缩压不能低于60mmHg")
    @Max(value = 250, message = "收缩压不能高于250mmHg")
    private Integer systolicPressure;

    @NotNull(message = "舒张压不能为空")
    @Min(value = 40, message = "舒张压不能低于40mmHg")
    @Max(value = 150, message = "舒张压不能高于150mmHg")
    private Integer diastolicPressure;

    @Min(value = 30, message = "脉搏不能低于30次/分钟")
    @Max(value = 200, message = "脉搏不能高于200次/分钟")
    private Integer pulse;

    private String note;
}
