package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.EmotionAnalysis;
import com.learning.silveragepass.entity.EmotionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 情感分析仓库接口
 * Repository for emotion analysis
 */
@Repository
public interface EmotionAnalysisRepository extends JpaRepository<EmotionAnalysis, String> {

    /**
     * 根据消息ID查找情感分析
     * Find emotion analysis by message ID
     */
    Optional<EmotionAnalysis> findByMessageId(String messageId);

    /**
     * 根据日记条目ID查找情感分析
     * Find emotion analysis by diary entry ID
     */
    Optional<EmotionAnalysis> findByDiaryEntryId(String diaryEntryId);

    /**
     * 根据日记条目ID查找所有情感分析（返回列表）
     * Find all emotion analyses by diary entry ID
     */
    List<EmotionAnalysis> findAllByDiaryEntryId(String diaryEntryId);

    /**
     * 根据风险等级查找分析，按分析时间降序排列
     * Find analyses by risk level, ordered by analyzed time descending
     */
    List<EmotionAnalysis> findByRiskLevelOrderByAnalyzedAtDesc(Integer riskLevel);

    /**
     * 查找风险等级大于等于指定值的分析
     * Find analyses with risk level greater than or equal to specified value
     */
    List<EmotionAnalysis> findByRiskLevelGreaterThanEqualOrderByAnalyzedAtDesc(Integer riskLevel);

    /**
     * 根据主要情感类型查找分析
     * Find analyses by primary emotion type
     */
    List<EmotionAnalysis> findByPrimaryEmotionOrderByAnalyzedAtDesc(EmotionType primaryEmotion);

    /**
     * 查找指定时间范围内的分析
     * Find analyses within time range
     */
    @Query("SELECT e FROM EmotionAnalysis e WHERE e.analyzedAt BETWEEN :startTime AND :endTime ORDER BY e.analyzedAt DESC")
    List<EmotionAnalysis> findByAnalyzedAtBetween(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据用户ID查找情感分析（通过消息关联）
     * Find emotion analyses by user ID (through message association)
     */
    @Query("SELECT e FROM EmotionAnalysis e " +
           "JOIN HealthConversationMessage m ON e.messageId = m.messageId " +
           "JOIN HealthConversationSession s ON m.sessionId = s.sessionId " +
           "WHERE s.userId = :userId " +
           "ORDER BY e.analyzedAt DESC")
    List<EmotionAnalysis> findByUserIdThroughMessages(@Param("userId") Long userId);

    /**
     * 根据用户ID查找情感分析（通过日记关联）
     * Find emotion analyses by user ID (through diary association)
     */
    @Query("SELECT e FROM EmotionAnalysis e " +
           "JOIN HealthDiaryEntry d ON e.diaryEntryId = d.entryId " +
           "WHERE d.userId = :userId " +
           "ORDER BY e.analyzedAt DESC")
    List<EmotionAnalysis> findByUserIdThroughDiary(@Param("userId") Long userId);

    /**
     * 统计指定风险等级的分析数量
     * Count analyses by risk level
     */
    long countByRiskLevel(Integer riskLevel);

    /**
     * 统计风险等级大于等于指定值的分析数量
     * Count analyses with risk level greater than or equal to specified value
     */
    long countByRiskLevelGreaterThanEqual(Integer riskLevel);

    /**
     * 删除指定时间之前的分析（用于数据保留策略）
     * Delete analyses before specified time (for data retention)
     */
    void deleteByAnalyzedAtBefore(LocalDateTime analyzedAt);

    /**
     * 统计日记条目的情感分析数量
     * Count emotion analyses by diary entry ID
     */
    long countByDiaryEntryId(String diaryEntryId);
}
