package com.learning.silveragepass.service.agent;

import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.dto.rag.UserHealthProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行上下文
 * Tool Execution Context
 * 
 * 提供工具执行所需的上下文信息，包括：
 * - 用户信息
 * - 会话信息
 * - 对话上下文
 * - 用户健康画像
 * - 执行配置
 * 
 * Requirements:
 * - 7.2: Context manager maintains user health profile (basic info, health conditions, medication records)
 * - 7.3: When retrieving knowledge, context manager incorporates conversation context into query
 * - 13.2: Agent framework validates tool permissions before execution
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionContext {
    
    /**
     * 用户ID
     * User ID
     */
    private Long userId;
    
    /**
     * 会话ID
     * Session ID
     */
    private String sessionId;
    
    /**
     * 对话上下文
     * Conversation context
     * 
     * 包含对话历史和检索到的知识
     */
    private ConversationContext conversationContext;
    
    /**
     * 用户健康画像
     * User health profile
     * 
     * Requirement 7.2: Context manager maintains user health profile
     */
    private UserHealthProfile userHealthProfile;
    
    /**
     * 执行时间戳
     * Execution timestamp
     */
    @Builder.Default
    private LocalDateTime executionTime = LocalDateTime.now();
    
    /**
     * 执行超时时间（毫秒）
     * Execution timeout in milliseconds
     * 
     * Requirement 4.6: Agent framework supports task execution timeout control (default 30 seconds)
     */
    @Builder.Default
    private long timeoutMs = 30000;
    
    /**
     * 是否允许重试
     * Whether retry is allowed
     * 
     * Requirement 4.4: When tool execution fails, agent framework retries or uses alternative
     */
    @Builder.Default
    private boolean retryEnabled = true;
    
    /**
     * 最大重试次数
     * Maximum retry count
     */
    @Builder.Default
    private int maxRetries = 3;
    
    /**
     * 当前重试次数
     * Current retry count
     */
    @Builder.Default
    private int currentRetry = 0;
    
    /**
     * 用户权限列表
     * User permissions list
     * 
     * Requirement 13.2: Agent framework validates tool permissions before execution
     */
    @Builder.Default
    private Map<String, Boolean> permissions = new HashMap<>();
    
    /**
     * 额外属性
     * Additional attributes
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
    
    /**
     * 请求来源
     * Request source
     * 
     * 标识请求来自哪个渠道（如：chat, api, scheduled）
     */
    private String requestSource;
    
    /**
     * 请求ID
     * Request ID
     * 
     * 用于日志追踪
     */
    private String requestId;
    
    /**
     * 检查用户是否有指定权限
     * Check if user has specified permission
     * 
     * Requirement 13.2: Agent framework validates tool permissions before execution
     * 
     * @param permission 权限名称 (permission name)
     * @return true如果有权限 (true if has permission)
     */
    public boolean hasPermission(String permission) {
        if (permissions == null) {
            return false;
        }
        return Boolean.TRUE.equals(permissions.get(permission));
    }
    
    /**
     * 添加权限
     * Add permission
     * 
     * @param permission 权限名称 (permission name)
     * @param granted 是否授予 (whether granted)
     * @return this for chaining
     */
    public ToolExecutionContext addPermission(String permission, boolean granted) {
        if (permissions == null) {
            permissions = new HashMap<>();
        }
        permissions.put(permission, granted);
        return this;
    }
    
    /**
     * 添加属性
     * Add attribute
     * 
     * @param key 键 (key)
     * @param value 值 (value)
     * @return this for chaining
     */
    public ToolExecutionContext addAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
        return this;
    }
    
    /**
     * 获取属性
     * Get attribute
     * 
     * @param key 键 (key)
     * @return 值 (value)
     */
    public Object getAttribute(String key) {
        return attributes != null ? attributes.get(key) : null;
    }
    
    /**
     * 获取类型化的属性
     * Get typed attribute
     * 
     * @param <T> 类型 (type)
     * @param key 键 (key)
     * @param clazz 类型类 (type class)
     * @return 类型化的值 (typed value)
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> clazz) {
        Object value = getAttribute(key);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException("Cannot cast attribute to " + clazz.getName());
    }
    
    /**
     * 检查是否超时
     * Check if timeout
     * 
     * @return true如果已超时 (true if timeout)
     */
    public boolean isTimeout() {
        long elapsed = System.currentTimeMillis() - 
                executionTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return elapsed > timeoutMs;
    }
    
    /**
     * 获取剩余时间（毫秒）
     * Get remaining time in milliseconds
     * 
     * @return 剩余时间 (remaining time)
     */
    public long getRemainingTimeMs() {
        long elapsed = System.currentTimeMillis() - 
                executionTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return Math.max(0, timeoutMs - elapsed);
    }
    
    /**
     * 检查是否可以重试
     * Check if can retry
     * 
     * @return true如果可以重试 (true if can retry)
     */
    public boolean canRetry() {
        return retryEnabled && currentRetry < maxRetries && !isTimeout();
    }
    
    /**
     * 增加重试计数
     * Increment retry count
     * 
     * @return 新的重试计数 (new retry count)
     */
    public int incrementRetry() {
        return ++currentRetry;
    }
    
    /**
     * 创建基本上下文
     * Create basic context
     * 
     * @param userId 用户ID (user ID)
     * @param sessionId 会话ID (session ID)
     * @return 基本执行上下文 (basic execution context)
     */
    public static ToolExecutionContext basic(Long userId, String sessionId) {
        return ToolExecutionContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
    }
    
    /**
     * 创建带对话上下文的执行上下文
     * Create execution context with conversation context
     * 
     * @param userId 用户ID (user ID)
     * @param sessionId 会话ID (session ID)
     * @param conversationContext 对话上下文 (conversation context)
     * @return 执行上下文 (execution context)
     */
    public static ToolExecutionContext withConversation(Long userId, String sessionId, 
                                                        ConversationContext conversationContext) {
        return ToolExecutionContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .conversationContext(conversationContext)
                .build();
    }
    
    /**
     * 创建完整的执行上下文
     * Create full execution context
     * 
     * @param userId 用户ID (user ID)
     * @param sessionId 会话ID (session ID)
     * @param conversationContext 对话上下文 (conversation context)
     * @param userHealthProfile 用户健康画像 (user health profile)
     * @return 完整的执行上下文 (full execution context)
     */
    public static ToolExecutionContext full(Long userId, String sessionId,
                                            ConversationContext conversationContext,
                                            UserHealthProfile userHealthProfile) {
        return ToolExecutionContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .conversationContext(conversationContext)
                .userHealthProfile(userHealthProfile)
                .build();
    }
    
    /**
     * 复制上下文（用于重试）
     * Copy context (for retry)
     * 
     * @return 复制的上下文 (copied context)
     */
    public ToolExecutionContext copyForRetry() {
        return ToolExecutionContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .conversationContext(conversationContext)
                .userHealthProfile(userHealthProfile)
                .executionTime(executionTime)
                .timeoutMs(timeoutMs)
                .retryEnabled(retryEnabled)
                .maxRetries(maxRetries)
                .currentRetry(currentRetry + 1)
                .permissions(permissions != null ? new HashMap<>(permissions) : new HashMap<>())
                .attributes(attributes != null ? new HashMap<>(attributes) : new HashMap<>())
                .requestSource(requestSource)
                .requestId(requestId)
                .build();
    }
}
