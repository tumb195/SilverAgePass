package com.learning.silveragepass.dto.proxy;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代发授权申请请求DTO
 * Requirements: 1.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyAuthorizationRequestDTO {

    @NotNull(message = "老人用户ID不能为空")
    private Long elderId;
}
