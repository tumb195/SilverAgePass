package com.learning.silveragepass.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 双Token响应DTO
 * 包含Access Token和Refresh Token及其相关信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DualTokenResponse {

    /**
     * Access Token（JWT格式）
     */
    private String accessToken;

    /**
     * Refresh Token
     */
    private String refreshToken;

    /**
     * Token类型，固定为"Bearer"
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Access Token有效期（秒）
     */
    private Long accessTokenExpiresIn;

    /**
     * Refresh Token有效期（秒）
     */
    private Long refreshTokenExpiresIn;

    /**
     * Access Token过期时间戳（毫秒）
     */
    private Long accessTokenExpiresAt;

    /**
     * Refresh Token过期时间戳（毫秒）
     */
    private Long refreshTokenExpiresAt;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户角色列表
     */
    private List<String> roles;
}
