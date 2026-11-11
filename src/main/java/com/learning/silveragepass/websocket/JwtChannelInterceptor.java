package com.learning.silveragepass.websocket;

import com.learning.silveragepass.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket连接JWT认证拦截器
 * 在STOMP CONNECT命令时验证JWT Token，在DISCONNECT时清理会话
 * 
 * Requirements: 1.1, 1.2
 * - 1.1: 用户携带有效JWT Token发起WebSocket连接请求时，验证Token并建立连接
 * - 1.2: 用户携带无效或过期JWT Token发起连接请求时，拒绝连接并返回认证失败错误
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final WebSocketSessionManager sessionManager;

    /**
     * 在消息发送前进行拦截处理
     * 主要处理CONNECT和DISCONNECT命令
     * 
     * @param message 消息对象
     * @param channel 消息通道
     * @return 处理后的消息，如果认证失败则抛出异常
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        
        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.DISCONNECT.equals(command)) {
            handleDisconnect(accessor);
        }

        return message;
    }

    /**
     * 处理CONNECT命令
     * 验证JWT Token并注册用户会话
     * 
     * @param accessor STOMP头访问器
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        
        if (token == null || token.isEmpty()) {
            log.warn("WebSocket connection attempt without JWT token");
            throw new AuthenticationCredentialsNotFoundException("Missing JWT token");
        }

        // 验证Token
        JwtUtils.TokenValidationResult validationResult = jwtUtils.validateTokenWithDetails(token);
        
        if (!validationResult.isValid()) {
            log.warn("WebSocket connection attempt with invalid token: {}", validationResult.getErrorCode());
            throw new AuthenticationCredentialsNotFoundException(
                    "Invalid JWT token: " + validationResult.getErrorMessage());
        }

        try {
            // 从Token中提取用户信息
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUserNameFromJwtToken(token);
            
            if (userId == null) {
                log.warn("WebSocket connection attempt with token missing userId");
                throw new AuthenticationCredentialsNotFoundException("Token missing userId claim");
            }

            // 创建Principal并设置到accessor
            StompPrincipal principal = new StompPrincipal(userId, username);
            accessor.setUser(principal);

            // 注册会话
            String sessionId = accessor.getSessionId();
            sessionManager.registerSession(userId, sessionId);
            
            log.info("WebSocket connection established for user {} (session: {})", userId, sessionId);
            
        } catch (AuthenticationCredentialsNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing JWT token for WebSocket connection: {}", e.getMessage());
            throw new AuthenticationCredentialsNotFoundException("Failed to process JWT token: " + e.getMessage());
        }
    }

    /**
     * 处理DISCONNECT命令
     * 清理用户会话映射
     * 
     * @param accessor STOMP头访问器
     */
    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        
        if (sessionId != null) {
            Long userId = sessionManager.removeSession(sessionId);
            if (userId != null) {
                log.info("WebSocket connection closed for user {} (session: {})", userId, sessionId);
            } else {
                log.debug("WebSocket session {} disconnected (no user mapping found)", sessionId);
            }
        }
    }

    /**
     * 从STOMP头中提取JWT Token
     * 支持两种方式：
     * 1. Authorization头（格式：Bearer <token>）
     * 2. 自定义token头
     * 
     * @param accessor STOMP头访问器
     * @return JWT Token字符串，如果未找到则返回null
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // 首先尝试从Authorization头获取
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            // 如果没有Bearer前缀，直接返回
            return authHeader;
        }

        // 尝试从token头获取
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        // 尝试从X-Auth-Token头获取
        List<String> xAuthHeaders = accessor.getNativeHeader("X-Auth-Token");
        if (xAuthHeaders != null && !xAuthHeaders.isEmpty()) {
            return xAuthHeaders.get(0);
        }

        return null;
    }
}
