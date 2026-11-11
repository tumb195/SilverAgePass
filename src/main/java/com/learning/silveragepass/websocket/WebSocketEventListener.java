package com.learning.silveragepass.websocket;

import com.learning.silveragepass.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

/**
 * WebSocket事件监听器
 * 监听用户连接、断开和订阅事件，处理离线消息推送
 * 
 * Requirements: 2.3
 * - 离线用户重新上线时，推送该用户的所有离线消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final NotificationService notificationService;
    private final WebSocketSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 处理用户连接成功事件
     * 当WebSocket连接建立成功后触发，推送离线消息
     * 
     * @param event 连接成功事件
     * Requirements: 2.3
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        
        if (principal == null) {
            log.debug("Session connected without principal");
            return;
        }

        Long userId = getUserIdFromPrincipal(principal);
        String sessionId = accessor.getSessionId();
        
        if (userId != null) {
            log.info("User {} connected with session {}", userId, sessionId);
            
            // 设置消息模板到会话管理器（用于发送踢出通知）
            sessionManager.setMessagingTemplate(messagingTemplate);
            
            // 延迟推送离线消息，确保客户端已完成订阅
            // 使用异步方式避免阻塞连接过程
            pushOfflineMessagesAsync(userId);
        }
    }

    /**
     * 处理用户断开连接事件
     * 
     * @param event 断开连接事件
     * Requirements: 1.4
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();
        
        Long userId = null;
        if (principal != null) {
            userId = getUserIdFromPrincipal(principal);
        }
        
        if (userId != null) {
            log.info("User {} disconnected (session: {})", userId, sessionId);
        } else {
            log.debug("Session {} disconnected (no user mapping)", sessionId);
        }
        
        // 注意：会话清理已在JwtChannelInterceptor的DISCONNECT处理中完成
        // 这里主要用于日志记录和额外的清理工作
    }

    /**
     * 处理用户订阅事件
     * 当用户订阅特定主题时触发
     * 
     * @param event 订阅事件
     */
    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        String destination = accessor.getDestination();
        
        if (principal != null && destination != null) {
            Long userId = getUserIdFromPrincipal(principal);
            log.debug("User {} subscribed to {}", userId, destination);
            
            // 如果用户订阅了通知队列，触发离线消息推送
            if (destination.contains("/queue/notifications") || 
                destination.contains("/user/queue/notifications")) {
                pushOfflineMessagesAsync(userId);
            }
        }
    }


    /**
     * 异步推送离线消息
     * 使用新线程推送，避免阻塞事件处理
     * 
     * @param userId 用户ID
     */
    private void pushOfflineMessagesAsync(Long userId) {
        if (userId == null) {
            return;
        }
        
        // 使用新线程异步推送，并添加短暂延迟确保客户端已准备好接收
        new Thread(() -> {
            try {
                // 等待500ms，确保客户端已完成订阅
                Thread.sleep(500);
                
                // 检查用户是否仍然在线
                if (sessionManager.isUserOnline(userId)) {
                    log.debug("Pushing offline messages for user {}", userId);
                    notificationService.pushOfflineMessages(userId);
                } else {
                    log.debug("User {} is no longer online, skipping offline message push", userId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting to push offline messages for user {}", userId);
            } catch (Exception e) {
                log.error("Failed to push offline messages for user {}: {}", userId, e.getMessage(), e);
            }
        }, "offline-msg-push-" + userId).start();
    }

    /**
     * 从Principal中提取用户ID
     * 
     * @param principal 用户身份信息
     * @return 用户ID，如果无法提取则返回null
     */
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }
        
        if (principal instanceof StompPrincipal) {
            return ((StompPrincipal) principal).getUserId();
        }
        
        // 尝试从name解析
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse userId from principal name: {}", principal.getName());
            return null;
        }
    }
}
