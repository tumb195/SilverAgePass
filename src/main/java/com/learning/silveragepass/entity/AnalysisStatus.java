package com.learning.silveragepass.entity;

/**
 * 分析状态枚举
 * Analysis status for diary analysis tasks
 */
public enum AnalysisStatus {
    /**
     * 待处理 - 已排队等待分析
     * Pending - Queued for analysis
     */
    PENDING,
    
    /**
     * 处理中 - 正在进行AI分析
     * Processing - AI analysis in progress
     */
    PROCESSING,
    
    /**
     * 已完成 - 分析成功完成
     * Completed - Analysis successfully completed
     */
    COMPLETED,
    
    /**
     * 失败 - 分析失败（达到最大重试次数）
     * Failed - Analysis failed after maximum retries
     */
    FAILED
}
