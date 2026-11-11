package com.learning.silveragepass.service;

import com.learning.silveragepass.entity.DiaryAnalysis;
import com.learning.silveragepass.entity.HealthDiaryEntry;

import java.time.LocalDate;
import java.util.List;

/**
 * 健康日记服务接口
 * Health diary service for managing diary entries and analysis
 */
public interface HealthDiaryService {

    /**
     * 创建健康日记条目
     * Create a health diary entry with validation
     * 
     * @param userId 用户ID
     * @param entryDate 日记日期
     * @param emotionScore 情绪评分（1-10）
     * @param sleepQuality 睡眠质量（1-5）
     * @param stressLevel 压力水平（1-5）
     * @param diaryContent 日记内容
     * @return 创建的日记条目
     * @throws IllegalArgumentException 如果评分范围无效
     */
    HealthDiaryEntry createEntry(
        Long userId,
        LocalDate entryDate,
        Integer emotionScore,
        Integer sleepQuality,
        Integer stressLevel,
        String diaryContent
    );

    /**
     * 获取用户的日记条目列表（带日期范围过滤）
     * Get user's diary entries with date range filtering
     * 
     * @param userId 用户ID
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 日记条目列表，按日期降序排列
     */
    List<HealthDiaryEntry> getUserEntries(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取日记条目的分析结果
     * Get analysis results for a diary entry
     * 
     * @param entryId 日记条目ID
     * @return 分析结果，如果不存在则返回null
     */
    DiaryAnalysis getEntryAnalysis(String entryId);

    /**
     * 将日记条目加入分析队列
     * Queue a diary entry for asynchronous analysis
     * 
     * @param entryId 日记条目ID
     */
    void queueForAnalysis(String entryId);

    /**
     * 根据ID获取日记条目
     * Get diary entry by ID
     * 
     * @param entryId 日记条目ID
     * @return 日记条目，如果不存在则返回null
     */
    HealthDiaryEntry getEntryById(String entryId);

    /**
     * 更新日记条目
     * Update a diary entry
     * 
     * @param entryId 日记条目ID
     * @param emotionScore 情绪评分（1-10）
     * @param sleepQuality 睡眠质量（1-5）
     * @param stressLevel 压力水平（1-5）
     * @param diaryContent 日记内容
     * @return 更新后的日记条目
     * @throws IllegalArgumentException 如果评分范围无效或条目不存在
     */
    HealthDiaryEntry updateEntry(
        String entryId,
        Integer emotionScore,
        Integer sleepQuality,
        Integer stressLevel,
        String diaryContent
    );

    /**
     * 删除日记条目
     * Delete a diary entry
     * 
     * @param entryId 日记条目ID
     * @throws IllegalArgumentException 如果条目不存在
     */
    void deleteEntry(String entryId);

    /**
     * 验证评分范围
     * Validate score ranges
     * 
     * @param emotionScore 情绪评分（1-10）
     * @param sleepQuality 睡眠质量（1-5）
     * @param stressLevel 压力水平（1-5）
     * @throws IllegalArgumentException 如果任何评分超出有效范围
     */
    void validateScores(Integer emotionScore, Integer sleepQuality, Integer stressLevel);
}
