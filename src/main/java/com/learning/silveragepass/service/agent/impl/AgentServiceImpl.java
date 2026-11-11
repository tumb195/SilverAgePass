package com.learning.silveragepass.service.agent.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.config.RAGAgentProperties;
import com.learning.silveragepass.dto.agent.*;
import com.learning.silveragepass.dto.rag.ConversationContext;
import com.learning.silveragepass.service.agent.*;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Agent服务实现
 * Agent Service Implementation
 * 
 * 实现Agent框架的核心功能：
 * - 用户请求分析
 * - 任务规划（使用LLM）
 * - 任务执行（支持依赖、并行、超时、重试）
 * - 结果综合
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
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {
    
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final RAGAgentProperties properties;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    
    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String chatModel;
    
    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double chatTemperature;
    
    @Value("${spring.ai.openai.chat.options.max-tokens:2000}")
    private Integer chatMaxTokens;
    
    /**
     * 构造函数
     * Constructor
     */
    public AgentServiceImpl(ToolRegistry toolRegistry, ToolExecutor toolExecutor,
                           RAGAgentProperties properties, OpenAiService openAiService,
                           ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.properties = properties;
        this.openAiService = openAiService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 执行器线程池
     * Executor thread pool for parallel execution
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    /**
     * 待确认的任务计划缓存
     * Cache for pending confirmation task plans
     */
    private final Map<String, TaskPlan> pendingConfirmationPlans = new ConcurrentHashMap<>();
    
    /**
     * 正在执行的任务计划
     * Currently executing task plans
     */
    private final Map<String, TaskPlan> executingPlans = new ConcurrentHashMap<>();
    
    // ==================== 分析方法 ====================
    
    @Override
    public AgentAnalysisResult analyze(String userMessage, ConversationContext context) {
        long startTime = System.currentTimeMillis();
        log.info("Analyzing user message: {}", userMessage);
        
        try {
            // 检查服务是否可用
            if (!isAvailable()) {
                log.warn("Agent service is not available");
                return AgentAnalysisResult.noToolsRequired("服务暂时不可用");
            }
            
            // 获取可用工具描述
            List<ToolDescription> toolDescriptions = toolRegistry.getToolDescriptions();
            if (toolDescriptions.isEmpty()) {
                log.info("No tools available, returning no tools required");
                return AgentAnalysisResult.noToolsRequired("普通对话");
            }
            
            // 构建分析提示词
            String analysisPrompt = buildAnalysisPrompt(userMessage, context, toolDescriptions);
            
            // 调用LLM进行分析
            String response = callLLM(analysisPrompt);
            
            // 解析LLM响应
            AgentAnalysisResult result = parseAnalysisResponse(response, userMessage);
            result.setAnalysisTimeMs(System.currentTimeMillis() - startTime);
            result.setOriginalMessage(userMessage);
            
            log.info("Analysis completed: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("Error analyzing user message: {}", e.getMessage(), e);
            return AgentAnalysisResult.noToolsRequired("分析失败，使用普通对话");
        }
    }
    
    // ==================== 任务规划方法 ====================
    
    @Override
    public TaskPlan planTasks(String userMessage, ConversationContext context) {
        log.info("Planning tasks for message: {}", userMessage);
        
        try {
            // 检查服务是否可用
            if (!isAvailable()) {
                log.warn("Agent service is not available for planning");
                return TaskPlan.createEmpty("服务不可用");
            }
            
            // 获取可用工具描述
            List<ToolDescription> toolDescriptions = toolRegistry.getToolDescriptions();
            if (toolDescriptions.isEmpty()) {
                log.info("No tools available for planning");
                return TaskPlan.createEmpty("无可用工具");
            }
            
            // 构建规划提示词
            String planningPrompt = buildPlanningPrompt(userMessage, context, toolDescriptions);
            
            // 调用LLM进行规划
            String response = callLLM(planningPrompt);
            
            // 解析LLM响应为任务计划
            TaskPlan plan = parsePlanningResponse(response, userMessage, context);
            
            // 设置超时时间
            plan.setTimeoutMs(properties.getAgent().getTaskTimeoutMs());
            
            log.info("Task plan created: {}", plan);
            return plan;
            
        } catch (Exception e) {
            log.error("Error planning tasks: {}", e.getMessage(), e);
            TaskPlan errorPlan = TaskPlan.create("规划失败", userMessage);
            errorPlan.markFailed("任务规划失败: " + e.getMessage());
            return errorPlan;
        }
    }
    
    // ==================== 任务执行方法 ====================
    
    @Override
    public AgentExecutionResult execute(TaskPlan plan, ConversationContext context) {
        log.info("Executing task plan: {}", plan.getPlanId());
        
        LocalDateTime startTime = LocalDateTime.now();
        List<AgentExecutionResult.StepResult> stepResults = new ArrayList<>();
        int totalRetries = 0;
        
        try {
            // 检查计划是否有效
            if (plan == null || plan.getSteps() == null || plan.getSteps().isEmpty()) {
                log.info("Empty task plan, no execution needed");
                return AgentExecutionResult.noActionNeeded("无需执行任何任务");
            }
            
            // 标记计划开始执行
            plan.markStarted();
            executingPlans.put(plan.getPlanId(), plan);
            
            // 创建执行上下文
            ToolExecutionContext execContext = createExecutionContext(plan, context);
            
            // 按依赖顺序执行步骤
            while (!plan.isAllStepsTerminated()) {
                // 检查超时
                if (plan.isTimeout()) {
                    log.warn("Task plan {} timeout", plan.getPlanId());
                    return AgentExecutionResult.timeout(plan, stepResults);
                }
                
                // 获取可执行的步骤
                List<TaskStep> executableSteps = plan.getParallelExecutableSteps();
                if (executableSteps.isEmpty()) {
                    // 没有可执行的步骤，可能是依赖未满足或全部完成
                    if (!plan.isAllStepsTerminated()) {
                        log.warn("No executable steps but plan not terminated");
                        break;
                    }
                    continue;
                }
                
                // 并行执行独立步骤
                if (executableSteps.size() > 1 && properties.getAgent().getMaxParallelExecutions() > 1) {
                    List<AgentExecutionResult.StepResult> parallelResults = 
                            executeStepsInParallel(executableSteps, execContext, plan);
                    stepResults.addAll(parallelResults);
                    totalRetries += parallelResults.stream()
                            .mapToInt(AgentExecutionResult.StepResult::getRetryCount)
                            .sum();
                } else {
                    // 顺序执行
                    for (TaskStep step : executableSteps) {
                        AgentExecutionResult.StepResult result = executeStep(step, execContext, plan);
                        stepResults.add(result);
                        totalRetries += result.getRetryCount();
                        
                        // 检查是否需要用户确认
                        if (step.getStatus() == TaskStepStatus.WAITING_CONFIRMATION) {
                            pendingConfirmationPlans.put(plan.getPlanId(), plan);
                            return AgentExecutionResult.requireConfirmation(
                                    plan, 
                                    step.getConfirmationMessage(),
                                    step.getResult() != null ? step.getResult().getData() : null
                            );
                        }
                    }
                }
            }
            
            // 更新计划状态
            plan.updateStatusFromSteps();
            
            // 综合结果
            String summary = synthesizeResults(plan, stepResults, context);
            
            // 构建执行结果
            AgentExecutionResult result;
            if (plan.isAllStepsCompleted()) {
                result = AgentExecutionResult.success(plan, stepResults, summary);
            } else if (plan.getCompletedStepCount() > 0) {
                result = AgentExecutionResult.partialSuccess(plan, stepResults, summary);
            } else {
                result = AgentExecutionResult.failure(plan, "EXECUTION_FAILED", 
                        plan.getErrorMessage() != null ? plan.getErrorMessage() : "所有步骤执行失败");
            }
            
            result.setStartedAt(startTime);
            result.setCompletedAt(LocalDateTime.now());
            result.setTotalRetries(totalRetries);
            result.calculateTotalExecutionTime();
            
            log.info("Task plan execution completed: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("Error executing task plan: {}", e.getMessage(), e);
            return AgentExecutionResult.failure(plan, "EXECUTION_ERROR", 
                    "任务执行出错: " + e.getMessage());
        } finally {
            executingPlans.remove(plan.getPlanId());
        }
    }
    
    // ==================== 辅助方法 ====================
    
    @Override
    public boolean isAvailable() {
        // 检查Agent功能是否启用
        if (!properties.isAgentAvailable()) {
            return false;
        }
        
        // 检查工具注册表是否有可用工具
        return toolRegistry.getAvailableToolCount() > 0;
    }
    
    @Override
    public int getAvailableToolCount() {
        return toolRegistry.getAvailableToolCount();
    }
    
    @Override
    public boolean cancelTask(String planId) {
        TaskPlan plan = executingPlans.get(planId);
        if (plan != null) {
            plan.markCancelled("用户取消");
            executingPlans.remove(planId);
            log.info("Task plan {} cancelled", planId);
            return true;
        }
        
        plan = pendingConfirmationPlans.remove(planId);
        if (plan != null) {
            plan.markCancelled("用户取消");
            log.info("Pending confirmation plan {} cancelled", planId);
            return true;
        }
        
        return false;
    }
    
    @Override
    public AgentExecutionResult confirmTask(String planId, boolean confirmed, 
                                            ConversationContext context) {
        TaskPlan plan = pendingConfirmationPlans.remove(planId);
        if (plan == null) {
            return AgentExecutionResult.failure(null, "PLAN_NOT_FOUND", 
                    "未找到待确认的任务计划");
        }
        
        if (!confirmed) {
            plan.markCancelled("用户拒绝确认");
            return AgentExecutionResult.failure(plan, "USER_REJECTED", "用户拒绝执行");
        }
        
        // 找到等待确认的步骤并继续执行
        Optional<TaskStep> waitingStep = plan.getSteps().stream()
                .filter(s -> s.getStatus() == TaskStepStatus.WAITING_CONFIRMATION)
                .findFirst();
        
        if (waitingStep.isPresent()) {
            TaskStep step = waitingStep.get();
            step.setUserConfirmed(true);
            step.setStatus(TaskStepStatus.PENDING);
        }
        
        // 继续执行计划
        return execute(plan, context);
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(String userMessage, ConversationContext context,
                                       List<ToolDescription> tools) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能助手，需要分析用户的请求并判断是否需要调用工具来完成。\n\n");
        
        prompt.append("可用的工具列表：\n");
        for (ToolDescription tool : tools) {
            prompt.append(String.format("- %s: %s\n", tool.getName(), tool.getDescription()));
        }
        prompt.append("\n");
        
        if (context != null && context.hasHistory()) {
            prompt.append("对话历史：\n");
            prompt.append(context.generateSummary(5));
            prompt.append("\n\n");
        }
        
        prompt.append("用户消息：").append(userMessage).append("\n\n");
        
        prompt.append("请分析用户请求，返回JSON格式的结果：\n");
        prompt.append("{\n");
        prompt.append("  \"requiresToolCall\": true/false,\n");
        prompt.append("  \"intent\": \"用户意图描述\",\n");
        prompt.append("  \"intentType\": \"query/action/mixed/chat\",\n");
        prompt.append("  \"recommendedTools\": [\"工具名称列表\"],\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"explanation\": \"分析说明\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    /**
     * 解析分析响应
     */
    private AgentAnalysisResult parseAnalysisResponse(String response, String originalMessage) {
        try {
            // 提取JSON部分
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                return AgentAnalysisResult.noToolsRequired("无法解析分析结果");
            }
            
            Map<String, Object> parsed = objectMapper.readValue(jsonStr, 
                    new TypeReference<Map<String, Object>>() {});
            
            AgentAnalysisResult result = AgentAnalysisResult.builder()
                    .requiresToolCall(Boolean.TRUE.equals(parsed.get("requiresToolCall")))
                    .intent((String) parsed.get("intent"))
                    .explanation((String) parsed.get("explanation"))
                    .originalMessage(originalMessage)
                    .build();
            
            // 解析意图类型
            String intentTypeStr = (String) parsed.get("intentType");
            if (intentTypeStr != null) {
                try {
                    result.setIntentType(AgentAnalysisResult.IntentType.valueOf(
                            intentTypeStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    result.setIntentType(AgentAnalysisResult.IntentType.UNKNOWN);
                }
            }
            
            // 解析推荐工具
            Object toolsObj = parsed.get("recommendedTools");
            if (toolsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> tools = (List<String>) toolsObj;
                result.setRecommendedTools(tools);
            }
            
            // 解析置信度
            Object confidenceObj = parsed.get("confidence");
            if (confidenceObj instanceof Number) {
                result.setConfidence(((Number) confidenceObj).doubleValue());
            }
            
            return result;
            
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse analysis response: {}", e.getMessage());
            return AgentAnalysisResult.noToolsRequired("解析失败");
        }
    }
    
    /**
     * 构建规划提示词
     */
    private String buildPlanningPrompt(String userMessage, ConversationContext context,
                                       List<ToolDescription> tools) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个任务规划助手，需要将用户请求分解为具体的工具调用步骤。\n\n");
        
        prompt.append("可用的工具及其参数：\n");
        for (ToolDescription tool : tools) {
            prompt.append(String.format("\n### %s\n", tool.getName()));
            prompt.append(String.format("描述: %s\n", tool.getDescription()));
            if (tool.getParameterSchema() != null) {
                prompt.append("参数: ").append(tool.getParameterSchema().toJsonSchema()).append("\n");
            }
        }
        prompt.append("\n");
        
        if (context != null && context.getUserProfile() != null) {
            prompt.append("用户信息：\n");
            prompt.append(String.format("- 用户ID: %d\n", context.getUserId()));
            prompt.append("\n");
        }
        
        prompt.append("用户请求：").append(userMessage).append("\n\n");
        
        prompt.append("请生成任务执行计划，返回JSON格式：\n");
        prompt.append("{\n");
        prompt.append("  \"userIntent\": \"用户意图\",\n");
        prompt.append("  \"steps\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"order\": 1,\n");
        prompt.append("      \"name\": \"步骤名称\",\n");
        prompt.append("      \"toolName\": \"工具名称\",\n");
        prompt.append("      \"parameters\": {\"参数名\": \"参数值\"},\n");
        prompt.append("      \"dependencies\": [],\n");
        prompt.append("      \"description\": \"步骤描述\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("\n注意：\n");
        prompt.append("- 步骤顺序从1开始\n");
        prompt.append("- dependencies数组包含此步骤依赖的其他步骤序号\n");
        prompt.append("- 参数值应该从用户消息中提取或使用合理的默认值\n");
        
        return prompt.toString();
    }
    
    /**
     * 解析规划响应
     */
    private TaskPlan parsePlanningResponse(String response, String userMessage, 
                                           ConversationContext context) {
        try {
            // 提取JSON部分
            String jsonStr = extractJson(response);
            if (jsonStr == null) {
                return TaskPlan.createEmpty("无法解析规划结果");
            }
            
            Map<String, Object> parsed = objectMapper.readValue(jsonStr,
                    new TypeReference<Map<String, Object>>() {});
            
            String userIntent = (String) parsed.getOrDefault("userIntent", "执行用户请求");
            
            TaskPlan plan = TaskPlan.create(userIntent, userMessage);
            if (context != null) {
                plan.setSessionId(context.getSessionId());
                plan.setUserId(context.getUserId());
            }
            
            // 解析步骤
            Object stepsObj = parsed.get("steps");
            if (stepsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> stepsList = (List<Map<String, Object>>) stepsObj;
                
                for (Map<String, Object> stepMap : stepsList) {
                    TaskStep step = parseTaskStep(stepMap);
                    if (step != null) {
                        plan.addStep(step);
                    }
                }
            }
            
            return plan;
            
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse planning response: {}", e.getMessage());
            return TaskPlan.createEmpty("解析失败");
        }
    }
    
    /**
     * 解析单个任务步骤
     */
    private TaskStep parseTaskStep(Map<String, Object> stepMap) {
        try {
            int order = ((Number) stepMap.getOrDefault("order", 1)).intValue();
            String name = (String) stepMap.get("name");
            String toolName = (String) stepMap.get("toolName");
            String description = (String) stepMap.get("description");
            
            if (toolName == null) {
                return null;
            }
            
            // 验证工具是否存在
            if (!toolRegistry.hasTool(toolName)) {
                log.warn("Tool {} not found in registry", toolName);
                return null;
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) stepMap.getOrDefault(
                    "parameters", new HashMap<>());
            
            @SuppressWarnings("unchecked")
            List<Integer> dependencies = stepMap.get("dependencies") instanceof List ?
                    ((List<Number>) stepMap.get("dependencies")).stream()
                            .map(Number::intValue)
                            .collect(Collectors.toList()) :
                    new ArrayList<>();
            
            return TaskStep.builder()
                    .stepId("step-" + order + "-" + System.currentTimeMillis())
                    .order(order)
                    .name(name != null ? name : "步骤" + order)
                    .toolName(toolName)
                    .parameters(parameters)
                    .dependencies(dependencies)
                    .description(description)
                    .maxRetries(properties.getAgent().getMaxRetryAttempts())
                    .timeoutMs(properties.getAgent().getTaskTimeoutMs())
                    .build();
                    
        } catch (Exception e) {
            log.warn("Failed to parse task step: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建执行上下文
     */
    private ToolExecutionContext createExecutionContext(TaskPlan plan, ConversationContext context) {
        // 为所有用户授予基本的知识搜索权限
        // 这些是只读权限，不涉及敏感操作
        Map<String, Boolean> defaultPermissions = new HashMap<>();
        defaultPermissions.put("knowledge.read", true);  // 允许搜索知识库
        defaultPermissions.put("health_data.read", true);  // 允许读取自己的健康数据
        defaultPermissions.put("health_data.write", true);  // 允许记录自己的健康数据
        defaultPermissions.put("diary.read", true);  // 允许读取自己的日记
        defaultPermissions.put("diary.write", true);  // 允许写日记
        defaultPermissions.put("medication.read", true);  // 允许查看用药提醒
        defaultPermissions.put("medication.write", true);  // 允许设置用药提醒
        
        return ToolExecutionContext.builder()
                .userId(plan.getUserId())
                .sessionId(plan.getSessionId())
                .conversationContext(context)
                .userHealthProfile(context != null ? context.getUserProfile() : null)
                .timeoutMs(plan.getTimeoutMs())
                .retryEnabled(true)
                .maxRetries(properties.getAgent().getMaxRetryAttempts())
                .requestId(plan.getPlanId())
                .requestSource("agent")
                .permissions(defaultPermissions)
                .build();
    }
    
    /**
     * 执行单个步骤
     */
    private AgentExecutionResult.StepResult executeStep(TaskStep step, 
                                                        ToolExecutionContext context,
                                                        TaskPlan plan) {
        long startTime = System.currentTimeMillis();
        log.info("Executing step {}: {} with tool {}", step.getOrder(), step.getName(), step.getToolName());
        
        step.markStarted();
        
        try {
            // 检查工具是否需要确认
            Optional<AgentTool> toolOpt = toolRegistry.getTool(step.getToolName());
            if (toolOpt.isPresent() && toolOpt.get().requiresConfirmation() && 
                    !Boolean.TRUE.equals(step.getUserConfirmed())) {
                
                String confirmMsg = String.format("确认执行操作：%s？", step.getName());
                step.markWaitingConfirmation(confirmMsg);
                
                return AgentExecutionResult.StepResult.builder()
                        .stepOrder(step.getOrder())
                        .stepName(step.getName())
                        .toolName(step.getToolName())
                        .success(false)
                        .errorMessage("等待用户确认")
                        .build();
            }
            
            // 执行工具（带重试）
            ToolResult result = toolExecutor.executeWithRetry(
                    step.getToolName(),
                    step.getParameters(),
                    context,
                    step.getMaxRetries()
            );
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (result.isSuccess()) {
                step.markCompleted(result);
                return AgentExecutionResult.StepResult.success(
                        step.getOrder(), step.getName(), step.getToolName(), 
                        result, executionTime);
            } else {
                step.markFailed(result);
                return AgentExecutionResult.StepResult.failure(
                        step.getOrder(), step.getName(), step.getToolName(),
                        result.getErrorMessage(), executionTime);
            }
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Step {} execution failed: {}", step.getOrder(), e.getMessage(), e);
            step.markFailed(e.getMessage());
            
            return AgentExecutionResult.StepResult.failure(
                    step.getOrder(), step.getName(), step.getToolName(),
                    e.getMessage(), executionTime);
        }
    }
    
    /**
     * 并行执行多个步骤
     */
    private List<AgentExecutionResult.StepResult> executeStepsInParallel(
            List<TaskStep> steps, ToolExecutionContext context, TaskPlan plan) {
        
        int maxParallel = Math.min(steps.size(), properties.getAgent().getMaxParallelExecutions());
        List<TaskStep> stepsToExecute = steps.subList(0, maxParallel);
        
        log.info("Executing {} steps in parallel", stepsToExecute.size());
        
        List<CompletableFuture<AgentExecutionResult.StepResult>> futures = stepsToExecute.stream()
                .map(step -> CompletableFuture.supplyAsync(
                        () -> executeStep(step, context, plan), executorService))
                .collect(Collectors.toList());
        
        // 等待所有步骤完成
        return futures.stream()
                .map(future -> {
                    try {
                        return future.get(plan.getTimeoutMs(), TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        log.warn("Step execution timeout");
                        return AgentExecutionResult.StepResult.builder()
                                .success(false)
                                .errorMessage("执行超时")
                                .build();
                    } catch (Exception e) {
                        log.error("Step execution error: {}", e.getMessage());
                        return AgentExecutionResult.StepResult.builder()
                                .success(false)
                                .errorMessage(e.getMessage())
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 综合执行结果
     * Requirement 4.5: Agent framework synthesizes results into coherent response
     */
    private String synthesizeResults(TaskPlan plan, List<AgentExecutionResult.StepResult> stepResults,
                                     ConversationContext context) {
        StringBuilder summary = new StringBuilder();
        
        int successCount = (int) stepResults.stream().filter(r -> r.isSuccess()).count();
        int totalCount = stepResults.size();
        
        if (successCount == totalCount) {
            summary.append("所有任务已成功完成。\n\n");
        } else if (successCount > 0) {
            summary.append(String.format("部分任务已完成（%d/%d成功）。\n\n", successCount, totalCount));
        } else {
            summary.append("任务执行失败。\n\n");
        }
        
        // 添加各步骤结果摘要
        for (AgentExecutionResult.StepResult result : stepResults) {
            if (result.isSuccess() && result.getToolResult() != null && 
                    result.getToolResult().getData() != null) {
                summary.append(String.format("- %s: 完成\n", result.getStepName()));
            } else if (result.isSkipped()) {
                summary.append(String.format("- %s: 跳过 (%s)\n", 
                        result.getStepName(), result.getSkipReason()));
            } else {
                summary.append(String.format("- %s: 失败 (%s)\n", 
                        result.getStepName(), result.getErrorMessage()));
            }
        }
        
        return summary.toString();
    }
    
    /**
     * 从响应中提取JSON
     */
    private String extractJson(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        // 尝试找到JSON块
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        
        return null;
    }
    
    /**
     * 调用LLM生成响应
     * Call LLM to generate response
     * 
     * @param prompt 提示词 (prompt)
     * @return LLM响应 (LLM response)
     */
    private String callLLM(String prompt) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                "你是一个智能任务规划助手，负责分析用户请求并生成任务执行计划。请始终返回有效的JSON格式。"));
        messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
        
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(chatModel)
                .messages(messages)
                .temperature(chatTemperature)
                .maxTokens(chatMaxTokens)
                .build();
        
        try {
            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
        } catch (Exception e) {
            log.error("Error calling LLM: {}", e.getMessage(), e);
            throw new RuntimeException("LLM调用失败: " + e.getMessage(), e);
        }
    }
}
