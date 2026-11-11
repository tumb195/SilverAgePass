package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.Notification;
import com.learning.silveragepass.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知数据访问层
 * 提供通知的分页查询、类型筛选、未读统计等功能
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // 分页查询用户的所有通知（按创建时间倒序）
    Page<Notification> findByReceiverIdOrderByCreateTimeDesc(Long receiverId, Pageable pageable);
    
    // 按类型筛选用户的通知（分页）
    Page<Notification> findByReceiverIdAndTypeOrderByCreateTimeDesc(
            Long receiverId, NotificationType type, Pageable pageable);
    
    // 按分类筛选用户的通知（分页）
    @Query("SELECT n FROM Notification n WHERE n.receiverId = :receiverId " +
           "AND n.type IN :types ORDER BY n.createTime DESC")
    Page<Notification> findByReceiverIdAndTypeInOrderByCreateTimeDesc(
            @Param("receiverId") Long receiverId, 
            @Param("types") List<NotificationType> types, 
            Pageable pageable);
    
    // 查询用户的未读通知总数
    Long countByReceiverIdAndIsReadFalse(Long receiverId);
    
    // 按类型统计用户的未读通知数
    @Query("SELECT n.type, COUNT(n) FROM Notification n " +
           "WHERE n.receiverId = :receiverId AND n.isRead = false " +
           "GROUP BY n.type")
    List<Object[]> countUnreadByReceiverIdGroupByType(@Param("receiverId") Long receiverId);
    
    // 标记单条通知为已读
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readTime = CURRENT_TIMESTAMP " +
           "WHERE n.id = :id AND n.receiverId = :receiverId")
    int markAsRead(@Param("id") Long id, @Param("receiverId") Long receiverId);
    
    // 标记用户所有通知为已读
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readTime = CURRENT_TIMESTAMP " +
           "WHERE n.receiverId = :receiverId AND n.isRead = false")
    int markAllAsRead(@Param("receiverId") Long receiverId);
    
    // 按类型标记用户通知为已读
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readTime = CURRENT_TIMESTAMP " +
           "WHERE n.receiverId = :receiverId AND n.type = :type AND n.isRead = false")
    int markAsReadByType(@Param("receiverId") Long receiverId, @Param("type") NotificationType type);
    
    // 删除用户的指定通知
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.id = :id AND n.receiverId = :receiverId")
    int deleteByIdAndReceiverId(@Param("id") Long id, @Param("receiverId") Long receiverId);
    
    // 查询用户未读通知列表
    List<Notification> findByReceiverIdAndIsReadFalseOrderByCreateTimeDesc(Long receiverId);
    
    // 查询关联业务ID的通知
    List<Notification> findByRelatedIdAndTypeOrderByCreateTimeDesc(Long relatedId, NotificationType type);
}
