package com.learning.silveragepass.service.agent.impl;

import com.learning.silveragepass.service.agent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 工具执行器实现
 * Tool Executor Implementation
 * 
 * 提供完整的工具执行管理功能：
 * - 参数验证：执行前验证参数是否符合工具的参数模式
 * - 超时控制：支持配置执行超时时间
 * - 重试机制：失败时自动重试
 * - 执行日志：记录所有工具调用的详细信息
 * - 指标收集：统计执行次数、成功率、平均耗时等
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
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolExecutorImpl implements ToolExecutor {
    
    private final ToolRegistry toolRegistry;
    
    /**
     * 异步执行线程池
     * Async execution thread pool
     */
    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "tool-executor-");
        t.setDaemon(true);
        return t;
    });
    
    /**
     * 默认超时时间（毫秒）
     * Default timeout in milliseconds
     */
    private static final long DEFAULT_TIMEOUT_MS = 30000;
    
    /**
     * 默认最大重试次数
     * Default maximum retry count
     */
    private static final int DEFAULT_MAX_RETRIES = 3;
    
    /**
     * 重试间隔（毫秒）
     * Retry interval in milliseconds
     */
    private static final long RETRY_INTERVAL_MS = 1000;
    
    // ========== 指标统计 ==========
    
    private final LongAdder totalExecutions = new LongAdder();
    private final LongAdder successfulExecutions = new LongAdder();
    private final LongAdder failedExecutions = new LongAdder();
    private final LongAdder totalRetries = new LongAdder();
    private final LongAdder totalDurationMs = new LongAdder();
    private final AtomicLong minDurationMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxDurationMs = new AtomicLong(0);
    
    /**
     * 工具级别指标
     * Tool-level metrics
     */
    private final ConcurrentHashMap<String, ToolMetricsCollector> toolMetrics = new ConcurrentHashMap<>();
    
    @Override
    public ToolResult execute(String toolName, Map<String, Object> parameters, 
                              ToolExecutionContext context) {
        long startTime = System.currentTimeMillis();
        
        // 记录执行开始
        log.info("Tool execution started: tool={}, params={}, userId={}, sessionId={}",
                toolName, sanitizeParameters(parameters), 
                context.getUserId(), context.getSessionId());
        
        try {
            // 1. 查找工具
            Optional<AgentTool> toolOpt = toolRegistry.getTool(toolName);
            if (toolOpt.isEmpty()) {
                log.warn("Tool not found: {}", toolName);
                return recordFailure(toolName, startTime, 
                        ToolResult.failure("TOOL_NOT_FOUND", "工具不存在: " + toolName));
            }
            
            AgentTool tool = toolOpt.get();
            
            // 2. 验证参数
            AgentTool.ValidationResult validation = tool.validateParameters(parameters);
            if (!validation.isValid()) {
                log.warn("Parameter validation failed for tool {}: {}", 
                        toolName, validation.getMessage());
                return recordFailure(toolName, startTime,
                        ToolResult.failure("INVALID_PARAMETER", validation.getMessage()));
            }
            
            // 3. 执行工具
            ToolResult result = tool.executeSafely(parameters, context);
            
            // 4. 记录结果
            long duration = System.currentTimeMillis() - startTime;
            result.setExecutionTimeMs(duration);
            
            if (result.isSuccess()) {
                recordSuccess(toolName, duration);
                log.info("Tool execution succeeded: tool={}, duration={}ms", toolName, duration);
            } else {
                recordFailure(toolName, startTime, result);
                log.warn("Tool execution failed: tool={}, error={}, duration={}ms",
                        toolName, result.getErrorMessage(), duration);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Tool execution error: tool={}, error={}", toolName, e.getMessage(), e);
            return recordFailure(toolName, startTime, ToolResult.failure(e));
        }
    }
    
    @Override
    public CompletableFuture<ToolResult> executeAsync(String toolName, Map<String, Object> parameters,
                                                       ToolExecutionContext context) {
        log.info("Async tool execution started: tool={}", toolName);
        
        return CompletableFuture.supplyAsync(() -> {
            return execute(toolName, parameters, context);
        }, executorService).exceptionally(throwable -> {
            log.error("Async tool execution failed: tool={}, error={}", 
                    toolName, throwable.getMessage(), throwable);
            return ToolResult.failure(throwable.getCause() != null ? 
                    throwable.getCause() : throwable);
        });
    }
    
    @Override
    public ToolResult executeWithTimeout(String toolName, Map<String, Object> parameters,
                                         ToolExecutionContext context, long timeoutMs) {
        log.info("Tool execution with timeout started: tool={}, timeout={}ms", toolName, timeoutMs);
        
        // 更新上下文的超时设置
        ToolExecutionContext timeoutContext = ToolExecutionContext.builder()
                .userId(context.getUserId())
                .sessionId(context.getSessionId())
                .conversationContext(context.getConversationContext())
                .userHealthProfile(context.getUserHealthProfile())
                .executionTime(context.getExecutionTime())
                .timeoutMs(timeoutMs)
                .retryEnabled(context.isRetryEnabled())
                .maxRetries(context.getMaxRetries())
                .currentRetry(context.getCurrentRetry())
                .permissions(context.getPermissions())
                .attributes(context.getAttributes())
                .requestSource(context.getRequestSource())
                .requestId(context.getRequestId())
                .build();
        
        CompletableFuture<ToolResult> future = executeAsync(toolName, parameters, timeoutContext);
        
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Tool execution timeout: tool={}, timeout={}ms", toolName, timeoutMs);
            future.cancel(true);
            return ToolResult.failure("TIMEOUT", "工具执行超时");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Tool execution interrupted: tool={}", toolName);
            return ToolResult.failure("INTERRUPTED", "工具执行被中断");
        } catch (ExecutionException e) {
            log.error("Tool execution error: tool={}, error={}", toolName, e.getMessage(), e);
            return ToolResult.failure(e.getCause() != null ? e.getCause() : e);
        }
    }
    
    @Override
    public ToolResult executeWithRetry(String toolName, Map<String, Object> parameters,
                                       ToolExecutionContext context, int maxRetries) {
        log.info("Tool execution with retry started: tool={}, maxRetries={}", toolName, maxRetries);
        
        int retryCount = 0;
        ToolResult lastResult = null;
        
        while (retryCount <= maxRetries) {
            if (retryCount > 0) {
                log.info("Retrying tool execution: tool={}, attempt={}/{}", 
                        toolName, retryCount, maxRetries);
                totalRetries.increment();
                
                // 等待重试间隔
                try {
                    Thread.sleep(RETRY_INTERVAL_MS * retryCount); // 指数退避
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return ToolResult.failure("INTERRUPTED", "重试被中断");
                }
            }
            
            // 更新上下文的重试计数
            ToolExecutionContext retryContext = context.copyForRetry();
            
            lastResult = execute(toolName, parameters, retryContext);
            
            if (lastResult.isSuccess()) {
                if (retryCount > 0) {
                    log.info("Tool execution succeeded after {} retries: tool={}", 
                            retryCount, toolName);
                }
                return lastResult;
            }
            
            // 检查是否可重试的错误
            if (!isRetryableError(lastResult)) {
                log.warn("Non-retryable error, stopping retry: tool={}, error={}", 
                        toolName, lastResult.getErrorCode());
                break;
            }
            
            retryCount++;
        }
        
        log.warn("Tool execution failed after {} retries: tool={}, lastError={}", 
                retryCount, toolName, lastResult != null ? lastResult.getErrorMessage() : "unknown");
        
        return lastResult != null ? lastResult : 
                ToolResult.failure("MAX_RETRIES_EXCEEDED", "已达到最大重试次数");
    }
    
    @Override
    public AgentTool.ValidationResult validateParameters(String toolName, Map<String, Object> parameters) {
        Optional<AgentTool> toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            return AgentTool.ValidationResult.invalid("工具不存在: " + toolName);
        }
        
        return toolOpt.get().validateParameters(parameters);
    }
    
    @Override
    public boolean hasToolAvailable(String toolName) {
        return toolRegistry.getTool(toolName).isPresent();
    }
    
    @Override
    public ExecutionMetrics getMetrics() {
        long total = totalExecutions.sum();
        long successful = successfulExecutions.sum();
        long failed = failedExecutions.sum();
        long totalDuration = totalDurationMs.sum();
        
        double successRate = total > 0 ? (double) successful / total * 100 : 0.0;
        double avgDuration = total > 0 ? (double) totalDuration / total : 0.0;
        
        long min = minDurationMs.get();
        long max = maxDurationMs.get();
        
        return new ExecutionMetrics(
                total,
                successful,
                failed,
                successRate,
                avgDuration,
                min == Long.MAX_VALUE ? 0 : min,
                max,
                totalRetries.sum()
        );
    }
    
    @Override
    public ToolMetrics getToolMetrics(String toolName) {
        ToolMetricsCollector collector = toolMetrics.get(toolName);
        if (collector == null) {
            return ToolMetrics.empty(toolName);
        }
        return collector.toMetrics();
    }
    
    @Override
    public void resetMetrics() {
        totalExecutions.reset();
        successfulExecutions.reset();
        failedExecutions.reset();
        totalRetries.reset();
        totalDurationMs.reset();
        minDurationMs.set(Long.MAX_VALUE);
        maxDurationMs.set(0);
        toolMetrics.clear();
        
        log.info("Execution metrics reset");
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 记录成功执行
     * Record successful execution
     */
    private void recordSuccess(String toolName, long durationMs) {
        totalExecutions.increment();
        successfulExecutions.increment();
        totalDurationMs.add(durationMs);
        updateMinMax(durationMs);
        
        // 更新工具级别指标
        toolMetrics.computeIfAbsent(toolName, ToolMetricsCollector::new)
                .recordSuccess(durationMs);
    }
    
    /**
     * 记录失败执行
     * Record failed execution
     */
    private ToolResult recordFailure(String toolName, long startTime, ToolResult result) {
        long durationMs = System.currentTimeMillis() - startTime;
        result.setExecutionTimeMs(durationMs);
        
        totalExecutions.increment();
        failedExecutions.increment();
        totalDurationMs.add(durationMs);
        updateMinMax(durationMs);
        
        // 更新工具级别指标
        toolMetrics.computeIfAbsent(toolName, ToolMetricsCollector::new)
                .recordFailure(durationMs, result.getErrorMessage());
        
        return result;
    }
    
    /**
     * 更新最小/最大耗时
     * Update min/max duration
     */
    private void updateMinMax(long durationMs) {
        minDurationMs.updateAndGet(current -> Math.min(current, durationMs));
        maxDurationMs.updateAndGet(current -> Math.max(current, durationMs));
    }
    
    /**
     * 检查是否可重试的错误
     * Check if error is retryable
     */
    private boolean isRetryableError(ToolResult result) {
        if (result == null || result.getErrorCode() == null) {
            return true;
        }
        
        // 不可重试的错误类型
        return switch (result.getErrorCode()) {
            case "TOOL_NOT_FOUND",
                 "INVALID_PARAMETER",
                 "PERMISSION_DENIED",
                 "TOOL_DISABLED" -> false;
            default -> true;
        };
    }
    
    /**
     * 清理敏感参数用于日志
     * Sanitize parameters for logging
     */
    private Map<String, Object> sanitizeParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }
        
        // 创建副本并隐藏敏感字段
        java.util.HashMap<String, Object> sanitized = new java.util.HashMap<>(parameters);
        
        // 隐藏可能的敏感字段
        for (String key : sanitized.keySet()) {
            String lowerKey = key.toLowerCase();
            if (lowerKey.contains("password") || 
                lowerKey.contains("token") || 
                lowerKey.contains("secret") ||
                lowerKey.contains("key")) {
                sanitized.put(key, "***");
            }
        }
        
        return sanitized;
    }
    
    /**
     * 工具指标收集器
     * Tool Metrics Collector
     */
    private static class ToolMetricsCollector {
        private final String toolName;
        private final LongAdder executionCount = new LongAdder();
        private final LongAdder successCount = new LongAdder();
        private final LongAdder failureCount = new LongAdder();
        private final LongAdder totalDurationMs = new LongAdder();
        private volatile long lastExecutionTimeMs = 0;
        private volatile String lastErrorMessage = null;
        
        ToolMetricsCollector(String toolName) {
            this.toolName = toolName;
        }
        
        void recordSuccess(long durationMs) {
            executionCount.increment();
            successCount.increment();
            totalDurationMs.add(durationMs);
            lastExecutionTimeMs = durationMs;
        }
        
        void recordFailure(long durationMs, String errorMessage) {
            executionCount.increment();
            failureCount.increment();
            totalDurationMs.add(durationMs);
            lastExecutionTimeMs = durationMs;
            lastErrorMessage = errorMessage;
        }
        
        ToolMetrics toMetrics() {
            long total = executionCount.sum();
            long success = successCount.sum();
            long failure = failureCount.sum();
            double successRate = total > 0 ? (double) success / total * 100 : 0.0;
            double avgDuration = total > 0 ? (double) totalDurationMs.sum() / total : 0.0;
            
            return new ToolMetrics(
                    toolName,
                    total,
                    success,
                    failure,
                    successRate,
                    avgDuration,
                    lastExecutionTimeMs,
                    lastErrorMessage
            );
        }
    }
}
