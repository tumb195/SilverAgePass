package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.AnalysisStatus;
import com.learning.silveragepass.entity.DiaryAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 日记分析仓库接口
 * Repository for diary analysis
 */
@Repository
public interface DiaryAnalysisRepository extends JpaRepository<DiaryAnalysis, String> {

    /**
     * 根据日记条目ID查找分析
     * Find analysis by diary entry ID
     */
    Optional<DiaryAnalysis> findByEntryId(String entryId);

    /**
     * 根据日记条目ID查找所有分析（返回列表）
     * Find all analyses by diary entry ID
     */
    List<DiaryAnalysis> findAllByEntryId(String entryId);

    /**
     * 根据分析状态查找分析，按分析时间排序
     * Find analyses by status, ordered by analyzed time
     */
    List<DiaryAnalysis> findByAnalysisStatusOrderByAnalyzedAtAsc(AnalysisStatus status);

    /**
     * 查找待处理的分析任务（用于队列处理）
     * Find pending analysis tasks (for queue processing)
     */
    @Query("SELECT d FROM DiaryAnalysis d WHERE d.analysisStatus = 'PENDING' ORDER BY d.analysisId")
    List<DiaryAnalysis> findPendingAnalyses();

    /**
     * 查找失败的分析任务（重试次数小于最大值）
     * Find failed analysis tasks (retry count less than max)
     */
    @Query("SELECT d FROM DiaryAnalysis d WHERE d.analysisStatus = 'FAILED' AND d.retryCount < 3 ORDER BY d.analysisId")
    List<DiaryAnalysis> findRetryableFailedAnalyses();

    /**
     * 根据用户ID查找分析（通过日记条目关联）
     * Find analyses by user ID (through diary entry association)
     */
    @Query("SELECT da FROM DiaryAnalysis da " +
           "JOIN HealthDiaryEntry de ON da.entryId = de.entryId " +
           "WHERE de.userId = :userId " +
           "ORDER BY da.analyzedAt DESC")
    List<DiaryAnalysis> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和状态查找分析
     * Find analyses by user ID and status
     */
    @Query("SELECT da FROM DiaryAnalysis da " +
           "JOIN HealthDiaryEntry de ON da.entryId = de.entryId " +
           "WHERE de.userId = :userId AND da.analysisStatus = :status " +
           "ORDER BY da.analyzedAt DESC")
    List<DiaryAnalysis> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") AnalysisStatus status
    );

    /**
     * 统计指定状态的分析数量
     * Count analyses by status
     */
    long countByAnalysisStatus(AnalysisStatus status);

    /**
     * 统计用户的分析数量
     * Count analyses by user ID
     */
    @Query("SELECT COUNT(da) FROM DiaryAnalysis da " +
           "JOIN HealthDiaryEntry de ON da.entryId = de.entryId " +
           "WHERE de.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 查找指定时间之前分析的记录（用于数据保留策略）
     * Find analyses before specified time (for data retention)
     */
    List<DiaryAnalysis> findByAnalyzedAtBefore(LocalDateTime analyzedAt);

    /**
     * 删除日记条目的分析
     * Delete analysis for diary entry
     */
    void deleteByEntryId(String entryId);

    /**
     * 查找处理中但超时的分析任务
     * Find processing analyses that have timed out
     */
    @Query("SELECT d FROM DiaryAnalysis d WHERE d.analysisStatus = 'PROCESSING' AND d.analyzedAt < :timeoutThreshold")
    List<DiaryAnalysis> findTimedOutProcessingAnalyses(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 统计日记条目的分析数量
     * Count analyses by diary entry ID
     */
    long countByEntryId(String entryId);
}
