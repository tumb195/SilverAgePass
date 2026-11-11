package com.learning.silveragepass.websocket;

import java.security.Principal;

/**
 * WebSocket STOMP连接的Principal实现
 * 用于在WebSocket会话中标识用户身份
 * 
 * Requirements: 1.1, 1.3
 * - 支持JWT认证的WebSocket连接
 * - 连接成功后将用户ID与会话映射关系存储到内存中
 */
public class StompPrincipal implements Principal {

    private final Long userId;
    private final String username;

    public StompPrincipal(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    /**
     * 获取用户ID
     * @return 用户ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 获取用户名
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * Principal接口要求的getName方法
     * 返回用户ID的字符串形式，用于STOMP用户目的地解析
     * @return 用户ID字符串
     */
    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    @Override
    public String toString() {
        return "StompPrincipal{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                '}';
    }
}
