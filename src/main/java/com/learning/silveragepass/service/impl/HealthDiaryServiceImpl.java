package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.entity.DiaryAnalysis;
import com.learning.silveragepass.entity.HealthDiaryEntry;
import com.learning.silveragepass.repository.DiaryAnalysisRepository;
import com.learning.silveragepass.repository.HealthDiaryEntryRepository;
import com.learning.silveragepass.service.DiaryAnalysisQueue;
import com.learning.silveragepass.service.HealthDiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康日记服务实现类
 * Health diary service implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthDiaryServiceImpl implements HealthDiaryService {

    private final HealthDiaryEntryRepository diaryRepository;
    private final DiaryAnalysisRepository analysisRepository;
    private final DiaryAnalysisQueue analysisQueue;

    /**
     * 创建健康日记条目
     * Validates score ranges and queues for analysis
     */
    @Override
    @Transactional
    public HealthDiaryEntry createEntry(
        Long userId,
        LocalDate entryDate,
        Integer emotionScore,
        Integer sleepQuality,
        Integer stressLevel,
        String diaryContent
    ) {
        log.info("Creating diary entry for user {} on date {}", userId, entryDate);

        // Validate required fields
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (entryDate == null) {
            throw new IllegalArgumentException("Entry date cannot be null");
        }
        if (emotionScore == null) {
            throw new IllegalArgumentException("Emotion score cannot be null");
        }
        if (sleepQuality == null) {
            throw new IllegalArgumentException("Sleep quality cannot be null");
        }
        if (stressLevel == null) {
            throw new IllegalArgumentException("Stress level cannot be null");
        }

        // Validate score ranges
        validateScores(emotionScore, sleepQuality, stressLevel);

        // Create diary entry
        HealthDiaryEntry entry = new HealthDiaryEntry();
        entry.setUserId(userId);
        entry.setEntryDate(entryDate);
        entry.setEmotionScore(emotionScore);
        entry.setSleepQuality(sleepQuality);
        entry.setStressLevel(stressLevel);
        entry.setDiaryContent(diaryContent);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());

        // Save entry
        HealthDiaryEntry savedEntry = diaryRepository.save(entry);
        log.info("Diary entry created with ID: {}", savedEntry.getEntryId());

        // Queue for analysis
        queueForAnalysis(savedEntry.getEntryId());

        return savedEntry;
    }

    /**
     * 获取用户的日记条目列表（带日期范围过滤）
     * Returns entries in reverse chronological order
     */
    @Override
    @Transactional(readOnly = true)
    public List<HealthDiaryEntry> getUserEntries(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching diary entries for user {} from {} to {}", userId, startDate, endDate);

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        // If both dates are provided, use date range query
        if (startDate != null && endDate != null) {
            return diaryRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        }
        // If only start date is provided, get entries from that date onwards
        else if (startDate != null) {
            return diaryRepository.findRecentEntriesByUserId(userId, startDate);
        }
        // If no date range specified, return all entries
        else {
            return diaryRepository.findByUserIdOrderByEntryDateDesc(userId);
        }
    }

    /**
     * 获取日记条目的分析结果
     */
    @Override
    @Transactional(readOnly = true)
    public DiaryAnalysis getEntryAnalysis(String entryId) {
        log.debug("Fetching analysis for diary entry: {}", entryId);

        if (entryId == null || entryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry ID cannot be null or empty");
        }

        return analysisRepository.findByEntryId(entryId).orElse(null);
    }

    /**
     * 将日记条目加入分析队列
     * Determines priority based on emotion and stress scores
     */
    @Override
    public void queueForAnalysis(String entryId) {
        log.info("Queueing diary entry {} for analysis", entryId);

        if (entryId == null || entryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry ID cannot be null or empty");
        }

        // Get the entry to determine priority
        HealthDiaryEntry entry = diaryRepository.findById(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Diary entry not found: " + entryId));

        // Determine priority based on scores
        // Priority 1 (High): Low emotion score or high stress level
        // Priority 2 (Normal): Regular entries
        // Priority 3 (Low): High emotion score and low stress
        int priority;
        if (entry.getEmotionScore() <= 3 || entry.getStressLevel() >= 4) {
            priority = 1; // High priority
        } else if (entry.getEmotionScore() >= 8 && entry.getStressLevel() <= 2) {
            priority = 3; // Low priority
        } else {
            priority = 2; // Normal priority
        }

        log.debug("Queueing entry {} with priority {}", entryId, priority);
        analysisQueue.queueTask(entryId, priority);
    }

    /**
     * 根据ID获取日记条目
     */
    @Override
    @Transactional(readOnly = true)
    public HealthDiaryEntry getEntryById(String entryId) {
        log.debug("Fetching diary entry by ID: {}", entryId);

        if (entryId == null || entryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry ID cannot be null or empty");
        }

        return diaryRepository.findById(entryId).orElse(null);
    }

    /**
     * 更新日记条目
     */
    @Override
    @Transactional
    public HealthDiaryEntry updateEntry(
        String entryId,
        Integer emotionScore,
        Integer sleepQuality,
        Integer stressLevel,
        String diaryContent
    ) {
        log.info("Updating diary entry: {}", entryId);

        if (entryId == null || entryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry ID cannot be null or empty");
        }

        // Validate score ranges
        if (emotionScore != null || sleepQuality != null || stressLevel != null) {
            validateScores(emotionScore, sleepQuality, stressLevel);
        }

        // Get existing entry
        HealthDiaryEntry entry = diaryRepository.findById(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Diary entry not found: " + entryId));

        // Update fields if provided
        if (emotionScore != null) {
            entry.setEmotionScore(emotionScore);
        }
        if (sleepQuality != null) {
            entry.setSleepQuality(sleepQuality);
        }
        if (stressLevel != null) {
            entry.setStressLevel(stressLevel);
        }
        if (diaryContent != null) {
            entry.setDiaryContent(diaryContent);
        }

        entry.setUpdatedAt(LocalDateTime.now());

        // Save updated entry
        HealthDiaryEntry updatedEntry = diaryRepository.save(entry);
        log.info("Diary entry updated: {}", entryId);

        // Re-queue for analysis if content or scores changed
        if (emotionScore != null || sleepQuality != null || stressLevel != null || diaryContent != null) {
            queueForAnalysis(entryId);
        }

        return updatedEntry;
    }

    /**
     * 删除日记条目
     */
    @Override
    @Transactional
    public void deleteEntry(String entryId) {
        log.info("Deleting diary entry: {}", entryId);

        if (entryId == null || entryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry ID cannot be null or empty");
        }

        // Check if entry exists
        if (!diaryRepository.existsById(entryId)) {
            throw new IllegalArgumentException("Diary entry not found: " + entryId);
        }

        // Delete associated analysis first (if exists)
        analysisRepository.deleteByEntryId(entryId);

        // Delete diary entry
        diaryRepository.deleteById(entryId);
        log.info("Diary entry deleted: {}", entryId);
    }

    /**
     * 验证评分范围
     * Validates: Requirements 4.2, 10.7
     */
    @Override
    public void validateScores(Integer emotionScore, Integer sleepQuality, Integer stressLevel) {
        // Validate emotion score (1-10)
        if (emotionScore != null && (emotionScore < 1 || emotionScore > 10)) {
            throw new IllegalArgumentException(
                String.format("Emotion score must be between 1 and 10, got: %d", emotionScore)
            );
        }

        // Validate sleep quality (1-5)
        if (sleepQuality != null && (sleepQuality < 1 || sleepQuality > 5)) {
            throw new IllegalArgumentException(
                String.format("Sleep quality must be between 1 and 5, got: %d", sleepQuality)
            );
        }

        // Validate stress level (1-5)
        if (stressLevel != null && (stressLevel < 1 || stressLevel > 5)) {
            throw new IllegalArgumentException(
                String.format("Stress level must be between 1 and 5, got: %d", stressLevel)
            );
        }
    }
}
