package com.learning.silveragepass.dto.certification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拒绝认证请求DTO
 * 用于管理员拒绝认证申请时提供拒绝原因
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificationRejectDTO {

    @NotBlank(message = "拒绝原因不能为空")
    private String reason;
}
