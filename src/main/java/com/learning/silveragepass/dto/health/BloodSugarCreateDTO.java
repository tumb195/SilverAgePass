package com.learning.silveragepass.dto.health;

import com.learning.silveragepass.entity.MeasurementPeriod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 血糖记录创建DTO
 * Requirements: 2.1, 2.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BloodSugarCreateDTO {

    @NotNull(message = "血糖值不能为空")
    @DecimalMin(value = "1.0", message = "血糖值不能低于1.0mmol/L")
    @DecimalMax(value = "35.0", message = "血糖值不能高于35.0mmol/L")
    private Double glucoseValue;

    private MeasurementPeriod period;  // 测量时段（空腹/餐后等）

    private String note;
}
