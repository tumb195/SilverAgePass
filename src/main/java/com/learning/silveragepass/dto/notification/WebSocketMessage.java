package com.learning.silveragepass.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket消息包装对象
 * 用于统一WebSocket消息格式
 * @param <T> 消息内容类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {
    
    /**
     * 消息类型
     * notification - 新通知
     * unread_count - 未读数更新
     * heartbeat - 心跳响应
     * offline_messages - 离线消息批量推送
     * error - 错误消息
     */
    private String type;
    
    /**
     * 消息内容
     */
    private T payload;
    
    /**
     * 消息时间戳
     */
    private long timestamp;
    
    /**
     * 创建通知消息
     */
    public static <T> WebSocketMessage<T> notification(T payload) {
        return WebSocketMessage.<T>builder()
                .type("notification")
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建未读数更新消息
     */
    public static WebSocketMessage<UnreadCountDTO> unreadCount(UnreadCountDTO count) {
        return WebSocketMessage.<UnreadCountDTO>builder()
                .type("unread_count")
                .payload(count)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建心跳响应消息
     */
    public static WebSocketMessage<String> heartbeat() {
        return WebSocketMessage.<String>builder()
                .type("heartbeat")
                .payload("pong")
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建错误消息
     */
    public static WebSocketMessage<String> error(String message) {
        return WebSocketMessage.<String>builder()
                .type("error")
                .payload(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
