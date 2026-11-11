package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.HealthConversationMessage;
import com.learning.silveragepass.entity.MessageRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI健康助手消息仓库接口
 * Repository for health conversation messages
 */
@Repository
public interface HealthConversationMessageRepository extends JpaRepository<HealthConversationMessage, String> {

    /**
     * 根据会话ID查找消息，按创建时间降序排列
     * Find messages by session ID, ordered by creation time descending
     */
    List<HealthConversationMessage> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * 根据会话ID查找消息，按创建时间升序排列
     * Find messages by session ID, ordered by creation time ascending
     */
    List<HealthConversationMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 根据会话ID查找最近的N条消息
     * Find recent N messages by session ID
     */
    @Query("SELECT m FROM HealthConversationMessage m WHERE m.sessionId = :sessionId ORDER BY m.createdAt DESC")
    List<HealthConversationMessage> findRecentMessagesBySessionId(
        @Param("sessionId") String sessionId, 
        Pageable pageable
    );

    /**
     * 根据会话ID和角色查找消息
     * Find messages by session ID and role
     */
    List<HealthConversationMessage> findBySessionIdAndRole(String sessionId, MessageRole role);

    /**
     * 统计会话的消息数量
     * Count messages by session ID
     */
    long countBySessionId(String sessionId);

    /**
     * 查找指定时间之前创建的消息（用于数据保留策略）
     * Find messages created before specified time (for data retention)
     */
    List<HealthConversationMessage> findByCreatedAtBefore(LocalDateTime createdAt);

    /**
     * 删除会话的所有消息
     * Delete all messages for a session
     */
    void deleteBySessionId(String sessionId);

    /**
     * 根据情感分析ID查找消息
     * Find message by emotion analysis ID
     */
    List<HealthConversationMessage> findByEmotionAnalysisId(String emotionAnalysisId);

    /**
     * 根据会话ID查找所有消息
     * Find all messages by session ID
     */
    List<HealthConversationMessage> findBySessionId(String sessionId);
}
