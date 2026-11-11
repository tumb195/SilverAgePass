package com.learning.silveragepass.dto.certification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交认证请求DTO
 * 用于用户提交组织认证申请
 * 
 * 支持两种模式：
 * 1. 选择已有组织：提供 organizationId
 * 2. 手动填写组织（兼容旧版本）：提供 organizationName
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificationSubmitDTO {

    /**
     * 选择的组织ID（新模式）
     * 如果提供了organizationId，则从组织表获取组织名称
     */
    private Long organizationId;

    /**
     * 组织名称（兼容旧版本）
     * 当organizationId为空时，使用此字段
     */
    private String organizationName;

    /**
     * 证书编号（可选）
     */
    private String certificateNumber;

    @NotBlank(message = "证书图片不能为空")
    private String certificateImageUrl;
}
