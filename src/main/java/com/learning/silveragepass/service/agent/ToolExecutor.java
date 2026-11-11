package com.learning.silveragepass.service.agent;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 工具执行器接口
 * Tool Executor Interface
 * 
 * 负责工具的执行管理，提供：
 * - 参数验证
 * - 工具执行（同步/异步）
 * - 超时控制
 * - 重试机制
 * - 执行日志记录
 * - 指标收集
 * 
 * Requirements:
 * - 5.3: Agent framework validates tool parameters before execution
 * - 5.4: Agent framework executes tools and captures execution results
 * - 5.6: Tool executor logs all tool calls (tool name, parameters, result, duration)
 * - 4.4: When tool execution fails, agent framework retries or uses alternative
 * - 4.6: Agent framework supports task execution timeout control (default 30 seconds)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public interface ToolExecutor {
    
    /**
     * 执行工具（同步）
     * Execute tool synchronously
     * 
     * 根据工具名称查找工具，验证参数，执行工具并返回结果。
     * 
     * Requirement 5.4: Agent framework executes tools and captures execution results
     * 
     * @param toolName 工具名称 (tool name)
     * @param parameters 参数 (parameters)
     * @param context 执行上下文 (execution context)
     * @return 执行结果 (execution result)
     */
    ToolResult execute(String toolName, Map<String, Object> parameters, 
                       ToolExecutionContext context);
    
    /**
     * 执行工具（异步）
     * Execute tool asynchronously
     * 
     * 适用于长时间运行的工具，不阻塞调用线程。
     * 
     * @param toolName 工具名称 (tool name)
     * @param parameters 参数 (parameters)
     * @param context 执行上下文 (execution context)
     * @return 异步执行结果 (async execution result)
     */
    CompletableFuture<ToolResult> executeAsync(String toolName, Map<String, Object> parameters,
                                                ToolExecutionContext context);
    
    /**
     * 执行工具（带超时）
     * Execute tool with timeout
     * 
     * Requirement 4.6: Agent framework supports task execution timeout control
     * 
     * @param toolName 工具名称 (tool name)
     * @param parameters 参数 (parameters)
     * @param context 执行上下文 (execution context)
     * @param timeoutMs 超时时间（毫秒）(timeout in milliseconds)
     * @return 执行结果 (execution result)
     */
    ToolResult executeWithTimeout(String toolName, Map<String, Object> parameters,
                                  ToolExecutionContext context, long timeoutMs);
    
    /**
     * 执行工具（带重试）
     * Execute tool with retry
     * 
     * Requirement 4.4: When tool execution fails, agent framework retries
     * 
     * @param toolName 工具名称 (tool name)
     * @param parameters 参数 (parameters)
     * @param context 执行上下文 (execution context)
     * @param maxRetries 最大重试次数 (maximum retry count)
     * @return 执行结果 (execution result)
     */
    ToolResult executeWithRetry(String toolName, Map<String, Object> parameters,
                                ToolExecutionContext context, int maxRetries);
    
    /**
     * 验证工具参数
     * Validate tool parameters
     * 
     * Requirement 5.3: Agent framework validates tool parameters before execution
     * 
     * @param toolName 工具名称 (tool name)
     * @param parameters 参数 (parameters)
     * @return 验证结果 (validation result)
     */
    AgentTool.ValidationResult validateParameters(String toolName, Map<String, Object> parameters);
    
    /**
     * 检查工具是否存在
     * Check if tool exists
     * 
     * @param toolName 工具名称 (tool name)
     * @return true如果工具存在 (true if tool exists)
     */
    boolean hasToolAvailable(String toolName);
    
    /**
     * 获取执行指标
     * Get execution metrics
     * 
     * @return 执行指标 (execution metrics)
     */
    ExecutionMetrics getMetrics();
    
    /**
     * 获取指定工具的执行指标
     * Get execution metrics for specific tool
     * 
     * @param toolName 工具名称 (tool name)
     * @return 工具执行指标 (tool execution metrics)
     */
    ToolMetrics getToolMetrics(String toolName);
    
    /**
     * 重置指标
     * Reset metrics
     */
    void resetMetrics();
    
    /**
     * 执行指标
     * Execution Metrics
     * 
     * 记录工具执行的统计信息
     */
    record ExecutionMetrics(
            long totalExecutions,
            long successfulExecutions,
            long failedExecutions,
            double successRate,
            double averageDurationMs,
            long minDurationMs,
            long maxDurationMs,
            long totalRetries
    ) {
        /**
         * 创建空指标
         * Create empty metrics
         */
        public static ExecutionMetrics empty() {
            return new ExecutionMetrics(0, 0, 0, 0.0, 0.0, 0, 0, 0);
        }
    }
    
    /**
     * 工具执行指标
     * Tool Execution Metrics
     * 
     * 记录单个工具的执行统计信息
     */
    record ToolMetrics(
            String toolName,
            long executionCount,
            long successCount,
            long failureCount,
            double successRate,
            double averageDurationMs,
            long lastExecutionTimeMs,
            String lastErrorMessage
    ) {
        /**
         * 创建空指标
         * Create empty metrics
         */
        public static ToolMetrics empty(String toolName) {
            return new ToolMetrics(toolName, 0, 0, 0, 0.0, 0.0, 0, null);
        }
    }
}
