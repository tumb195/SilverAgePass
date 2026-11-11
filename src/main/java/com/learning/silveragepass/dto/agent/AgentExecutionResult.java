package com.learning.silveragepass.dto.agent;

import com.learning.silveragepass.service.agent.ToolResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent执行结果
 * Agent Execution Result
 * 
 * 封装Agent任务计划执行的完整结果，包括：
 * - 执行状态
 * - 各步骤执行结果
 * - 综合结果摘要
 * - 执行统计信息
 * 
 * Requirements:
 * - 4.4: When tool execution fails, agent framework retries or uses alternative
 * - 4.5: When all subtasks complete, agent framework synthesizes results into coherent response
 * - 4.6: Agent framework supports task execution timeout control (default 30s)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionResult {
    
    /**
     * 执行是否成功
     * Whether execution was successful
     */
    private boolean success;
    
    /**
     * 执行状态
     * Execution status
     */
    private ExecutionStatus status;
    
    /**
     * 执行的任务计划
     * Executed task plan
     */
    private TaskPlan taskPlan;
    
    /**
     * 各步骤执行结果
     * Step execution results
     */
    @Builder.Default
    private List<StepResult> stepResults = new ArrayList<>();
    
    /**
     * 综合结果摘要
     * Synthesized result summary
     * 
     * Requirement 4.5: Agent framework synthesizes results into coherent response
     */
    private String resultSummary;
    
    /**
     * 综合结果数据
     * Synthesized result data
     * 
     * 所有步骤结果的综合数据
     */
    private Object synthesizedData;
    
    /**
     * 错误信息
     * Error message
     */
    private String errorMessage;
    
    /**
     * 错误代码
     * Error code
     */
    private String errorCode;
    
    /**
     * 执行开始时间
     * Execution start time
     */
    private LocalDateTime startedAt;
    
    /**
     * 执行结束时间
     * Execution end time
     */
    private LocalDateTime completedAt;
    
    /**
     * 总执行耗时（毫秒）
     * Total execution time in milliseconds
     */
    private long totalExecutionTimeMs;
    
    /**
     * 是否超时
     * Whether timeout occurred
     * 
     * Requirement 4.6: Agent framework supports task execution timeout control
     */
    private boolean timeout;
    
    /**
     * 重试次数
     * Retry count
     * 
     * Requirement 4.4: When tool execution fails, agent framework retries
     */
    @Builder.Default
    private int totalRetries = 0;
    
    /**
     * 是否需要用户确认
     * Whether user confirmation is required
     */
    private boolean requiresConfirmation;
    
    /**
     * 确认消息
     * Confirmation message
     */
    private String confirmationMessage;
    
    /**
     * 待确认的数据
     * Pending confirmation data
     */
    private Object pendingConfirmationData;
    
    /**
     * 元数据
     * Metadata
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 执行状态枚举
     * Execution Status Enum
     */
    public enum ExecutionStatus {
        /**
         * 执行成功
         * Execution successful
         */
        SUCCESS("成功", "success"),
        
        /**
         * 部分成功
         * Partially successful
         */
        PARTIAL_SUCCESS("部分成功", "partial_success"),
        
        /**
         * 执行失败
         * Execution failed
         */
        FAILED("失败", "failed"),
        
        /**
         * 执行超时
         * Execution timeout
         */
        TIMEOUT("超时", "timeout"),
        
        /**
         * 等待确认
         * Waiting for confirmation
         */
        WAITING_CONFIRMATION("等待确认", "waiting_confirmation"),
        
        /**
         * 已取消
         * Cancelled
         */
        CANCELLED("已取消", "cancelled"),
        
        /**
         * 无需执行
         * No execution needed
         */
        NO_ACTION_NEEDED("无需执行", "no_action_needed");
        
        private final String description;
        private final String code;
        
        ExecutionStatus(String description, String code) {
            this.description = description;
            this.code = code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getCode() {
            return code;
        }
        
        public boolean isTerminal() {
            return this == SUCCESS || this == PARTIAL_SUCCESS || 
                   this == FAILED || this == TIMEOUT || this == CANCELLED;
        }
    }
    
    /**
     * 步骤执行结果
     * Step Execution Result
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepResult {
        /**
         * 步骤序号
         * Step order
         */
        private int stepOrder;
        
        /**
         * 步骤名称
         * Step name
         */
        private String stepName;
        
        /**
         * 工具名称
         * Tool name
         */
        private String toolName;
        
        /**
         * 执行是否成功
         * Whether execution was successful
         */
        private boolean success;
        
        /**
         * 工具执行结果
         * Tool execution result
         */
        private ToolResult toolResult;
        
        /**
         * 执行耗时（毫秒）
         * Execution time in milliseconds
         */
        private long executionTimeMs;
        
        /**
         * 重试次数
         * Retry count
         */
        private int retryCount;
        
        /**
         * 错误信息
         * Error message
         */
        private String errorMessage;
        
        /**
         * 是否被跳过
         * Whether skipped
         */
        private boolean skipped;
        
        /**
         * 跳过原因
         * Skip reason
         */
        private String skipReason;
        
        /**
         * 创建成功的步骤结果
         * Create successful step result
         */
        public static StepResult success(int order, String name, String toolName, 
                                         ToolResult result, long timeMs) {
            return StepResult.builder()
                    .stepOrder(order)
                    .stepName(name)
                    .toolName(toolName)
                    .success(true)
                    .toolResult(result)
                    .executionTimeMs(timeMs)
                    .build();
        }
        
        /**
         * 创建失败的步骤结果
         * Create failed step result
         */
        public static StepResult failure(int order, String name, String toolName, 
                                         String errorMessage, long timeMs) {
            return StepResult.builder()
                    .stepOrder(order)
                    .stepName(name)
                    .toolName(toolName)
                    .success(false)
                    .errorMessage(errorMessage)
                    .executionTimeMs(timeMs)
                    .build();
        }
        
        /**
         * 创建跳过的步骤结果
         * Create skipped step result
         */
        public static StepResult skipped(int order, String name, String toolName, String reason) {
            return StepResult.builder()
                    .stepOrder(order)
                    .stepName(name)
                    .toolName(toolName)
                    .success(false)
                    .skipped(true)
                    .skipReason(reason)
                    .build();
        }
    }
    
    // ==================== 工厂方法 ====================
    
    /**
     * 创建成功的执行结果
     * Create successful execution result
     * 
     * @param taskPlan 任务计划 (task plan)
     * @param stepResults 步骤结果 (step results)
     * @param summary 结果摘要 (result summary)
     * @return 执行结果 (execution result)
     */
    public static AgentExecutionResult success(TaskPlan taskPlan, List<StepResult> stepResults, 
                                               String summary) {
        return AgentExecutionResult.builder()
                .success(true)
                .status(ExecutionStatus.SUCCESS)
                .taskPlan(taskPlan)
                .stepResults(stepResults != null ? stepResults : new ArrayList<>())
                .resultSummary(summary)
                .completedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建部分成功的执行结果
     * Create partially successful execution result
     * 
     * @param taskPlan 任务计划 (task plan)
     * @param stepResults 步骤结果 (step results)
     * @param summary 结果摘要 (result summary)
     * @return 执行结果 (execution result)
     */
    public static AgentExecutionResult partialSuccess(TaskPlan taskPlan, List<StepResult> stepResults,
                                                      String summary) {
        return AgentExecutionResult.builder()
                .success(true)
                .status(ExecutionStatus.PARTIAL_SUCCESS)
                .taskPlan(taskPlan)
                .stepResults(stepResults != null ? stepResults : new ArrayList<>())
                .resultSummary(summary)
                .completedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建失败的执行结果
     * Create failed execution result
     * 
     * @param taskPlan 任务计划 (task plan)
     * @param errorCode 错误代码 (error code)
     * @param errorMessage 错误信息 (error message)
     * @return 执行结果 (execution result)
     */
    public static AgentExecutionResult failure(TaskPlan taskPlan, String errorCode, String errorMessage) {
        return AgentExecutionResult.builder()
                .success(false)
                .status(ExecutionStatus.FAILED)
                .taskPlan(taskPlan)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .completedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建超时的执行结果
     * Create timeout execution result
     * 
     * @param taskPlan 任务计划 (task plan)
     * @param stepResults 已完成的步骤结果 (completed step results)
     * @return 执行结果 (execution result)
     */
    public static AgentExecutionResult timeout(TaskPlan taskPlan, List<StepResult> stepResults) {
        return AgentExecutionResult.builder()
                .success(false)
                .status(ExecutionStatus.TIMEOUT)
                .taskPlan(taskPlan)
                .stepResults(stepResults != null ? stepResults : new ArrayList<>())
                .timeout(true)
                .errorCode("TIMEOUT")
                .errorMessage("任务执行超时")
                .completedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建需要确认的执行结果
     * Create execution result requiring confirmation
     * 
     * @param taskPlan 任务计划 (task plan)
     * @param confirmationMessage 确认消息 (confirmation message)
     * @param pendingData 待确认数据 (pending data)
     * @return 执行结果 (execution result)
     */
    public static AgentExecutionResult requireConfirmation(TaskPlan taskPlan, 
                                                           String confirmationMessage,
                                                           Object pendingData) {
        return AgentExecutionResult.builder()
                .success(true)
                .status(ExecutionStatus.WAITING_CONFIRMATION)
                .taskPlan(taskPlan)
                .requiresConfirmation(true)
                .confirmationMessage(confirmationMessage)
                .pendingConfirmationData(pendingData)
                .build();
    }
    
    /**
     * 创建无需执行的结果
     * Create no action needed result
     * 
     * @param reason 原因 (reason)
     * @return 执行结果 (execution result)
     */
    public static AgentExecutionResult noActionNeeded(String reason) {
        return AgentExecutionResult.builder()
                .success(true)
                .status(ExecutionStatus.NO_ACTION_NEEDED)
                .resultSummary(reason)
                .completedAt(LocalDateTime.now())
                .build();
    }
    
    // ==================== 统计方法 ====================
    
    /**
     * 获取成功的步骤数
     * Get successful step count
     * 
     * @return 成功步骤数 (successful step count)
     */
    public int getSuccessfulStepCount() {
        if (stepResults == null) {
            return 0;
        }
        return (int) stepResults.stream().filter(StepResult::isSuccess).count();
    }
    
    /**
     * 获取失败的步骤数
     * Get failed step count
     * 
     * @return 失败步骤数 (failed step count)
     */
    public int getFailedStepCount() {
        if (stepResults == null) {
            return 0;
        }
        return (int) stepResults.stream()
                .filter(r -> !r.isSuccess() && !r.isSkipped())
                .count();
    }
    
    /**
     * 获取跳过的步骤数
     * Get skipped step count
     * 
     * @return 跳过步骤数 (skipped step count)
     */
    public int getSkippedStepCount() {
        if (stepResults == null) {
            return 0;
        }
        return (int) stepResults.stream().filter(StepResult::isSkipped).count();
    }
    
    /**
     * 获取总步骤数
     * Get total step count
     * 
     * @return 总步骤数 (total step count)
     */
    public int getTotalStepCount() {
        return stepResults != null ? stepResults.size() : 0;
    }
    
    /**
     * 获取成功率
     * Get success rate
     * 
     * @return 成功率 (success rate)
     */
    public double getSuccessRate() {
        int total = getTotalStepCount();
        if (total == 0) {
            return 1.0;
        }
        return (double) getSuccessfulStepCount() / total;
    }
    
    /**
     * 获取所有成功步骤的结果数据
     * Get all successful step result data
     * 
     * @return 结果数据列表 (list of result data)
     */
    public List<Object> getSuccessfulResultData() {
        if (stepResults == null) {
            return new ArrayList<>();
        }
        return stepResults.stream()
                .filter(StepResult::isSuccess)
                .filter(r -> r.getToolResult() != null && r.getToolResult().getData() != null)
                .map(r -> r.getToolResult().getData())
                .collect(Collectors.toList());
    }
    
    /**
     * 添加步骤结果
     * Add step result
     * 
     * @param result 步骤结果 (step result)
     * @return this for chaining
     */
    public AgentExecutionResult addStepResult(StepResult result) {
        if (stepResults == null) {
            stepResults = new ArrayList<>();
        }
        stepResults.add(result);
        return this;
    }
    
    /**
     * 添加元数据
     * Add metadata
     * 
     * @param key 键 (key)
     * @param value 值 (value)
     * @return this for chaining
     */
    public AgentExecutionResult addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }
    
    /**
     * 计算总执行时间
     * Calculate total execution time
     */
    public void calculateTotalExecutionTime() {
        if (startedAt != null && completedAt != null) {
            totalExecutionTimeMs = java.time.Duration.between(startedAt, completedAt).toMillis();
        } else if (stepResults != null) {
            totalExecutionTimeMs = stepResults.stream()
                    .mapToLong(StepResult::getExecutionTimeMs)
                    .sum();
        }
    }
    
    /**
     * 生成用户友好的结果描述
     * Generate user-friendly result description
     * 
     * @return 用户友好的描述 (user-friendly description)
     */
    public String getUserFriendlyDescription() {
        if (resultSummary != null && !resultSummary.isEmpty()) {
            return resultSummary;
        }
        
        if (status == null) {
            return "操作已完成";
        }
        
        return switch (status) {
            case SUCCESS -> "所有任务已成功完成";
            case PARTIAL_SUCCESS -> String.format("部分任务已完成（%d/%d成功）", 
                    getSuccessfulStepCount(), getTotalStepCount());
            case FAILED -> errorMessage != null ? errorMessage : "任务执行失败";
            case TIMEOUT -> "任务执行超时，请稍后重试";
            case WAITING_CONFIRMATION -> confirmationMessage != null ? 
                    confirmationMessage : "需要您的确认才能继续";
            case CANCELLED -> "任务已取消";
            case NO_ACTION_NEEDED -> "无需执行任何操作";
        };
    }
    
    @Override
    public String toString() {
        return String.format("AgentExecutionResult{success=%s, status=%s, steps=%d/%d, time=%dms}",
                success, status, getSuccessfulStepCount(), getTotalStepCount(), totalExecutionTimeMs);
    }
}
