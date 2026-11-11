package com.learning.silveragepass.dto.proxy;

import com.learning.silveragepass.entity.DemandType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 代发需求创建请求DTO
 * Requirements: 3.1, 4.1, 4.2, 4.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyDemandCreateDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotNull(message = "需求类型不能为空")
    private DemandType type;

    @NotBlank(message = "需求描述不能为空")
    private String description;

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

    private Double latitude;

    private Double longitude;

    /**
     * 从地址簿选择的地址ID（可选）
     */
    private Long addressId;

    /**
     * 地址来源: "elder" 或 "relative"
     */
    private String addressSource;

    /**
     * 是否保存到老人地址簿（当使用联系人地址时）
     */
    private Boolean saveToElderAddressBook;
}
