package com.learning.silveragepass.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token刷新请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {

    /**
     * Refresh Token
     */
    @NotBlank(message = "Refresh token不能为空")
    private String refreshToken;
}
