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

/**
 * 任务步骤
 * Task Step
 * 
 * 定义Agent任务执行计划中的单个步骤。
 * 每个步骤包含：
 * - 执行顺序
 * - 要调用的工具
 * - 工具参数
 * - 依赖关系
 * - 执行状态
 * - 执行结果
 * 
 * Requirements:
 * - 4.1: Agent framework decomposes complex user requests into subtasks
 * - 4.2: Agent framework maintains task execution plan with dependencies
 * - 4.3: Agent framework selects and calls appropriate tools to complete subtasks
 * - 4.4: When tool execution fails, agent framework retries or uses alternative
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStep {
    
    /**
     * 步骤ID
     * Step ID
     * 
     * 唯一标识此步骤
     */
    private String stepId;
    
    /**
     * 执行顺序
     * Execution order
     * 
     * 从1开始的步骤序号，用于确定执行顺序
     * 
     * Requirement 4.2: Agent framework maintains task execution plan with dependencies
     */
    private int order;
    
    /**
     * 步骤名称
     * Step name
     * 
     * 人类可读的步骤描述
     */
    private String name;
    
    /**
     * 步骤描述
     * Step description
     * 
     * 详细说明此步骤要完成的任务
     */
    private String description;
    
    /**
     * 工具名称
     * Tool name
     * 
     * 要调用的Agent工具名称
     * 
     * Requirement 4.3: Agent framework selects and calls appropriate tools
     */
    private String toolName;
    
    /**
     * 工具参数
     * Tool parameters
     * 
     * 传递给工具的参数
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();
    
    /**
     * 依赖的步骤序号列表
     * List of dependent step orders
     * 
     * 此步骤依赖的其他步骤的序号
     * 只有当所有依赖步骤完成后，此步骤才能执行
     * 
     * Requirement 4.2: Agent framework maintains task execution plan with dependencies
     */
    @Builder.Default
    private List<Integer> dependencies = new ArrayList<>();
    
    /**
     * 执行状态
     * Execution status
     * 
     * 当前步骤的执行状态
     */
    @Builder.Default
    private TaskStepStatus status = TaskStepStatus.PENDING;
    
    /**
     * 执行结果
     * Execution result
     * 
     * 工具执行后的结果
     */
    private ToolResult result;
    
    /**
     * 开始执行时间
     * Start execution time
     */
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     * Completion time
     */
    private LocalDateTime completedAt;
    
    /**
     * 重试次数
     * Retry count
     * 
     * Requirement 4.4: When tool execution fails, agent framework retries
     */
    @Builder.Default
    private int retryCount = 0;
    
    /**
     * 最大重试次数
     * Maximum retry count
     */
    @Builder.Default
    private int maxRetries = 3;
    
    /**
     * 错误信息
     * Error message
     * 
     * 执行失败时的错误描述
     */
    private String errorMessage;
    
    /**
     * 是否需要用户确认
     * Whether requires user confirmation
     * 
     * Requirement 8.6: When tool needs user confirmation, agent framework pauses execution
     */
    @Builder.Default
    private boolean requiresConfirmation = false;
    
    /**
     * 确认消息
     * Confirmation message
     * 
     * 需要用户确认时显示的消息
     */
    private String confirmationMessage;
    
    /**
     * 用户确认结果
     * User confirmation result
     * 
     * true表示用户确认，false表示用户拒绝，null表示尚未确认
     */
    private Boolean userConfirmed;
    
    /**
     * 是否可选步骤
     * Whether optional step
     * 
     * 可选步骤失败不会导致整个计划失败
     */
    @Builder.Default
    private boolean optional = false;
    
    /**
     * 超时时间（毫秒）
     * Timeout in milliseconds
     * 
     * Requirement 4.6: Agent framework supports task execution timeout control
     */
    @Builder.Default
    private long timeoutMs = 30000;
    
    /**
     * 元数据
     * Metadata
     * 
     * 额外的步骤信息
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 检查是否可以执行
     * Check if can execute
     * 
     * @return true如果可以执行 (true if can execute)
     */
    public boolean canExecute() {
        return status.canExecute();
    }
    
    /**
     * 检查是否已完成
     * Check if completed
     * 
     * @return true如果已完成 (true if completed)
     */
    public boolean isCompleted() {
        return status == TaskStepStatus.COMPLETED;
    }
    
    /**
     * 检查是否失败
     * Check if failed
     * 
     * @return true如果失败 (true if failed)
     */
    public boolean isFailed() {
        return status == TaskStepStatus.FAILED;
    }
    
    /**
     * 检查是否为终态
     * Check if is terminal state
     * 
     * @return true如果是终态 (true if terminal state)
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }
    
    /**
     * 检查是否可以重试
     * Check if can retry
     * 
     * Requirement 4.4: When tool execution fails, agent framework retries
     * 
     * @return true如果可以重试 (true if can retry)
     */
    public boolean canRetry() {
        return status == TaskStepStatus.FAILED && retryCount < maxRetries;
    }
    
    /**
     * 增加重试计数
     * Increment retry count
     * 
     * @return 新的重试计数 (new retry count)
     */
    public int incrementRetry() {
        return ++retryCount;
    }
    
    /**
     * 标记为开始执行
     * Mark as started
     */
    public void markStarted() {
        this.status = TaskStepStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为完成
     * Mark as completed
     * 
     * @param result 执行结果 (execution result)
     */
    public void markCompleted(ToolResult result) {
        this.status = TaskStepStatus.COMPLETED;
        this.result = result;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为失败
     * Mark as failed
     * 
     * @param errorMessage 错误信息 (error message)
     */
    public void markFailed(String errorMessage) {
        this.status = TaskStepStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为失败（带结果）
     * Mark as failed with result
     * 
     * @param result 执行结果 (execution result)
     */
    public void markFailed(ToolResult result) {
        this.status = TaskStepStatus.FAILED;
        this.result = result;
        this.errorMessage = result != null ? result.getErrorMessage() : "Unknown error";
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为跳过
     * Mark as skipped
     * 
     * @param reason 跳过原因 (skip reason)
     */
    public void markSkipped(String reason) {
        this.status = TaskStepStatus.SKIPPED;
        this.errorMessage = reason;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为等待确认
     * Mark as waiting confirmation
     * 
     * @param confirmationMessage 确认消息 (confirmation message)
     */
    public void markWaitingConfirmation(String confirmationMessage) {
        this.status = TaskStepStatus.WAITING_CONFIRMATION;
        this.confirmationMessage = confirmationMessage;
    }
    
    /**
     * 标记为已取消
     * Mark as cancelled
     * 
     * @param reason 取消原因 (cancel reason)
     */
    public void markCancelled(String reason) {
        this.status = TaskStepStatus.CANCELLED;
        this.errorMessage = reason;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 重置为待执行状态（用于重试）
     * Reset to pending status (for retry)
     */
    public void resetForRetry() {
        this.status = TaskStepStatus.PENDING;
        this.result = null;
        this.startedAt = null;
        this.completedAt = null;
        this.errorMessage = null;
        incrementRetry();
    }
    
    /**
     * 获取执行时长（毫秒）
     * Get execution duration in milliseconds
     * 
     * @return 执行时长，如果尚未完成则返回-1 (execution duration, or -1 if not completed)
     */
    public long getExecutionDurationMs() {
        if (startedAt == null) {
            return -1;
        }
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).toMillis();
    }
    
    /**
     * 检查是否超时
     * Check if timeout
     * 
     * @return true如果已超时 (true if timeout)
     */
    public boolean isTimeout() {
        if (startedAt == null) {
            return false;
        }
        return getExecutionDurationMs() > timeoutMs;
    }
    
    /**
     * 添加依赖
     * Add dependency
     * 
     * @param stepOrder 依赖的步骤序号 (dependent step order)
     */
    public void addDependency(int stepOrder) {
        if (dependencies == null) {
            dependencies = new ArrayList<>();
        }
        if (!dependencies.contains(stepOrder)) {
            dependencies.add(stepOrder);
        }
    }
    
    /**
     * 检查是否依赖指定步骤
     * Check if depends on specified step
     * 
     * @param stepOrder 步骤序号 (step order)
     * @return true如果依赖 (true if depends)
     */
    public boolean dependsOn(int stepOrder) {
        return dependencies != null && dependencies.contains(stepOrder);
    }
    
    /**
     * 检查是否有依赖
     * Check if has dependencies
     * 
     * @return true如果有依赖 (true if has dependencies)
     */
    public boolean hasDependencies() {
        return dependencies != null && !dependencies.isEmpty();
    }
    
    /**
     * 添加参数
     * Add parameter
     * 
     * @param key 参数键 (parameter key)
     * @param value 参数值 (parameter value)
     * @return this for chaining
     */
    public TaskStep addParameter(String key, Object value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
        return this;
    }
    
    /**
     * 获取参数
     * Get parameter
     * 
     * @param key 参数键 (parameter key)
     * @return 参数值 (parameter value)
     */
    public Object getParameter(String key) {
        return parameters != null ? parameters.get(key) : null;
    }
    
    /**
     * 添加元数据
     * Add metadata
     * 
     * @param key 元数据键 (metadata key)
     * @param value 元数据值 (metadata value)
     * @return this for chaining
     */
    public TaskStep addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
        return this;
    }
    
    /**
     * 创建简单步骤
     * Create simple step
     * 
     * @param order 执行顺序 (execution order)
     * @param toolName 工具名称 (tool name)
     * @param parameters 参数 (parameters)
     * @return 任务步骤 (task step)
     */
    public static TaskStep create(int order, String toolName, Map<String, Object> parameters) {
        return TaskStep.builder()
                .stepId("step-" + order + "-" + System.currentTimeMillis())
                .order(order)
                .toolName(toolName)
                .parameters(parameters != null ? parameters : new HashMap<>())
                .build();
    }
    
    /**
     * 创建带名称的步骤
     * Create step with name
     * 
     * @param order 执行顺序 (execution order)
     * @param name 步骤名称 (step name)
     * @param toolName 工具名称 (tool name)
     * @param parameters 参数 (parameters)
     * @return 任务步骤 (task step)
     */
    public static TaskStep create(int order, String name, String toolName, 
                                  Map<String, Object> parameters) {
        return TaskStep.builder()
                .stepId("step-" + order + "-" + System.currentTimeMillis())
                .order(order)
                .name(name)
                .toolName(toolName)
                .parameters(parameters != null ? parameters : new HashMap<>())
                .build();
    }
    
    /**
     * 创建带依赖的步骤
     * Create step with dependencies
     * 
     * @param order 执行顺序 (execution order)
     * @param name 步骤名称 (step name)
     * @param toolName 工具名称 (tool name)
     * @param parameters 参数 (parameters)
     * @param dependencies 依赖的步骤序号 (dependent step orders)
     * @return 任务步骤 (task step)
     */
    public static TaskStep createWithDependencies(int order, String name, String toolName,
                                                   Map<String, Object> parameters,
                                                   List<Integer> dependencies) {
        return TaskStep.builder()
                .stepId("step-" + order + "-" + System.currentTimeMillis())
                .order(order)
                .name(name)
                .toolName(toolName)
                .parameters(parameters != null ? parameters : new HashMap<>())
                .dependencies(dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>())
                .build();
    }
    
    @Override
    public String toString() {
        return String.format("TaskStep{order=%d, name='%s', tool='%s', status=%s, deps=%s}",
                order, name, toolName, status, dependencies);
    }
}
