package com.learning.silveragepass.service.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行结果
 * Tool Execution Result
 * 
 * 封装Agent工具执行的结果，包括：
 * - 执行状态（成功/失败）
 * - 返回数据
 * - 错误信息
 * - 执行耗时
 * - 元数据
 * 
 * Requirements:
 * - 5.4: Agent framework executes tools and captures execution results
 * - 5.5: When tool execution fails, agent framework handles gracefully and provides fallback response
 * - 5.6: Tool executor logs all tool calls (tool name, parameters, result, duration)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult {
    
    /**
     * 执行是否成功
     * Whether execution was successful
     */
    private boolean success;
    
    /**
     * 返回数据
     * Return data
     * 
     * 工具执行成功时返回的数据
     */
    private Object data;
    
    /**
     * 错误信息
     * Error message
     * 
     * 工具执行失败时的错误描述
     */
    private String errorMessage;
    
    /**
     * 错误代码
     * Error code
     * 
     * 用于程序化处理错误
     */
    private String errorCode;
    
    /**
     * 执行耗时（毫秒）
     * Execution time in milliseconds
     * 
     * Requirement 5.6: Tool executor logs execution duration
     */
    private long executionTimeMs;
    
    /**
     * 执行时间戳
     * Execution timestamp
     */
    @Builder.Default
    private LocalDateTime executedAt = LocalDateTime.now();
    
    /**
     * 元数据
     * Metadata
     * 
     * 额外的执行信息，如：
     * - 重试次数
     * - 使用的替代方案
     * - 警告信息
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 是否需要用户确认
     * Whether user confirmation is required
     * 
     * Requirement 8.6: When tool needs user confirmation, agent framework pauses execution
     */
    private boolean requiresConfirmation;
    
    /**
     * 确认消息
     * Confirmation message
     * 
     * 需要用户确认时显示的消息
     */
    private String confirmationMessage;
    
    /**
     * 创建成功结果
     * Create success result
     * 
     * @param data 返回数据 (return data)
     * @return 成功的工具执行结果 (successful tool result)
     */
    public static ToolResult success(Object data) {
        return ToolResult.builder()
                .success(true)
                .data(data)
                .build();
    }
    
    /**
     * 创建成功结果（带执行时间）
     * Create success result with execution time
     * 
     * @param data 返回数据 (return data)
     * @param executionTimeMs 执行耗时 (execution time in ms)
     * @return 成功的工具执行结果 (successful tool result)
     */
    public static ToolResult success(Object data, long executionTimeMs) {
        return ToolResult.builder()
                .success(true)
                .data(data)
                .executionTimeMs(executionTimeMs)
                .build();
    }
    
    /**
     * 创建失败结果
     * Create failure result
     * 
     * @param errorMessage 错误信息 (error message)
     * @return 失败的工具执行结果 (failed tool result)
     */
    public static ToolResult failure(String errorMessage) {
        return ToolResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * 创建失败结果（带错误代码）
     * Create failure result with error code
     * 
     * @param errorCode 错误代码 (error code)
     * @param errorMessage 错误信息 (error message)
     * @return 失败的工具执行结果 (failed tool result)
     */
    public static ToolResult failure(String errorCode, String errorMessage) {
        return ToolResult.builder()
                .success(false)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * 创建失败结果（从异常）
     * Create failure result from exception
     * 
     * @param exception 异常 (exception)
     * @return 失败的工具执行结果 (failed tool result)
     */
    public static ToolResult failure(Throwable exception) {
        return ToolResult.builder()
                .success(false)
                .errorCode(exception.getClass().getSimpleName())
                .errorMessage(exception.getMessage())
                .build();
    }
    
    /**
     * 创建需要确认的结果
     * Create result requiring confirmation
     * 
     * @param confirmationMessage 确认消息 (confirmation message)
     * @param pendingData 待确认的数据 (pending data)
     * @return 需要确认的工具执行结果 (tool result requiring confirmation)
     */
    public static ToolResult requireConfirmation(String confirmationMessage, Object pendingData) {
        return ToolResult.builder()
                .success(true)
                .requiresConfirmation(true)
                .confirmationMessage(confirmationMessage)
                .data(pendingData)
                .build();
    }
    
    /**
     * 添加元数据
     * Add metadata
     * 
     * @param key 键 (key)
     * @param value 值 (value)
     * @return this for chaining
     */
    public ToolResult addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }
    
    /**
     * 获取元数据值
     * Get metadata value
     * 
     * @param key 键 (key)
     * @return 值 (value)
     */
    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }
    
    /**
     * 获取类型化的数据
     * Get typed data
     * 
     * @param <T> 数据类型 (data type)
     * @param clazz 类型类 (type class)
     * @return 类型化的数据 (typed data)
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> clazz) {
        if (data == null) {
            return null;
        }
        if (clazz.isInstance(data)) {
            return (T) data;
        }
        throw new ClassCastException("Cannot cast data to " + clazz.getName());
    }
    
    /**
     * 检查是否有错误代码
     * Check if has error code
     * 
     * @return true如果有错误代码 (true if has error code)
     */
    public boolean hasErrorCode() {
        return errorCode != null && !errorCode.isEmpty();
    }
    
    /**
     * 检查是否有数据
     * Check if has data
     * 
     * @return true如果有数据 (true if has data)
     */
    public boolean hasData() {
        return data != null;
    }
    
    /**
     * 获取用户友好的错误消息
     * Get user-friendly error message
     * 
     * Requirement 12.6: Health assistant provides friendly error prompts instead of technical errors
     * 
     * @return 用户友好的错误消息 (user-friendly error message)
     */
    public String getUserFriendlyErrorMessage() {
        if (success) {
            return null;
        }
        
        // 将技术错误转换为用户友好的消息
        if (errorCode != null) {
            switch (errorCode) {
                case "TIMEOUT":
                    return "操作超时，请稍后重试";
                case "NETWORK_ERROR":
                    return "网络连接出现问题，请检查网络后重试";
                case "INVALID_PARAMETER":
                    return "输入信息有误，请检查后重新输入";
                case "NOT_FOUND":
                    return "未找到相关信息";
                case "PERMISSION_DENIED":
                    return "您没有权限执行此操作";
                case "SERVICE_UNAVAILABLE":
                    return "服务暂时不可用，请稍后重试";
                default:
                    break;
            }
        }
        
        // 返回原始错误消息或默认消息
        return errorMessage != null ? errorMessage : "操作失败，请稍后重试";
    }
    
    /**
     * 转换为日志格式
     * Convert to log format
     * 
     * Requirement 5.6: Tool executor logs all tool calls
     * 
     * @param toolName 工具名称 (tool name)
     * @param parameters 参数 (parameters)
     * @return 日志格式字符串 (log format string)
     */
    public String toLogFormat(String toolName, Map<String, Object> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tool: ").append(toolName);
        sb.append(", Success: ").append(success);
        sb.append(", Duration: ").append(executionTimeMs).append("ms");
        
        if (!success) {
            sb.append(", Error: ").append(errorCode != null ? errorCode : "UNKNOWN");
            sb.append(" - ").append(errorMessage);
        }
        
        if (parameters != null && !parameters.isEmpty()) {
            sb.append(", Params: ").append(parameters);
        }
        
        return sb.toString();
    }
}
