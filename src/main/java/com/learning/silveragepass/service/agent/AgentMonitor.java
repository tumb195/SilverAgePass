package com.learning.silveragepass.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent监控器
 * Agent Monitor
 * 
 * 记录和监控Agent系统的性能指标。
 * 
 * Requirements:
 * - 14.2: Health assistant logs all agent task executions (plan, tools, results)
 * - 5.6: Tool executor logs all tool calls (tool name, parameters, result, duration)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Component
public class AgentMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(AgentMonitor.class);
    
    /**
     * 任务执行计数器
     */
    private final AtomicLong taskCount = new AtomicLong(0);
    private final AtomicLong taskSuccessCount = new AtomicLong(0);
    private final AtomicLong taskFailureCount = new AtomicLong(0);
    
    /**
     * 工具调用计数器
     */
    private final AtomicLong toolCallCount = new AtomicLong(0);
    private final AtomicLong toolCallSuccessCount = new AtomicLong(0);
    private final AtomicLong toolCallFailureCount = new AtomicLong(0);
    
    /**
     * 延迟统计
     */
    private final AtomicLong totalTaskLatencyMs = new AtomicLong(0);
    private final AtomicLong totalToolCallLatencyMs = new AtomicLong(0);
    
    /**
     * 工具调用统计（按工具名称）
     */
    private final Map<String, ToolStats> toolStatsMap = new ConcurrentHashMap<>();
    
    /**
     * 最近的任务记录
     */
    private final List<TaskExecutionRecord> recentTasks = new ArrayList<>();
    private static final int MAX_RECENT_RECORDS = 100;
    
    /**
     * 记录任务执行
     * Record task execution
     * 
     * @param planId 计划ID (plan ID)
     * @param stepCount 步骤数量 (step count)
     * @param completedSteps 完成的步骤数 (completed steps)
     * @param latencyMs 延迟毫秒 (latency in milliseconds)
     * @param success 是否成功 (whether successful)
     */
    public void recordTaskExecution(String planId, int stepCount, int completedSteps, 
                                    long latencyMs, boolean success) {
        taskCount.incrementAndGet();
        totalTaskLatencyMs.addAndGet(latencyMs);
        
        if (success) {
            taskSuccessCount.incrementAndGet();
        } else {
            taskFailureCount.incrementAndGet();
        }
        
        // 记录详细信息
        TaskExecutionRecord record = new TaskExecutionRecord();
        record.setPlanId(planId);
        record.setStepCount(stepCount);
        record.setCompletedSteps(completedSteps);
        record.setLatencyMs(latencyMs);
        record.setSuccess(success);
        record.setTimestamp(LocalDateTime.now());
        
        addRecentTask(record);
        
        // 日志记录
        log.info("Agent Task: planId='{}', steps={}/{}, latency={}ms, success={}",
                planId, completedSteps, stepCount, latencyMs, success);
    }
    
    /**
     * 记录工具调用
     * Record tool call
     * 
     * @param toolName 工具名称 (tool name)
     * @param parameters 参数 (parameters)
     * @param latencyMs 延迟毫秒 (latency in milliseconds)
     * @param success 是否成功 (whether successful)
     * @param errorMessage 错误消息（如果失败）(error message if failed)
     */
    public void recordToolCall(String toolName, Map<String, Object> parameters, 
                               long latencyMs, boolean success, String errorMessage) {
        toolCallCount.incrementAndGet();
        totalToolCallLatencyMs.addAndGet(latencyMs);
        
        if (success) {
            toolCallSuccessCount.incrementAndGet();
        } else {
            toolCallFailureCount.incrementAndGet();
        }
        
        // 更新工具统计
        toolStatsMap.computeIfAbsent(toolName, k -> new ToolStats(toolName))
                .recordCall(latencyMs, success);
        
        // 日志记录
        if (success) {
            log.info("Tool Call: tool='{}', params={}, latency={}ms, success=true",
                    toolName, sanitizeParameters(parameters), latencyMs);
        } else {
            log.warn("Tool Call: tool='{}', params={}, latency={}ms, success=false, error='{}'",
                    toolName, sanitizeParameters(parameters), latencyMs, errorMessage);
        }
    }
    
    /**
     * 获取Agent指标
     * Get Agent metrics
     * 
     * @return Agent指标 (Agent metrics)
     */
    public AgentMetrics getMetrics() {
        AgentMetrics metrics = new AgentMetrics();
        
        // 任务指标
        metrics.setTaskCount(taskCount.get());
        metrics.setTaskSuccessCount(taskSuccessCount.get());
        metrics.setTaskFailureCount(taskFailureCount.get());
        metrics.setTaskSuccessRate(calculateSuccessRate(
                taskSuccessCount.get(), taskCount.get()));
        metrics.setAverageTaskLatencyMs(calculateAverageLatency(
                totalTaskLatencyMs.get(), taskCount.get()));
        
        // 工具调用指标
        metrics.setToolCallCount(toolCallCount.get());
        metrics.setToolCallSuccessCount(toolCallSuccessCount.get());
        metrics.setToolCallFailureCount(toolCallFailureCount.get());
        metrics.setToolCallSuccessRate(calculateSuccessRate(
                toolCallSuccessCount.get(), toolCallCount.get()));
        metrics.setAverageToolCallLatencyMs(calculateAverageLatency(
                totalToolCallLatencyMs.get(), toolCallCount.get()));
        
        // 工具统计
        metrics.setToolStats(new HashMap<>(toolStatsMap));
        
        return metrics;
    }
    
    /**
     * 获取工具统计
     * Get tool statistics
     * 
     * @param toolName 工具名称 (tool name)
     * @return 工具统计 (tool statistics)
     */
    public ToolStats getToolStats(String toolName) {
        return toolStatsMap.get(toolName);
    }
    
    /**
     * 获取所有工具统计
     * Get all tool statistics
     * 
     * @return 工具统计映射 (tool statistics map)
     */
    public Map<String, ToolStats> getAllToolStats() {
        return new HashMap<>(toolStatsMap);
    }
    
    /**
     * 获取最近的任务记录
     * Get recent task records
     * 
     * @param limit 限制数量 (limit)
     * @return 任务记录列表 (list of task records)
     */
    public List<TaskExecutionRecord> getRecentTasks(int limit) {
        synchronized (recentTasks) {
            int size = recentTasks.size();
            int start = Math.max(0, size - limit);
            return new ArrayList<>(recentTasks.subList(start, size));
        }
    }
    
    /**
     * 重置指标
     * Reset metrics
     */
    public void resetMetrics() {
        taskCount.set(0);
        taskSuccessCount.set(0);
        taskFailureCount.set(0);
        toolCallCount.set(0);
        toolCallSuccessCount.set(0);
        toolCallFailureCount.set(0);
        totalTaskLatencyMs.set(0);
        totalToolCallLatencyMs.set(0);
        toolStatsMap.clear();
        
        synchronized (recentTasks) {
            recentTasks.clear();
        }
        
        log.info("Agent metrics reset");
    }
    
    // ==========================================
    // Private Helper Methods
    // ==========================================
    
    private void addRecentTask(TaskExecutionRecord record) {
        synchronized (recentTasks) {
            recentTasks.add(record);
            while (recentTasks.size() > MAX_RECENT_RECORDS) {
                recentTasks.remove(0);
            }
        }
    }
    
    private double calculateSuccessRate(long successCount, long totalCount) {
        if (totalCount == 0) {
            return 0.0;
        }
        return (double) successCount / totalCount * 100.0;
    }
    
    private double calculateAverageLatency(long totalLatency, long count) {
        if (count == 0) {
            return 0.0;
        }
        return (double) totalLatency / count;
    }
    
    private Map<String, Object> sanitizeParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }
        
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 隐藏敏感参数
            if (isSensitiveParameter(key)) {
                sanitized.put(key, "***");
            } else if (value instanceof String && ((String) value).length() > 100) {
                sanitized.put(key, ((String) value).substring(0, 100) + "...");
            } else {
                sanitized.put(key, value);
            }
        }
        return sanitized;
    }
    
    private boolean isSensitiveParameter(String paramName) {
        if (paramName == null) {
            return false;
        }
        String lower = paramName.toLowerCase();
        return lower.contains("password") ||
               lower.contains("token") ||
               lower.contains("secret") ||
               lower.contains("key") ||
               lower.contains("phone") ||
               lower.contains("id_number");
    }
    
    /**
     * Agent指标类
     */
    public static class AgentMetrics {
        private long taskCount;
        private long taskSuccessCount;
        private long taskFailureCount;
        private double taskSuccessRate;
        private double averageTaskLatencyMs;
        
        private long toolCallCount;
        private long toolCallSuccessCount;
        private long toolCallFailureCount;
        private double toolCallSuccessRate;
        private double averageToolCallLatencyMs;
        
        private Map<String, ToolStats> toolStats;
        
        // Getters and Setters
        public long getTaskCount() { return taskCount; }
        public void setTaskCount(long taskCount) { this.taskCount = taskCount; }
        
        public long getTaskSuccessCount() { return taskSuccessCount; }
        public void setTaskSuccessCount(long taskSuccessCount) { this.taskSuccessCount = taskSuccessCount; }
        
        public long getTaskFailureCount() { return taskFailureCount; }
        public void setTaskFailureCount(long taskFailureCount) { this.taskFailureCount = taskFailureCount; }
        
        public double getTaskSuccessRate() { return taskSuccessRate; }
        public void setTaskSuccessRate(double taskSuccessRate) { this.taskSuccessRate = taskSuccessRate; }
        
        public double getAverageTaskLatencyMs() { return averageTaskLatencyMs; }
        public void setAverageTaskLatencyMs(double averageTaskLatencyMs) { this.averageTaskLatencyMs = averageTaskLatencyMs; }
        
        public long getToolCallCount() { return toolCallCount; }
        public void setToolCallCount(long toolCallCount) { this.toolCallCount = toolCallCount; }
        
        public long getToolCallSuccessCount() { return toolCallSuccessCount; }
        public void setToolCallSuccessCount(long toolCallSuccessCount) { this.toolCallSuccessCount = toolCallSuccessCount; }
        
        public long getToolCallFailureCount() { return toolCallFailureCount; }
        public void setToolCallFailureCount(long toolCallFailureCount) { this.toolCallFailureCount = toolCallFailureCount; }
        
        public double getToolCallSuccessRate() { return toolCallSuccessRate; }
        public void setToolCallSuccessRate(double toolCallSuccessRate) { this.toolCallSuccessRate = toolCallSuccessRate; }
        
        public double getAverageToolCallLatencyMs() { return averageToolCallLatencyMs; }
        public void setAverageToolCallLatencyMs(double averageToolCallLatencyMs) { this.averageToolCallLatencyMs = averageToolCallLatencyMs; }
        
        public Map<String, ToolStats> getToolStats() { return toolStats; }
        public void setToolStats(Map<String, ToolStats> toolStats) { this.toolStats = toolStats; }
    }
    
    /**
     * 工具统计类
     */
    public static class ToolStats {
        private final String toolName;
        private final AtomicLong callCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong totalLatencyMs = new AtomicLong(0);
        
        public ToolStats(String toolName) {
            this.toolName = toolName;
        }
        
        public void recordCall(long latencyMs, boolean success) {
            callCount.incrementAndGet();
            totalLatencyMs.addAndGet(latencyMs);
            if (success) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
            }
        }
        
        public String getToolName() { return toolName; }
        public long getCallCount() { return callCount.get(); }
        public long getSuccessCount() { return successCount.get(); }
        public long getFailureCount() { return failureCount.get(); }
        public double getSuccessRate() {
            long total = callCount.get();
            return total == 0 ? 0.0 : (double) successCount.get() / total * 100.0;
        }
        public double getAverageLatencyMs() {
            long total = callCount.get();
            return total == 0 ? 0.0 : (double) totalLatencyMs.get() / total;
        }
    }
    
    /**
     * 任务执行记录类
     */
    public static class TaskExecutionRecord {
        private String planId;
        private int stepCount;
        private int completedSteps;
        private long latencyMs;
        private boolean success;
        private LocalDateTime timestamp;
        
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        
        public int getStepCount() { return stepCount; }
        public void setStepCount(int stepCount) { this.stepCount = stepCount; }
        
        public int getCompletedSteps() { return completedSteps; }
        public void setCompletedSteps(int completedSteps) { this.completedSteps = completedSteps; }
        
        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
