package com.learning.silveragepass.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Agent任务计划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPlan {

    private String planId;
    private String userIntent;
    private String originalMessage;

    @Builder.Default
    private List<TaskStep> steps = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Builder.Default
    private TaskPlanStatus status = TaskPlanStatus.CREATED;

    private String sessionId;
    private Long userId;
    private String resultSummary;
    private String errorMessage;

    @Builder.Default
    private long timeoutMs = 30000;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();


    public enum TaskPlanStatus {
        CREATED("已创建", "created"),
        EXECUTING("执行中", "executing"),
        COMPLETED("已完成", "completed"),
        PARTIALLY_COMPLETED("部分完成", "partially_completed"),
        FAILED("执行失败", "failed"),
        CANCELLED("已取消", "cancelled"),
        WAITING_CONFIRMATION("等待确认", "waiting_confirmation");

        private final String description;
        private final String code;

        TaskPlanStatus(String description, String code) {
            this.description = description;
            this.code = code;
        }

        public String getDescription() { return description; }
        public String getCode() { return code; }

        public boolean isTerminal() {
            return this == COMPLETED || this == PARTIALLY_COMPLETED ||
                    this == FAILED || this == CANCELLED;
        }
    }

    public TaskPlan addStep(TaskStep step) {
        if (steps == null) steps = new ArrayList<>();
        steps.add(step);
        return this;
    }

    public TaskPlan addSteps(List<TaskStep> newSteps) {
        if (steps == null) steps = new ArrayList<>();
        steps.addAll(newSteps);
        return this;
    }

    public Optional<TaskStep> getStepByOrder(int order) {
        if (steps == null) return Optional.empty();
        return steps.stream().filter(s -> s.getOrder() == order).findFirst();
    }

    public Optional<TaskStep> getStepById(String stepId) {
        if (steps == null) return Optional.empty();
        return steps.stream().filter(s -> stepId.equals(s.getStepId())).findFirst();
    }

    public Optional<TaskStep> getNextExecutableStep() {
        if (steps == null || steps.isEmpty()) return Optional.empty();
        return steps.stream()
                .filter(TaskStep::canExecute)
                .filter(this::areDependenciesSatisfied)
                .findFirst();
    }

    public List<TaskStep> getParallelExecutableSteps() {
        if (steps == null || steps.isEmpty()) return new ArrayList<>();
        return steps.stream()
                .filter(TaskStep::canExecute)
                .filter(this::areDependenciesSatisfied)
                .collect(Collectors.toList());
    }

    public boolean areDependenciesSatisfied(TaskStep step) {
        if (!step.hasDependencies()) return true;
        return step.getDependencies().stream()
                .allMatch(depOrder -> {
                    Optional<TaskStep> depStep = getStepByOrder(depOrder);
                    return depStep.isPresent() && depStep.get().isCompleted();
                });
    }

    public List<TaskStep> getCompletedSteps() {
        if (steps == null) return new ArrayList<>();
        return steps.stream().filter(TaskStep::isCompleted).collect(Collectors.toList());
    }

    public List<TaskStep> getFailedSteps() {
        if (steps == null) return new ArrayList<>();
        return steps.stream().filter(TaskStep::isFailed).collect(Collectors.toList());
    }

    public List<TaskStep> getPendingSteps() {
        if (steps == null) return new ArrayList<>();
        return steps.stream().filter(s -> s.getStatus() == TaskStepStatus.PENDING).collect(Collectors.toList());
    }

    public List<TaskStep> getRunningSteps() {
        if (steps == null) return new ArrayList<>();
        return steps.stream().filter(s -> s.getStatus() == TaskStepStatus.RUNNING).collect(Collectors.toList());
    }

    public boolean isAllStepsCompleted() {
        if (steps == null || steps.isEmpty()) return true;
        return steps.stream().allMatch(TaskStep::isCompleted);
    }

    public boolean hasFailedSteps() {
        if (steps == null) return false;
        return steps.stream().anyMatch(TaskStep::isFailed);
    }

    public boolean isAllStepsTerminated() {
        if (steps == null || steps.isEmpty()) return true;
        return steps.stream().allMatch(TaskStep::isTerminal);
    }

    public int getTotalStepCount() { return steps != null ? steps.size() : 0; }
    public int getCompletedStepCount() { return getCompletedSteps().size(); }

    public double getProgressPercentage() {
        int total = getTotalStepCount();
        if (total == 0) return 100.0;
        return (double) getCompletedStepCount() / total * 100.0;
    }

    public void markStarted() {
        this.status = TaskPlanStatus.EXECUTING;
        this.startedAt = LocalDateTime.now();
    }

    public void markCompleted(String resultSummary) {
        this.status = TaskPlanStatus.COMPLETED;
        this.resultSummary = resultSummary;
        this.completedAt = LocalDateTime.now();
    }

    public void markPartiallyCompleted(String resultSummary) {
        this.status = TaskPlanStatus.PARTIALLY_COMPLETED;
        this.resultSummary = resultSummary;
        this.completedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = TaskPlanStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    public void markCancelled(String reason) {
        this.status = TaskPlanStatus.CANCELLED;
        this.errorMessage = reason;
        this.completedAt = LocalDateTime.now();
    }

    public void markWaitingConfirmation() {
        this.status = TaskPlanStatus.WAITING_CONFIRMATION;
    }

    public void updateStatusFromSteps() {
        if (!isAllStepsTerminated()) return;
        if (isAllStepsCompleted()) {
            markCompleted(generateResultSummary());
        } else if (hasFailedSteps()) {
            if (getCompletedStepCount() > 0) {
                markPartiallyCompleted(generateResultSummary());
            } else {
                markFailed("所有步骤执行失败");
            }
        }
    }

    private String generateResultSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("任务计划执行完成。");
        summary.append(String.format("共%d个步骤，完成%d个", getTotalStepCount(), getCompletedStepCount()));
        List<TaskStep> failed = getFailedSteps();
        if (!failed.isEmpty()) {
            summary.append(String.format("，失败%d个", failed.size()));
        }
        return summary.toString();
    }

    public boolean isTimeout() {
        if (startedAt == null) return false;
        return getExecutionDurationMs() > timeoutMs;
    }

    public long getExecutionDurationMs() {
        if (startedAt == null) return -1;
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).toMillis();
    }

    public TaskPlan addMetadata(String key, Object value) {
        if (metadata == null) metadata = new HashMap<>();
        metadata.put(key, value);
        return this;
    }

    public Object getMetadata(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    public static TaskPlan create(String userIntent, String originalMessage) {
        return TaskPlan.builder()
                .planId("plan-" + System.currentTimeMillis())
                .userIntent(userIntent)
                .originalMessage(originalMessage)
                .createdAt(LocalDateTime.now())
                .status(TaskPlanStatus.CREATED)
                .build();
    }

    public static TaskPlan create(String userIntent, String originalMessage, String sessionId, Long userId) {
        return TaskPlan.builder()
                .planId("plan-" + System.currentTimeMillis())
                .userIntent(userIntent)
                .originalMessage(originalMessage)
                .sessionId(sessionId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .status(TaskPlanStatus.CREATED)
                .build();
    }

    public static TaskPlan createEmpty(String userIntent) {
        TaskPlan plan = create(userIntent, null);
        plan.markCompleted("无需执行工具调用");
        return plan;
    }

    @Override
    public String toString() {
        return String.format("TaskPlan{id='%s', intent='%s', status=%s, steps=%d, progress=%.1f%%}",
                planId, userIntent, status, getTotalStepCount(), getProgressPercentage());
    }
}
