package com.learning.silveragepass.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token错误响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenErrorResponse {

    /**
     * HTTP状态码
     */
    private int status;

    /**
     * 错误代码
     * ACCESS_TOKEN_EXPIRED - Access Token已过期
     * REFRESH_TOKEN_EXPIRED - Refresh Token已过期
     * INVALID_TOKEN - Token签名无效
     * MALFORMED_TOKEN - Token格式错误
     * TOKEN_REUSE_DETECTED - 检测到Token重放攻击
     * TOKEN_REVOKED - Token已被撤销
     */
    private String errorCode;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 时间戳（毫秒）
     */
    private long timestamp;

    /**
     * 创建错误响应的便捷方法
     */
    public static TokenErrorResponse of(int status, String errorCode, String message) {
        return TokenErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
