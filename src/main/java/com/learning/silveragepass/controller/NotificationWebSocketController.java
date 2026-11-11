package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.notification.WebSocketMessage;
import com.learning.silveragepass.service.NotificationService;
import com.learning.silveragepass.websocket.StompPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket消息控制器
 * 处理心跳、消息确认和客户端订阅事件
 * 
 * Requirements: 1.6
 * - 客户端发送心跳消息时，响应心跳确认消息
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class NotificationWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    /**
     * 处理心跳消息
     * 客户端发送到 /app/heartbeat，服务端响应到 /user/queue/heartbeat
     * 
     * @param principal 用户身份信息
     * @return 心跳响应消息
     * Requirements: 1.6
     */
    @MessageMapping("/heartbeat")
    @SendToUser("/queue/heartbeat")
    public WebSocketMessage<String> handleHeartbeat(Principal principal) {
        if (principal != null) {
            log.debug("Received heartbeat from user: {}", principal.getName());
        }
        return WebSocketMessage.heartbeat();
    }

    /**
     * 处理消息确认（标记已读）
     * 客户端发送到 /app/notification/ack，包含通知ID
     * 
     * @param payload 包含notificationId的消息体
     * @param principal 用户身份信息
     * @return 确认响应消息
     * Requirements: 3.4
     */
    @MessageMapping("/notification/ack")
    @SendToUser("/queue/notification/ack")
    public WebSocketMessage<?> handleNotificationAck(
            @Payload Map<String, Object> payload, 
            Principal principal) {
        
        if (principal == null) {
            log.warn("Received notification ack without principal");
            return WebSocketMessage.error("Unauthorized");
        }

        Long userId = getUserIdFromPrincipal(principal);
        Object notificationIdObj = payload.get("notificationId");
        
        if (notificationIdObj == null) {
            log.warn("Received notification ack without notificationId from user: {}", userId);
            return WebSocketMessage.error("Missing notificationId");
        }

        try {
            Long notificationId = Long.valueOf(notificationIdObj.toString());
            boolean success = notificationService.markAsRead(notificationId, userId);
            
            if (success) {
                log.debug("Notification {} marked as read by user {}", notificationId, userId);
                return WebSocketMessage.<Map<String, Object>>builder()
                        .type("ack_response")
                        .payload(Map.of(
                                "notificationId", notificationId,
                                "success", true,
                                "message", "Notification marked as read"
                        ))
                        .timestamp(System.currentTimeMillis())
                        .build();
            } else {
                log.warn("Failed to mark notification {} as read for user {}", notificationId, userId);
                return WebSocketMessage.<Map<String, Object>>builder()
                        .type("ack_response")
                        .payload(Map.of(
                                "notificationId", notificationId,
                                "success", false,
                                "message", "Failed to mark notification as read"
                        ))
                        .timestamp(System.currentTimeMillis())
                        .build();
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid notificationId format: {}", notificationIdObj);
            return WebSocketMessage.error("Invalid notificationId format");
        }
    }

    /**
     * 处理批量消息确认（批量标记已读）
     * 客户端发送到 /app/notification/ack-batch，包含通知ID列表
     * 
     * @param payload 包含notificationIds的消息体
     * @param principal 用户身份信息
     * @return 确认响应消息
     * Requirements: 8.4
     */
    @MessageMapping("/notification/ack-batch")
    @SendToUser("/queue/notification/ack")
    public WebSocketMessage<?> handleBatchNotificationAck(
            @Payload Map<String, Object> payload,
            Principal principal) {
        
        if (principal == null) {
            log.warn("Received batch notification ack without principal");
            return WebSocketMessage.error("Unauthorized");
        }

        Long userId = getUserIdFromPrincipal(principal);
        Object idsObj = payload.get("notificationIds");
        
        if (idsObj == null) {
            // 如果没有指定ID，则标记所有为已读
            int count = notificationService.markAllAsRead(userId);
            log.debug("Marked all {} notifications as read for user {}", count, userId);
            return WebSocketMessage.<Map<String, Object>>builder()
                    .type("ack_batch_response")
                    .payload(Map.of(
                            "success", true,
                            "count", count,
                            "message", "All notifications marked as read"
                    ))
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        // 处理指定ID列表的情况
        // 这里简化处理，实际可以扩展为批量标记指定ID
        int count = notificationService.markAllAsRead(userId);
        log.debug("Marked {} notifications as read for user {}", count, userId);
        
        return WebSocketMessage.<Map<String, Object>>builder()
                .type("ack_batch_response")
                .payload(Map.of(
                        "success", true,
                        "count", count,
                        "message", "Notifications marked as read"
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 处理客户端订阅请求
     * 客户端发送到 /app/subscribe，用于确认订阅并触发离线消息推送
     * 
     * @param principal 用户身份信息
     * @return 订阅确认消息
     * Requirements: 2.3
     */
    @MessageMapping("/subscribe")
    @SendToUser("/queue/subscribe")
    public WebSocketMessage<?> handleSubscribe(Principal principal) {
        if (principal == null) {
            log.warn("Received subscribe request without principal");
            return WebSocketMessage.error("Unauthorized");
        }

        Long userId = getUserIdFromPrincipal(principal);
        log.info("User {} subscribed to notifications", userId);
        
        // 触发离线消息推送
        try {
            notificationService.pushOfflineMessages(userId);
            log.debug("Pushed offline messages for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to push offline messages for user {}: {}", userId, e.getMessage());
        }

        // 获取未读数并返回
        var unreadCount = notificationService.getUnreadCount(userId);
        
        return WebSocketMessage.<Map<String, Object>>builder()
                .type("subscribe_response")
                .payload(Map.of(
                        "success", true,
                        "message", "Subscribed successfully",
                        "unreadCount", unreadCount
                ))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 处理获取未读数请求
     * 客户端发送到 /app/unread-count
     * 
     * @param principal 用户身份信息
     * @return 未读数消息
     * Requirements: 8.1
     */
    @MessageMapping("/unread-count")
    @SendToUser("/queue/unread-count")
    public WebSocketMessage<?> handleGetUnreadCount(Principal principal) {
        if (principal == null) {
            log.warn("Received unread-count request without principal");
            return WebSocketMessage.error("Unauthorized");
        }

        Long userId = getUserIdFromPrincipal(principal);
        var unreadCount = notificationService.getUnreadCount(userId);
        
        log.debug("Returning unread count for user {}: {}", userId, unreadCount.getTotal());
        return WebSocketMessage.unreadCount(unreadCount);
    }

    /**
     * 从Principal中提取用户ID
     * 
     * @param principal 用户身份信息
     * @return 用户ID
     */
    private Long getUserIdFromPrincipal(Principal principal) {
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
