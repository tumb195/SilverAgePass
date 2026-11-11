package com.learning.silveragepass.config;

import com.learning.silveragepass.websocket.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置类
 * 配置STOMP消息代理和WebSocket端点
 * 
 * Requirements: 1.1, 1.2, 1.6
 * - 支持JWT认证的WebSocket连接
 * - 拒绝无效或过期的JWT Token
 * - 支持心跳消息处理
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    /**
     * 创建WebSocket心跳任务调度器
     * 用于处理STOMP心跳消息
     */
    @Bean
    public TaskScheduler websocketHeartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，客户端订阅前缀
        // /topic - 用于广播消息
        // /queue - 用于点对点消息
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{30000, 30000}) // 心跳间隔30秒
                .setTaskScheduler(websocketHeartbeatTaskScheduler()); // 设置心跳任务调度器
        
        // 客户端发送消息前缀
        config.setApplicationDestinationPrefixes("/app");
        
        // 用户目的地前缀（点对点消息）
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket端点，支持SockJS降级
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // 纯WebSocket端点（不使用SockJS，适用于原生WebSocket客户端）
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    /**
     * 配置客户端入站通道拦截器
     * 添加JWT认证拦截器，在CONNECT时验证Token
     * 
     * Requirements: 1.1, 1.2
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }
}
