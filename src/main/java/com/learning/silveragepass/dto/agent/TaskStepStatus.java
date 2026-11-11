package com.learning.silveragepass.dto.agent;

/**
 * 任务步骤状态枚举
 * Task Step Status Enum
 * 
 * 定义Agent任务执行计划中每个步骤的状态。
 * 
 * 状态流转：
 * PENDING -> RUNNING -> COMPLETED/FAILED/SKIPPED
 * 
 * Requirements:
 * - 4.2: Agent framework maintains task execution plan with dependencies
 * - 4.4: When tool execution fails, agent framework retries or uses alternative
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public enum TaskStepStatus {
    
    /**
     * 待执行
     * Pending execution
     * 
     * 步骤已创建但尚未开始执行
     */
    PENDING("待执行", "pending"),
    
    /**
     * 执行中
     * Running
     * 
     * 步骤正在执行
     */
    RUNNING("执行中", "running"),
    
    /**
     * 已完成
     * Completed
     * 
     * 步骤执行成功完成
     */
    COMPLETED("已完成", "completed"),
    
    /**
     * 执行失败
     * Failed
     * 
     * 步骤执行失败
     * Requirement 4.4: When tool execution fails, agent framework retries or uses alternative
     */
    FAILED("执行失败", "failed"),
    
    /**
     * 已跳过
     * Skipped
     * 
     * 步骤被跳过（可能因为依赖失败或条件不满足）
     */
    SKIPPED("已跳过", "skipped"),
    
    /**
     * 等待确认
     * Waiting for confirmation
     * 
     * 步骤需要用户确认才能继续
     * Requirement 8.6: When tool needs user confirmation, agent framework pauses execution
     */
    WAITING_CONFIRMATION("等待确认", "waiting_confirmation"),
    
    /**
     * 已取消
     * Cancelled
     * 
     * 步骤被取消（可能因为超时或用户取消）
     */
    CANCELLED("已取消", "cancelled");
    
    /**
     * 中文描述
     * Chinese description
     */
    private final String description;
    
    /**
     * 英文代码
     * English code
     */
    private final String code;
    
    TaskStepStatus(String description, String code) {
        this.description = description;
        this.code = code;
    }
    
    /**
     * 获取中文描述
     * Get Chinese description
     * 
     * @return 中文描述 (Chinese description)
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 获取英文代码
     * Get English code
     * 
     * @return 英文代码 (English code)
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 检查是否为终态
     * Check if is terminal state
     * 
     * 终态表示步骤已经结束，不会再有状态变化
     * 
     * @return true如果是终态 (true if terminal state)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == SKIPPED || this == CANCELLED;
    }
    
    /**
     * 检查是否为成功状态
     * Check if is success state
     * 
     * @return true如果是成功状态 (true if success state)
     */
    public boolean isSuccess() {
        return this == COMPLETED;
    }
    
    /**
     * 检查是否为失败状态
     * Check if is failure state
     * 
     * @return true如果是失败状态 (true if failure state)
     */
    public boolean isFailure() {
        return this == FAILED || this == CANCELLED;
    }
    
    /**
     * 检查是否可以执行
     * Check if can execute
     * 
     * @return true如果可以执行 (true if can execute)
     */
    public boolean canExecute() {
        return this == PENDING;
    }
    
    /**
     * 检查是否正在执行
     * Check if is executing
     * 
     * @return true如果正在执行 (true if executing)
     */
    public boolean isExecuting() {
        return this == RUNNING || this == WAITING_CONFIRMATION;
    }
    
    /**
     * 根据代码获取状态
     * Get status by code
     * 
     * @param code 英文代码 (English code)
     * @return 状态枚举，如果未找到则返回null (status enum, or null if not found)
     */
    public static TaskStepStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TaskStepStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return String.format("%s(%s)", name(), description);
    }
}
