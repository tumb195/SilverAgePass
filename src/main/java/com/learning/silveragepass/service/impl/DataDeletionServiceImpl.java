package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.entity.*;
import com.learning.silveragepass.repository.*;
import com.learning.silveragepass.service.DataDeletionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 数据删除请求服务实现
 * Data deletion request service implementation
 * 
 * 处理用户数据删除请求，在30天内永久删除所有用户相关数据
 * Handles user data deletion requests and permanently removes all user-related data within 30 days
 * 
 * 实现完整的数据删除，包括：
 * - 会话记录 (Sessions)
 * - 消息记录 (Messages)
 * - 情感分析 (Emotion analyses)
 * - 日记条目 (Diary entries)
 * - 日记分析 (Diary analyses)
 * 
 * Implements complete data deletion including:
 * - Conversation sessions
 * - Messages
 * - Emotion analyses
 * - Diary entries
 * - Diary analyses
 */
@Slf4j
@Service
public class DataDeletionServiceImpl implements DataDeletionService {

    private final DataDeletionRequestRepository deletionRequestRepository;
    private final HealthConversationSessionRepository sessionRepository;
    private final HealthConversationMessageRepository messageRepository;
    private final EmotionAnalysisRepository emotionAnalysisRepository;
    private final HealthDiaryEntryRepository diaryEntryRepository;
    private final DiaryAnalysisRepository diaryAnalysisRepository;

    public DataDeletionServiceImpl(
            DataDeletionRequestRepository deletionRequestRepository,
            HealthConversationSessionRepository sessionRepository,
            HealthConversationMessageRepository messageRepository,
            EmotionAnalysisRepository emotionAnalysisRepository,
            HealthDiaryEntryRepository diaryEntryRepository,
            DiaryAnalysisRepository diaryAnalysisRepository) {
        this.deletionRequestRepository = deletionRequestRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.emotionAnalysisRepository = emotionAnalysisRepository;
        this.diaryEntryRepository = diaryEntryRepository;
        this.diaryAnalysisRepository = diaryAnalysisRepository;
    }

    @Override
    @Transactional
    public UUID createDeletionRequest(Long userId) {
        log.info("创建数据删除请求 - userId: {}", userId);

        // Check if user already has a pending or processing deletion request
        if (deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionStatus.PENDING) ||
            deletionRequestRepository.existsByUserIdAndStatus(userId, DeletionStatus.PROCESSING)) {
            log.warn("用户已有待处理的删除请求 - userId: {}", userId);
            throw new IllegalStateException("用户已有待处理的删除请求");
        }

        // Create new deletion request
        DataDeletionRequest request = new DataDeletionRequest();
        request.setUserId(userId);
        request.setStatus(DeletionStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        request.setScheduledDeletionDate(LocalDateTime.now().plusDays(30));

        DataDeletionRequest savedRequest = deletionRequestRepository.save(request);
        
        log.info("数据删除请求已创建 - requestId: {}, userId: {}, scheduledDate: {}", 
                 savedRequest.getRequestId(), userId, savedRequest.getScheduledDeletionDate());
        
        // Log for audit trail
        log.info("审计日志 - 数据删除请求 - userId: {}, requestId: {}, timestamp: {}", 
                 userId, savedRequest.getRequestId(), LocalDateTime.now());

        return savedRequest.getRequestId();
    }

    @Override
    @Transactional
    public int executeDataDeletion(Long userId) {
        log.info("开始执行数据删除 - userId: {}", userId);
        
        int totalDeleted = 0;

        try {
            // 1. Delete emotion analyses associated with user's messages
            List<HealthConversationSession> sessions = sessionRepository.findByUserId(userId);
            for (HealthConversationSession session : sessions) {
                List<HealthConversationMessage> messages = messageRepository.findBySessionId(session.getSessionId());
                for (HealthConversationMessage message : messages) {
                    if (message.getEmotionAnalysisId() != null) {
                        emotionAnalysisRepository.deleteById(message.getEmotionAnalysisId());
                        totalDeleted++;
                    }
                }
            }
            log.info("已删除消息相关的情感分析记录 - count: {}", totalDeleted);

            // 2. Delete emotion analyses associated with user's diary entries
            List<HealthDiaryEntry> diaryEntries = diaryEntryRepository.findByUserId(userId);
            int diaryEmotionCount = 0;
            for (HealthDiaryEntry entry : diaryEntries) {
                List<EmotionAnalysis> analyses = emotionAnalysisRepository.findAllByDiaryEntryId(entry.getEntryId());
                diaryEmotionCount += analyses.size();
                emotionAnalysisRepository.deleteAll(analyses);
            }
            totalDeleted += diaryEmotionCount;
            log.info("已删除日记相关的情感分析记录 - count: {}", diaryEmotionCount);

            // 3. Delete diary analyses
            int diaryAnalysisCount = 0;
            for (HealthDiaryEntry entry : diaryEntries) {
                List<DiaryAnalysis> analyses = diaryAnalysisRepository.findAllByEntryId(entry.getEntryId());
                diaryAnalysisCount += analyses.size();
                diaryAnalysisRepository.deleteAll(analyses);
            }
            totalDeleted += diaryAnalysisCount;
            log.info("已删除日记分析记录 - count: {}", diaryAnalysisCount);

            // 4. Delete diary entries
            int diaryEntryCount = diaryEntries.size();
            diaryEntryRepository.deleteAll(diaryEntries);
            totalDeleted += diaryEntryCount;
            log.info("已删除日记条目 - count: {}", diaryEntryCount);

            // 5. Delete messages (cascade will handle emotion analyses if not already deleted)
            int messageCount = 0;
            for (HealthConversationSession session : sessions) {
                List<HealthConversationMessage> messages = messageRepository.findBySessionId(session.getSessionId());
                messageCount += messages.size();
                messageRepository.deleteAll(messages);
            }
            totalDeleted += messageCount;
            log.info("已删除消息记录 - count: {}", messageCount);

            // 6. Delete sessions
            int sessionCount = sessions.size();
            sessionRepository.deleteAll(sessions);
            totalDeleted += sessionCount;
            log.info("已删除会话记录 - count: {}", sessionCount);

            log.info("数据删除完成 - userId: {}, totalDeleted: {}", userId, totalDeleted);
            
            // Log for audit trail
            log.info("审计日志 - 数据删除完成 - userId: {}, recordsDeleted: {}, timestamp: {}", 
                     userId, totalDeleted, LocalDateTime.now());

            return totalDeleted;

        } catch (Exception e) {
            log.error("执行数据删除时发生错误 - userId: {}", userId, e);
            throw new RuntimeException("数据删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Scheduled(cron = "${app.data-deletion.schedule:0 0 3 * * ?}") // Daily at 3 AM
    @Transactional
    public int processPendingDeletionRequests() {
        log.info("开始处理待处理的数据删除请求");

        try {
            // Find pending requests that are due for processing (scheduled date has passed)
            List<DataDeletionRequest> pendingRequests = deletionRequestRepository
                .findByStatusAndScheduledDeletionDateBefore(DeletionStatus.PENDING, LocalDateTime.now());

            if (pendingRequests.isEmpty()) {
                log.info("没有待处理的删除请求");
                return 0;
            }

            log.info("找到 {} 个待处理的删除请求", pendingRequests.size());

            int processedCount = 0;
            for (DataDeletionRequest request : pendingRequests) {
                try {
                    // Update status to processing
                    request.setStatus(DeletionStatus.PROCESSING);
                    deletionRequestRepository.save(request);

                    // Execute deletion
                    int deletedCount = executeDataDeletion(request.getUserId());

                    // Update request as completed
                    request.setStatus(DeletionStatus.COMPLETED);
                    request.setProcessedAt(LocalDateTime.now());
                    request.setRecordsDeleted(deletedCount);
                    deletionRequestRepository.save(request);

                    processedCount++;
                    log.info("删除请求处理成功 - requestId: {}, userId: {}, recordsDeleted: {}", 
                             request.getRequestId(), request.getUserId(), deletedCount);

                } catch (Exception e) {
                    log.error("处理删除请求失败 - requestId: {}, userId: {}", 
                             request.getRequestId(), request.getUserId(), e);
                    
                    // Update request as failed
                    request.setStatus(DeletionStatus.FAILED);
                    request.setProcessedAt(LocalDateTime.now());
                    request.setErrorMessage(e.getMessage());
                    deletionRequestRepository.save(request);
                }
            }

            log.info("数据删除请求处理完成 - 成功处理: {}/{}", processedCount, pendingRequests.size());
            return processedCount;

        } catch (Exception e) {
            log.error("处理待处理删除请求时发生错误", e);
            throw new RuntimeException("处理删除请求失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getDeletionRequestStatus(Long userId) {
        return deletionRequestRepository.findFirstByUserIdOrderByRequestedAtDesc(userId)
            .map(request -> request.getStatus().name())
            .orElse(null);
    }

    @Override
    public long countUserData(Long userId) {
        long count = 0;

        // Count sessions
        count += sessionRepository.countByUserId(userId);

        // Count messages
        List<HealthConversationSession> sessions = sessionRepository.findByUserId(userId);
        for (HealthConversationSession session : sessions) {
            count += messageRepository.countBySessionId(session.getSessionId());
        }

        // Count diary entries
        count += diaryEntryRepository.countByUserId(userId);

        // Count emotion analyses (both message and diary related)
        for (HealthConversationSession session : sessions) {
            List<HealthConversationMessage> messages = messageRepository.findBySessionId(session.getSessionId());
            for (HealthConversationMessage message : messages) {
                if (message.getEmotionAnalysisId() != null) {
                    count++;
                }
            }
        }

        List<HealthDiaryEntry> diaryEntries = diaryEntryRepository.findByUserId(userId);
        for (HealthDiaryEntry entry : diaryEntries) {
            count += emotionAnalysisRepository.countByDiaryEntryId(entry.getEntryId());
        }

        // Count diary analyses
        for (HealthDiaryEntry entry : diaryEntries) {
            count += diaryAnalysisRepository.countByEntryId(entry.getEntryId());
        }

        return count;
    }
}
