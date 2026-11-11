package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.HealthConversationSession;
import com.learning.silveragepass.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI健康助手会话仓库接口
 * Repository for health conversation sessions
 */
@Repository
public interface HealthConversationSessionRepository extends JpaRepository<HealthConversationSession, String> {

    /**
     * 根据用户ID和状态查找会话
     * Find sessions by user ID and status
     */
    List<HealthConversationSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    /**
     * 根据用户ID查找所有会话，按最后活动时间降序排列
     * Find all sessions by user ID, ordered by last activity descending
     */
    List<HealthConversationSession> findByUserIdOrderByLastActivityAtDesc(Long userId);

    /**
     * 根据用户ID查找活跃会话
     * Find active session by user ID
     */
    Optional<HealthConversationSession> findFirstByUserIdAndStatusOrderByLastActivityAtDesc(
        Long userId, 
        SessionStatus status
    );

    /**
     * 查找在指定时间之前最后活动的会话
     * Find sessions with last activity before specified time
     */
    List<HealthConversationSession> findByLastActivityAtBeforeAndStatus(
        LocalDateTime lastActivityAt, 
        SessionStatus status
    );

    /**
     * 统计用户的会话数量
     * Count sessions by user ID
     */
    long countByUserId(Long userId);

    /**
     * 统计用户指定状态的会话数量
     * Count sessions by user ID and status
     */
    long countByUserIdAndStatus(Long userId, SessionStatus status);

    /**
     * 删除用户的所有会话（用于数据删除请求）
     * Delete all sessions for a user (for data deletion requests)
     */
    void deleteByUserId(Long userId);

    /**
     * 根据用户ID查找所有会话
     * Find all sessions by user ID
     */
    List<HealthConversationSession> findByUserId(Long userId);
}
