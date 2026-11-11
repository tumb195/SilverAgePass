package com.learning.silveragepass.service;

/**
 * 日记分析队列服务接口
 * Diary analysis queue service for asynchronous processing
 */
public interface DiaryAnalysisQueue {

    /**
     * 将日记条目加入分析队列
     * Queue a diary entry for analysis
     * 
     * @param entryId 日记条目ID
     * @param priority 优先级（1=高，2=正常，3=低）
     */
    void queueTask(String entryId, int priority);

    /**
     * 处理分析任务（异步执行）
     * Process analysis task asynchronously
     * 
     * @param entryId 日记条目ID
     */
    void processAnalysisTask(String entryId);

    /**
     * 获取队列深度（待处理任务数量）
     * Get queue depth (number of pending tasks)
     * 
     * @return 待处理任务数量
     */
    int getQueueDepth();

    /**
     * 处理所有排队的任务（由调度器调用）
     * Process all queued tasks (called by scheduler)
     */
    void processQueuedTasks();
}
