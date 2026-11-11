package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.entity.AnalysisStatus;
import com.learning.silveragepass.entity.DiaryAnalysis;
import com.learning.silveragepass.entity.HealthDiaryEntry;
import com.learning.silveragepass.repository.DiaryAnalysisRepository;
import com.learning.silveragepass.repository.HealthDiaryEntryRepository;
import com.learning.silveragepass.service.DiaryAnalysisQueue;
import com.learning.silveragepass.service.EmotionAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日记分析队列服务实现类
 * Diary analysis queue service implementation with priority-based processing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryAnalysisQueueImpl implements DiaryAnalysisQueue {

    private final HealthDiaryEntryRepository diaryRepository;
    private final DiaryAnalysisRepository analysisRepository;
    private final EmotionAnalysisService emotionAnalysisService;

    // Priority queue for analysis tasks (lower priority number = higher priority)
    private final PriorityBlockingQueue<AnalysisTask> taskQueue = new PriorityBlockingQueue<>();
    
    // Counter for queue depth monitoring
    private final AtomicInteger queueDepth = new AtomicInteger(0);

    /**
     * 分析任务类（支持优先级排序）
     * Analysis task with priority support
     */
    private static class AnalysisTask implements Comparable<AnalysisTask> {
        private final String entryId;
        private final int priority;
        private final long timestamp;

        public AnalysisTask(String entryId, int priority) {
            this.entryId = entryId;
            this.priority = priority;
            this.timestamp = System.currentTimeMillis();
        }

        public String getEntryId() {
            return entryId;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(AnalysisTask other) {
            // First compare by priority (lower number = higher priority)
            int priorityCompare = Integer.compare(this.priority, other.priority);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // If same priority, compare by timestamp (FIFO)
            return Long.compare(this.timestamp, other.timestamp);
        }
    }

    /**
     * 将日记条目加入分析队列
     */
    @Override
    @Transactional
    public void queueTask(String entryId, int priority) {
        log.info("Queueing diary entry {} for analysis with priority {}", entryId, priority);

        // Validate priority
        if (priority < 1 || priority > 3) {
            throw new IllegalArgumentException("Priority must be between 1 and 3");
        }

        // Check if entry exists
        if (!diaryRepository.existsById(entryId)) {
            throw new IllegalArgumentException("Diary entry not found: " + entryId);
        }

        // Create or update analysis record with PENDING status
        DiaryAnalysis analysis = analysisRepository.findByEntryId(entryId)
            .orElse(new DiaryAnalysis());
        
        analysis.setEntryId(entryId);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis.setRetryCount(0);
        analysis.setErrorMessage(null);
        
        analysisRepository.save(analysis);

        // Add to queue
        taskQueue.offer(new AnalysisTask(entryId, priority));
        queueDepth.incrementAndGet();

        log.debug("Task queued. Current queue depth: {}", queueDepth.get());
    }

    /**
     * 处理分析任务（异步执行）
     */
    @Override
    @Async
    @Transactional
    public void processAnalysisTask(String entryId) {
        log.info("Processing analysis task for entry: {}", entryId);

        try {
            // Get diary entry
            HealthDiaryEntry entry = diaryRepository.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Diary entry not found: " + entryId));

            // Get or create analysis record
            DiaryAnalysis analysis = analysisRepository.findByEntryId(entryId)
                .orElse(new DiaryAnalysis());
            
            analysis.setEntryId(entryId);
            analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
            analysisRepository.save(analysis);

            // Perform emotion analysis on diary content
            log.debug("Analyzing diary content for entry: {}", entryId);
            var emotionAnalysis = emotionAnalysisService.analyzeDiaryEntry(entry);

            // Update analysis with results
            analysis.setEmotionTriggers(emotionAnalysis.getKeywords());
            analysis.setSleepAssessment(generateSleepAssessment(entry));
            analysis.setStressAssessment(generateStressAssessment(entry));
            analysis.setAiInsights(emotionAnalysis.getSuggestions() != null && !emotionAnalysis.getSuggestions().isEmpty() 
                ? String.join("\n", emotionAnalysis.getSuggestions()) 
                : "继续保持良好的生活习惯");
            analysis.setAnalyzedAt(LocalDateTime.now());
            analysis.setAnalysisStatus(AnalysisStatus.COMPLETED);

            analysisRepository.save(analysis);
            log.info("Analysis completed successfully for entry: {}", entryId);

        } catch (Exception e) {
            log.error("Error processing analysis task for entry: {}", entryId, e);
            handleAnalysisFailure(entryId, e.getMessage());
        }
    }

    /**
     * 生成睡眠质量评估
     */
    private String generateSleepAssessment(HealthDiaryEntry entry) {
        int sleepQuality = entry.getSleepQuality();
        if (sleepQuality >= 4) {
            return "睡眠质量良好，请继续保持规律的作息时间。";
        } else if (sleepQuality == 3) {
            return "睡眠质量一般，建议睡前避免使用电子设备，保持卧室安静舒适。";
        } else {
            return "睡眠质量较差，建议咨询医生，检查是否有睡眠障碍或其他健康问题。";
        }
    }

    /**
     * 生成压力水平评估
     */
    private String generateStressAssessment(HealthDiaryEntry entry) {
        int stressLevel = entry.getStressLevel();
        if (stressLevel <= 2) {
            return "压力水平较低，心态良好，继续保持积极乐观的生活态度。";
        } else if (stressLevel == 3) {
            return "压力水平适中，建议适当放松，多与家人朋友交流。";
        } else {
            return "压力水平较高，建议进行放松活动，如散步、听音乐，必要时寻求心理咨询。";
        }
    }

    /**
     * 处理分析失败
     */
    private void handleAnalysisFailure(String entryId, String errorMessage) {
        try {
            DiaryAnalysis analysis = analysisRepository.findByEntryId(entryId)
                .orElse(new DiaryAnalysis());
            
            analysis.setEntryId(entryId);
            int retryCount = analysis.getRetryCount() != null ? analysis.getRetryCount() : 0;
            
            if (retryCount < 3) {
                // Retry with exponential backoff
                analysis.setRetryCount(retryCount + 1);
                analysis.setAnalysisStatus(AnalysisStatus.PENDING);
                analysis.setErrorMessage(errorMessage);
                analysisRepository.save(analysis);
                
                // Calculate backoff delay (1s, 2s, 4s)
                long delayMs = (long) Math.pow(2, retryCount) * 1000;
                log.info("Scheduling retry {} for entry {} after {}ms", retryCount + 1, entryId, delayMs);
                
                // Re-queue with delay (simplified - in production use ScheduledExecutorService)
                try {
                    Thread.sleep(delayMs);
                    queueTask(entryId, 1); // High priority for retries
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry scheduling interrupted for entry: {}", entryId);
                }
            } else {
                // Max retries reached, mark as failed
                analysis.setAnalysisStatus(AnalysisStatus.FAILED);
                analysis.setErrorMessage("Analysis failed after maximum retries: " + errorMessage);
                analysisRepository.save(analysis);
                log.error("Analysis failed permanently for entry {} after {} retries", entryId, retryCount);
            }
        } catch (Exception e) {
            log.error("Error handling analysis failure for entry: {}", entryId, e);
        }
    }

    /**
     * 获取队列深度
     */
    @Override
    public int getQueueDepth() {
        return queueDepth.get();
    }

    /**
     * 处理所有排队的任务（由调度器每分钟调用一次）
     */
    @Override
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void processQueuedTasks() {
        log.debug("Processing queued analysis tasks. Queue depth: {}", queueDepth.get());

        // Process up to 10 tasks per run to avoid overload
        int processedCount = 0;
        int maxTasksPerRun = 10;

        while (processedCount < maxTasksPerRun && !taskQueue.isEmpty()) {
            AnalysisTask task = taskQueue.poll();
            if (task != null) {
                queueDepth.decrementAndGet();
                processAnalysisTask(task.getEntryId());
                processedCount++;
            }
        }

        if (processedCount > 0) {
            log.info("Processed {} analysis tasks. Remaining queue depth: {}", processedCount, queueDepth.get());
        }

        // Also check for any pending analyses in database that might have been missed
        List<DiaryAnalysis> pendingAnalyses = analysisRepository.findPendingAnalyses();
        for (DiaryAnalysis analysis : pendingAnalyses) {
            if (!isInQueue(analysis.getEntryId())) {
                log.info("Re-queueing missed pending analysis for entry: {}", analysis.getEntryId());
                queueTask(analysis.getEntryId(), 2); // Normal priority
            }
        }
    }

    /**
     * 检查条目是否已在队列中
     */
    private boolean isInQueue(String entryId) {
        return taskQueue.stream()
            .anyMatch(task -> task.getEntryId().equals(entryId));
    }
}
