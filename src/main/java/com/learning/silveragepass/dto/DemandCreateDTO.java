package com.learning.silveragepass.dto;

import com.learning.silveragepass.entity.DemandType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandCreateDTO {
    @NotBlank(message = "标题不能为空")
    private String title;
    
    @NotNull(message = "需求类型不能为空")
    private DemandType type;
    
    @NotBlank(message = "需求描述不能为空")
    private String description;
    
    @NotBlank(message = "服务地址不能为空")
    private String serviceAddress;
    
    @NotNull(message = "服务日期不能为空")
    private LocalDate serviceDate;
    
    @NotNull(message = "服务时间不能为空")
    private LocalTime serviceTime;
    
    @NotNull(message = "预计服务时长不能为空")
    @Min(value = 1, message = "预计服务时长必须大于0")
    private Integer estimatedDuration;
    
    @NotNull(message = "暖龄币奖励不能为空")
    @Min(value = 1, message = "暖龄币奖励必须大于0")
    private Integer warmCoinReward;
    
    // 地理位置坐标
    private Double latitude;
    private Double longitude;
} 