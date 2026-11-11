package com.learning.silveragepass.service.agent;

import com.learning.silveragepass.config.RAGAgentProperties;
import com.learning.silveragepass.dto.agent.TaskPlan;
import com.learning.silveragepass.dto.agent.TaskStep;
import com.learning.silveragepass.dto.agent.TaskStepStatus;
import com.learning.silveragepass.dto.rag.ConversationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 并行任务执行器
 * Parallel Task Executor
 * 
 * 支持独立子任务的并行执行，提高Agent任务执行效率。
 * 
 * Requirements:
 * - 11.3: Agent framework executes independent subtasks in parallel when possible
 * - 11.5: When knowledge retrieval exceeds 2 seconds, provide loading prompt
 * - 4.6: Agent framework supports task execution timeout control (default 30s)
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-20
 */
@Component
public class ParallelTaskExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(ParallelTaskExecutor.class);
    
    private final RAGAgentProperties properties;
    private final ToolExecutor toolExecutor;
    private final ExecutorService executorService;
    
    @Autowired
    public ParallelTaskExecutor(RAGAgentProperties properties, ToolExecutor toolExecutor) {
        this.properties = properties;
        this.toolExecutor = toolExecutor;
        
        // 创建固定大小的线程池
        int maxParallel = properties.getAgent().getMaxParallelExecutions();
        this.executorService = Executors.newFixedThreadPool(maxParallel, r -> {
            Thread t = new Thread(r, "agent-task-executor");
            t.setDaemon(true);
            return t;
        });
        
        log.info("Parallel task executor initialized with {} threads", maxParallel);
    }
    
    /**
     * 执行任务计划
     * Execute task plan
     * 
     * 分析任务依赖关系，并行执行独立任务。
     * 
     * @param plan 任务计划 (task plan)
     * @param context 对话上下文 (conversation context)
     * @return 执行结果列表 (list of execution results)
     */
    public List<StepExecutionResult> execute(TaskPlan plan, ConversationContext context) {
        if (plan == null || plan.getSteps() == null || plan.getSteps().isEmpty()) {
            return new ArrayList<>();
        }
        
        log.info("Executing task plan: {} with {} steps", plan.getPlanId(), plan.getSteps().size());
        
        List<TaskStep> steps = plan.getSteps();
        List<StepExecutionResult> results = new ArrayList<>();
        Map<Integer, StepExecutionResult> resultMap = new ConcurrentHashMap<>();
        
        // 按依赖关系分组执行
        List<List<TaskStep>> executionGroups = groupByDependencies(steps);
        
        for (int groupIndex = 0; groupIndex < executionGroups.size(); groupIndex++) {
            List<TaskStep> group = executionGroups.get(groupIndex);
            log.debug("Executing group {} with {} steps", groupIndex, group.size());
            
            // 并行执行组内的独立任务
            List<StepExecutionResult> groupResults = executeGroup(group, context, resultMap);
            results.addAll(groupResults);
            
            // 检查是否有失败的任务
            boolean hasFailure = groupResults.stream().anyMatch(r -> !r.isSuccess());
            if (hasFailure) {
                log.warn("Group {} has failed steps, stopping execution", groupIndex);
                break;
            }
        }
        
        return results;
    }
    
    /**
     * 执行一组独立任务
     * Execute a group of independent tasks
     * 
     * @param steps 任务步骤列表 (list of task steps)
     * @param context 对话上下文 (conversation context)
     * @param resultMap 结果映射 (result map)
     * @return 执行结果列表 (list of execution results)
     */
    private List<StepExecutionResult> executeGroup(List<TaskStep> steps, 
                                                    ConversationContext context,
                                                    Map<Integer, StepExecutionResult> resultMap) {
        if (steps.size() == 1) {
            // 单个任务，直接执行
            StepExecutionResult result = executeStep(steps.get(0), context, resultMap);
            resultMap.put(steps.get(0).getOrder(), result);
            return Collections.singletonList(result);
        }
        
        // 多个任务，并行执行
        long timeout = properties.getAgent().getTaskTimeoutMs();
        List<Future<StepExecutionResult>> futures = new ArrayList<>();
        
        for (TaskStep step : steps) {
            Future<StepExecutionResult> future = executorService.submit(() -> 
                    executeStep(step, context, resultMap));
            futures.add(future);
        }
        
        // 收集结果
        List<StepExecutionResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            TaskStep step = steps.get(i);
            Future<StepExecutionResult> future = futures.get(i);
            
            try {
                StepExecutionResult result = future.get(timeout, TimeUnit.MILLISECONDS);
                resultMap.put(step.getOrder(), result);
                results.add(result);
            } catch (TimeoutException e) {
                log.warn("Step {} timed out", step.getOrder());
                StepExecutionResult timeoutResult = StepExecutionResult.timeout(step);
                resultMap.put(step.getOrder(), timeoutResult);
                results.add(timeoutResult);
                future.cancel(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                StepExecutionResult interruptedResult = StepExecutionResult.failure(step, "执行被中断");
                resultMap.put(step.getOrder(), interruptedResult);
                results.add(interruptedResult);
            } catch (ExecutionException e) {
                log.error("Step {} execution failed: {}", step.getOrder(), e.getCause().getMessage());
                StepExecutionResult failureResult = StepExecutionResult.failure(step, e.getCause().getMessage());
                resultMap.put(step.getOrder(), failureResult);
                results.add(failureResult);
            }
        }
        
        return results;
    }
    
    /**
     * 执行单个任务步骤
     * Execute single task step
     * 
     * @param step 任务步骤 (task step)
     * @param context 对话上下文 (conversation context)
     * @param resultMap 结果映射（用于获取依赖结果）(result map for dependency results)
     * @return 执行结果 (execution result)
     */
    private StepExecutionResult executeStep(TaskStep step, ConversationContext context,
                                            Map<Integer, StepExecutionResult> resultMap) {
        log.debug("Executing step {}: {}", step.getOrder(), step.getToolName());
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查依赖是否都已完成
            if (step.getDependencies() != null && !step.getDependencies().isEmpty()) {
                for (Integer depOrder : step.getDependencies()) {
                    StepExecutionResult depResult = resultMap.get(depOrder);
                    if (depResult == null || !depResult.isSuccess()) {
                        return StepExecutionResult.skipped(step, "依赖任务未完成或失败");
                    }
                }
            }
            
            // 为所有用户授予基本的知识搜索权限
            Map<String, Boolean> defaultPermissions = new HashMap<>();
            defaultPermissions.put("knowledge.read", true);
            defaultPermissions.put("health_data.read", true);
            defaultPermissions.put("health_data.write", true);
            defaultPermissions.put("diary.read", true);
            defaultPermissions.put("diary.write", true);
            defaultPermissions.put("medication.read", true);
            defaultPermissions.put("medication.write", true);
            
            // 构建执行上下文
            ToolExecutionContext execContext = ToolExecutionContext.builder()
                    .userId(context.getUserId())
                    .sessionId(context.getSessionId())
                    .permissions(defaultPermissions)
                    .build();
            
            // 执行工具
            ToolResult toolResult = toolExecutor.execute(
                    step.getToolName(), 
                    step.getParameters(), 
                    execContext);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (toolResult.isSuccess()) {
                step.setStatus(TaskStepStatus.COMPLETED);
                return StepExecutionResult.success(step, toolResult.getData(), executionTime);
            } else {
                step.setStatus(TaskStepStatus.FAILED);
                return StepExecutionResult.failure(step, toolResult.getErrorMessage(), executionTime);
            }
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Step {} execution error: {}", step.getOrder(), e.getMessage(), e);
            step.setStatus(TaskStepStatus.FAILED);
            return StepExecutionResult.failure(step, e.getMessage(), executionTime);
        }
    }
    
    /**
     * 按依赖关系分组任务
     * Group tasks by dependencies
     * 
     * 将任务分成多个组，每组内的任务可以并行执行，
     * 组之间按顺序执行。
     * 
     * @param steps 任务步骤列表 (list of task steps)
     * @return 分组后的任务列表 (grouped task lists)
     */
    private List<List<TaskStep>> groupByDependencies(List<TaskStep> steps) {
        List<List<TaskStep>> groups = new ArrayList<>();
        Set<Integer> completed = new HashSet<>();
        List<TaskStep> remaining = new ArrayList<>(steps);
        
        while (!remaining.isEmpty()) {
            List<TaskStep> currentGroup = new ArrayList<>();
            
            for (TaskStep step : remaining) {
                // 检查所有依赖是否都已完成
                boolean canExecute = true;
                if (step.getDependencies() != null) {
                    for (Integer dep : step.getDependencies()) {
                        if (!completed.contains(dep)) {
                            canExecute = false;
                            break;
                        }
                    }
                }
                
                if (canExecute) {
                    currentGroup.add(step);
                }
            }
            
            if (currentGroup.isEmpty()) {
                // 存在循环依赖或无法解决的依赖
                log.warn("Cannot resolve dependencies for remaining {} steps", remaining.size());
                // 将剩余任务作为一组
                groups.add(remaining);
                break;
            }
            
            groups.add(currentGroup);
            
            // 标记为已完成
            for (TaskStep step : currentGroup) {
                completed.add(step.getOrder());
            }
            
            // 从剩余列表中移除
            remaining.removeAll(currentGroup);
        }
        
        return groups;
    }
    
    /**
     * 关闭执行器
     * Shutdown executor
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Parallel task executor shutdown");
    }
    
    /**
     * 步骤执行结果
     */
    public static class StepExecutionResult {
        private TaskStep step;
        private boolean success;
        private Object data;
        private String errorMessage;
        private long executionTimeMs;
        private boolean skipped;
        private boolean timeout;
        
        public static StepExecutionResult success(TaskStep step, Object data, long executionTimeMs) {
            StepExecutionResult result = new StepExecutionResult();
            result.step = step;
            result.success = true;
            result.data = data;
            result.executionTimeMs = executionTimeMs;
            return result;
        }
        
        public static StepExecutionResult failure(TaskStep step, String errorMessage) {
            return failure(step, errorMessage, 0);
        }
        
        public static StepExecutionResult failure(TaskStep step, String errorMessage, long executionTimeMs) {
            StepExecutionResult result = new StepExecutionResult();
            result.step = step;
            result.success = false;
            result.errorMessage = errorMessage;
            result.executionTimeMs = executionTimeMs;
            return result;
        }
        
        public static StepExecutionResult skipped(TaskStep step, String reason) {
            StepExecutionResult result = new StepExecutionResult();
            result.step = step;
            result.success = false;
            result.skipped = true;
            result.errorMessage = reason;
            return result;
        }
        
        public static StepExecutionResult timeout(TaskStep step) {
            StepExecutionResult result = new StepExecutionResult();
            result.step = step;
            result.success = false;
            result.timeout = true;
            result.errorMessage = "执行超时";
            return result;
        }
        
        // Getters
        public TaskStep getStep() { return step; }
        public boolean isSuccess() { return success; }
        public Object getData() { return data; }
        public String getErrorMessage() { return errorMessage; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public boolean isSkipped() { return skipped; }
        public boolean isTimeout() { return timeout; }
    }
}
