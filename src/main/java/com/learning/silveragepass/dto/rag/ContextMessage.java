package com.learning.silveragepass.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 上下文消息DTO
 * Context Message DTO
 * 
 * 表示对话上下文中的单条消息，包含角色、内容和时间戳。
 * 用于ConversationContext中存储对话历史。
 * 
 * Requirements:
 * - 7.1: Context manager maintains conversation history (up to 20 messages)
 * - 7.4: Context manager assigns higher weight to recent messages
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextMessage {
    
    /**
     * 消息角色常量
     * Message role constants
     */
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";
    
    /**
     * 消息角色
     * Message role
     * 
     * 可选值：
     * - user: 用户消息
     * - assistant: 助手消息
     * - system: 系统消息
     */
    private String role;
    
    /**
     * 消息内容
     * Message content
     */
    private String content;
    
    /**
     * 消息时间戳
     * Message timestamp
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 消息元数据
     * Message metadata
     * 
     * 可选的额外信息，如：
     * - 情绪分析结果
     * - 意图识别结果
     * - 工具调用信息
     */
    private java.util.Map<String, Object> metadata;
    
    /**
     * 消息权重
     * Message weight
     * 
     * 用于上下文管理时的权重计算
     * 默认为1.0，近期消息可以设置更高权重
     * 
     * Requirement 7.4: Context manager assigns higher weight to recent messages
     */
    @Builder.Default
    private double weight = 1.0;
    
    /**
     * 检查是否为用户消息
     * Check if is user message
     * 
     * @return true如果是用户消息 (true if is user message)
     */
    public boolean isUserMessage() {
        return ROLE_USER.equals(role);
    }
    
    /**
     * 检查是否为助手消息
     * Check if is assistant message
     * 
     * @return true如果是助手消息 (true if is assistant message)
     */
    public boolean isAssistantMessage() {
        return ROLE_ASSISTANT.equals(role);
    }
    
    /**
     * 检查是否为系统消息
     * Check if is system message
     * 
     * @return true如果是系统消息 (true if is system message)
     */
    public boolean isSystemMessage() {
        return ROLE_SYSTEM.equals(role);
    }
    
    /**
     * 获取消息的年龄（秒）
     * Get message age in seconds
     * 
     * @return 消息年龄（秒） (message age in seconds)
     */
    public long getAgeInSeconds() {
        if (timestamp == null) {
            return 0;
        }
        return java.time.Duration.between(timestamp, LocalDateTime.now()).getSeconds();
    }
    
    /**
     * 获取消息的年龄（分钟）
     * Get message age in minutes
     * 
     * @return 消息年龄（分钟） (message age in minutes)
     */
    public long getAgeInMinutes() {
        return getAgeInSeconds() / 60;
    }
    
    /**
     * 设置元数据属性
     * Set metadata attribute
     * 
     * @param key 属性键 (attribute key)
     * @param value 属性值 (attribute value)
     */
    public void setMetadataAttribute(String key, Object value) {
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
        metadata.put(key, value);
    }
    
    /**
     * 获取元数据属性
     * Get metadata attribute
     * 
     * @param key 属性键 (attribute key)
     * @return 属性值 (attribute value)
     */
    public Object getMetadataAttribute(String key) {
        if (metadata == null) {
            return null;
        }
        return metadata.get(key);
    }
    
    /**
     * 获取内容长度
     * Get content length
     * 
     * @return 内容长度 (content length)
     */
    public int getContentLength() {
        return content != null ? content.length() : 0;
    }
    
    /**
     * 获取内容摘要
     * Get content summary
     * 
     * @param maxLength 最大长度 (maximum length)
     * @return 内容摘要 (content summary)
     */
    public String getContentSummary(int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
    
    /**
     * 创建用户消息
     * Create user message
     * 
     * @param content 消息内容 (message content)
     * @return 消息实例 (message instance)
     */
    public static ContextMessage userMessage(String content) {
        return ContextMessage.builder()
                .role(ROLE_USER)
                .content(content)
                .timestamp(LocalDateTime.now())
                .weight(1.0)
                .build();
    }
    
    /**
     * 创建助手消息
     * Create assistant message
     * 
     * @param content 消息内容 (message content)
     * @return 消息实例 (message instance)
     */
    public static ContextMessage assistantMessage(String content) {
        return ContextMessage.builder()
                .role(ROLE_ASSISTANT)
                .content(content)
                .timestamp(LocalDateTime.now())
                .weight(1.0)
                .build();
    }
    
    /**
     * 创建系统消息
     * Create system message
     * 
     * @param content 消息内容 (message content)
     * @return 消息实例 (message instance)
     */
    public static ContextMessage systemMessage(String content) {
        return ContextMessage.builder()
                .role(ROLE_SYSTEM)
                .content(content)
                .timestamp(LocalDateTime.now())
                .weight(1.0)
                .build();
    }
    
    /**
     * 创建带权重的用户消息
     * Create user message with weight
     * 
     * @param content 消息内容 (message content)
     * @param weight 权重 (weight)
     * @return 消息实例 (message instance)
     */
    public static ContextMessage userMessage(String content, double weight) {
        return ContextMessage.builder()
                .role(ROLE_USER)
                .content(content)
                .timestamp(LocalDateTime.now())
                .weight(weight)
                .build();
    }
    
    /**
     * 创建带权重的助手消息
     * Create assistant message with weight
     * 
     * @param content 消息内容 (message content)
     * @param weight 权重 (weight)
     * @return 消息实例 (message instance)
     */
    public static ContextMessage assistantMessage(String content, double weight) {
        return ContextMessage.builder()
                .role(ROLE_ASSISTANT)
                .content(content)
                .timestamp(LocalDateTime.now())
                .weight(weight)
                .build();
    }
    
    /**
     * 创建带时间戳的消息
     * Create message with timestamp
     * 
     * @param role 角色 (role)
     * @param content 内容 (content)
     * @param timestamp 时间戳 (timestamp)
     * @return 消息实例 (message instance)
     */
    public static ContextMessage of(String role, String content, LocalDateTime timestamp) {
        return ContextMessage.builder()
                .role(role)
                .content(content)
                .timestamp(timestamp)
                .weight(1.0)
                .build();
    }
    
    /**
     * 创建完整消息
     * Create full message
     * 
     * @param role 角色 (role)
     * @param content 内容 (content)
     * @param timestamp 时间戳 (timestamp)
     * @param weight 权重 (weight)
     * @return 消息实例 (message instance)
     */
    public static ContextMessage of(String role, String content, 
                                    LocalDateTime timestamp, double weight) {
        return ContextMessage.builder()
                .role(role)
                .content(content)
                .timestamp(timestamp)
                .weight(weight)
                .build();
    }
}
