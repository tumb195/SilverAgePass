package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.auth.DualTokenResponse;

/**
 * Refresh Token服务接口
 */
public interface RefreshTokenService {

    /**
     * 为用户创建新的Refresh Token
     * @param userId 用户ID
     * @return 原始Refresh Token字符串（仅返回一次，之后只存储哈希值）
     */
    String createRefreshToken(Long userId);

    /**
     * 验证并轮换Refresh Token
     * @param refreshToken 原始Refresh Token
     * @return 包含新Access Token和新Refresh Token的响应
     */
    DualTokenResponse validateAndRotate(String refreshToken);

    /**
     * 撤销Refresh Token（用于登出）
     * @param refreshToken 原始Refresh Token
     */
    void revokeToken(String refreshToken);

    /**
     * 撤销用户的所有Refresh Token（用于安全事件响应）
     * @param userId 用户ID
     */
    void revokeAllUserTokens(Long userId);
}
