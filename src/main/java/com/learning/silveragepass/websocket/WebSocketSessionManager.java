package com.learning.silveragepass.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket会话管理器
 * 维护用户ID与WebSocket会话的双向映射关系
 * 
 * Requirements: 1.3, 1.4, 1.5
 * - 1.3: 连接成功后将用户ID与会话映射关系存储到内存中
 * - 1.4: 断开连接时移除该用户的会话映射
 * - 1.5: 同一用户建立新连接时关闭旧连接并使用新连接
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * userId -> sessionId 映射
     * 用于通过用户ID快速查找其会话
     */
    private final ConcurrentHashMap<Long, String> userSessions = new ConcurrentHashMap<>();

    /**
     * sessionId -> userId 反向映射
     * 用于在会话断开时快速查找对应的用户ID
     */
    private final ConcurrentHashMap<String, Long> sessionUsers = new ConcurrentHashMap<>();

    /**
     * 用于发送WebSocket消息（关闭旧连接时使用）
     */
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 设置消息模板（用于延迟注入，避免循环依赖）
     */
    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 注册用户会话
     * 如果用户已有旧会话，则关闭旧会话并使用新会话
     * 
     * @param userId 用户ID
     * @param sessionId 新的会话ID
     * @return 被关闭的旧会话ID，如果没有旧会话则返回null
     */
    public String registerSession(Long userId, String sessionId) {
        if (userId == null || sessionId == null) {
            log.warn("Cannot register session: userId or sessionId is null");
            return null;
        }

        String oldSessionId = null;

        // 检查是否存在旧会话
        String existingSessionId = userSessions.get(userId);
        if (existingSessionId != null && !existingSessionId.equals(sessionId)) {
            // 存在旧会话，需要关闭
            oldSessionId = existingSessionId;
            log.info("User {} has existing session {}, will be replaced by new session {}", 
                    userId, existingSessionId, sessionId);
            
            // 移除旧会话的反向映射
            sessionUsers.remove(existingSessionId);
            
            // 通知旧会话被踢出（如果消息模板可用）
            notifySessionKicked(userId, existingSessionId);
        }

        // 注册新会话
        userSessions.put(userId, sessionId);
        sessionUsers.put(sessionId, userId);
        
        log.info("User {} registered with session {}", userId, sessionId);
        
        return oldSessionId;
    }

    /**
     * 移除会话
     * 根据会话ID移除用户会话映射
     * 
     * @param sessionId 要移除的会话ID
     * @return 被移除会话对应的用户ID，如果会话不存在则返回null
     */
    public Long removeSession(String sessionId) {
        if (sessionId == null) {
            log.warn("Cannot remove session: sessionId is null");
            return null;
        }

        Long userId = sessionUsers.remove(sessionId);
        if (userId != null) {
            // 只有当当前会话是用户的活跃会话时才移除用户映射
            // 这是为了防止在新会话注册后，旧会话断开时错误地移除新会话的映射
            userSessions.remove(userId, sessionId);
            log.info("Session {} removed for user {}", sessionId, userId);
        } else {
            log.debug("Session {} not found in session manager", sessionId);
        }
        
        return userId;
    }

    /**
     * 根据用户ID移除会话
     * 
     * @param userId 用户ID
     * @return 被移除的会话ID，如果用户没有会话则返回null
     */
    public String removeSessionByUserId(Long userId) {
        if (userId == null) {
            log.warn("Cannot remove session: userId is null");
            return null;
        }

        String sessionId = userSessions.remove(userId);
        if (sessionId != null) {
            sessionUsers.remove(sessionId);
            log.info("Session {} removed for user {} by userId", sessionId, userId);
        }
        
        return sessionId;
    }

    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return true如果用户有活跃的WebSocket会话
     */
    public boolean isUserOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        return userSessions.containsKey(userId);
    }

    /**
     * 获取用户的会话ID
     * 
     * @param userId 用户ID
     * @return 用户的会话ID，如果用户不在线则返回null
     */
    public String getSessionId(Long userId) {
        if (userId == null) {
            return null;
        }
        return userSessions.get(userId);
    }

    /**
     * 获取会话对应的用户ID
     * 
     * @param sessionId 会话ID
     * @return 用户ID，如果会话不存在则返回null
     */
    public Long getUserId(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionUsers.get(sessionId);
    }

    /**
     * 获取所有在线用户ID
     * 
     * @return 在线用户ID的不可变集合
     */
    public Set<Long> getOnlineUsers() {
        return Collections.unmodifiableSet(userSessions.keySet());
    }

    /**
     * 获取在线用户数量
     * 
     * @return 当前在线用户数
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }

    /**
     * 检查会话是否存在
     * 
     * @param sessionId 会话ID
     * @return true如果会话存在
     */
    public boolean hasSession(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        return sessionUsers.containsKey(sessionId);
    }

    /**
     * 通知旧会话被踢出
     * 向旧会话发送被踢出的消息
     */
    private void notifySessionKicked(Long userId, String oldSessionId) {
        if (messagingTemplate != null) {
            try {
                // 发送踢出通知到用户的私有队列
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        "/queue/kicked",
                        "您的账号在其他设备登录，当前连接已断开"
                );
                log.debug("Sent kick notification to user {} session {}", userId, oldSessionId);
            } catch (Exception e) {
                log.warn("Failed to send kick notification to user {}: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * 清除所有会话（用于测试或服务重启）
     */
    public void clearAllSessions() {
        int count = userSessions.size();
        userSessions.clear();
        sessionUsers.clear();
        log.info("Cleared all {} sessions", count);
    }
}
