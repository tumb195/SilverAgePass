package com.learning.silveragepass.service.agent;

import com.learning.silveragepass.dto.agent.AgentAnalysisResult;
import com.learning.silveragepass.dto.agent.AgentExecutionResult;
import com.learning.silveragepass.dto.agent.TaskPlan;
import com.learning.silveragepass.dto.rag.ConversationContext;

/**
 * Agent服务接口
 * Agent Service Interface
 * 
 * 负责任务规划、工具选择和执行。提供：
 * - 用户请求分析
 * - 任务规划
 * - 任务执行
 * - 服务状态检查
 * 
 * Requirements:
 * - 4.1: Agent framework decomposes complex user requests into subtasks
 * - 4.2: Agent framework maintains task execution plan with dependencies
 * - 4.3: Agent framework selects and calls appropriate tools to complete subtasks
 * - 4.4: When tool execution fails, agent framework retries or uses alternative
 * - 4.5: When all subtasks complete, agent framework synthesizes results into coherent response
 * - 4.6: Agent framework supports task execution timeout control (default 30s)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
public interface AgentService {
    
    /**
     * 分析用户请求，判断是否需要工具调用
     * Analyze user request to determine if tool call is needed
     * 
     * 分析用户消息，识别意图，判断是否需要调用工具来完成请求。
     * 如果需要工具调用，返回推荐的工具列表。
     * 
     * Requirement 4.1: Agent framework decomposes complex user requests into subtasks
     * Requirement 4.3: Agent framework selects and calls appropriate tools
     * 
     * @param userMessage 用户消息 (user message)
     * @param context 对话上下文 (conversation context)
     * @return 分析结果 (analysis result)
     */
    AgentAnalysisResult analyze(String userMessage, ConversationContext context);
    
    /**
     * 规划任务执行计划
     * Plan task execution
     * 
     * 根据用户消息和上下文，生成任务执行计划。
     * 计划包含多个步骤，每个步骤指定要调用的工具和参数。
     * 步骤之间可以有依赖关系。
     * 
     * Requirement 4.1: Agent framework decomposes complex user requests into subtasks
     * Requirement 4.2: Agent framework maintains task execution plan with dependencies
     * 
     * @param userMessage 用户消息 (user message)
     * @param context 对话上下文 (conversation context)
     * @return 任务执行计划 (task execution plan)
     */
    TaskPlan planTasks(String userMessage, ConversationContext context);
    
    /**
     * 执行任务计划
     * Execute task plan
     * 
     * 按照任务计划中的步骤顺序执行工具调用。
     * 支持：
     * - 依赖顺序执行
     * - 并行执行独立步骤
     * - 超时控制
     * - 失败重试
     * - 结果综合
     * 
     * Requirement 4.3: Agent framework selects and calls appropriate tools
     * Requirement 4.4: When tool execution fails, agent framework retries or uses alternative
     * Requirement 4.5: When all subtasks complete, agent framework synthesizes results
     * Requirement 4.6: Agent framework supports task execution timeout control
     * 
     * @param plan 任务计划 (task plan)
     * @param context 对话上下文 (conversation context)
     * @return 执行结果 (execution result)
     */
    AgentExecutionResult execute(TaskPlan plan, ConversationContext context);
    
    /**
     * 分析并执行（便捷方法）
     * Analyze and execute (convenience method)
     * 
     * 组合analyze、planTasks和execute方法，一步完成分析和执行。
     * 
     * @param userMessage 用户消息 (user message)
     * @param context 对话上下文 (conversation context)
     * @return 执行结果 (execution result)
     */
    default AgentExecutionResult analyzeAndExecute(String userMessage, ConversationContext context) {
        // 1. 分析请求
        AgentAnalysisResult analysis = analyze(userMessage, context);
        
        // 2. 如果不需要工具调用，返回无需执行的结果
        if (!analysis.isRequiresToolCall()) {
            return AgentExecutionResult.noActionNeeded("不需要执行工具调用");
        }
        
        // 3. 规划任务
        TaskPlan plan = planTasks(userMessage, context);
        
        // 4. 执行任务
        return execute(plan, context);
    }
    
    /**
     * 检查Agent服务是否可用
     * Check if Agent service is available
     * 
     * 检查Agent服务的健康状态，包括：
     * - 工具注册表是否可用
     * - LLM服务是否可用
     * - 配置是否正确
     * 
     * @return true如果服务可用 (true if service is available)
     */
    boolean isAvailable();
    
    /**
     * 获取可用工具数量
     * Get available tool count
     * 
     * @return 可用工具数量 (available tool count)
     */
    int getAvailableToolCount();
    
    /**
     * 取消正在执行的任务
     * Cancel executing task
     * 
     * @param planId 任务计划ID (task plan ID)
     * @return true如果成功取消 (true if successfully cancelled)
     */
    boolean cancelTask(String planId);
    
    /**
     * 确认待确认的任务
     * Confirm pending task
     * 
     * 当任务需要用户确认时，调用此方法继续执行。
     * 
     * @param planId 任务计划ID (task plan ID)
     * @param confirmed 是否确认 (whether confirmed)
     * @param context 对话上下文 (conversation context)
     * @return 执行结果 (execution result)
     */
    AgentExecutionResult confirmTask(String planId, boolean confirmed, ConversationContext context);
}
