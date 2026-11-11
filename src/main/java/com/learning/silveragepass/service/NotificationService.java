package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.notification.NotificationDTO;
import com.learning.silveragepass.dto.notification.UnreadCountDTO;
import com.learning.silveragepass.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 统一通知服务接口
 * 提供消息推送、持久化、查询和状态管理功能
 * 
 * Requirements: 2.1, 2.2, 3.1, 3.2, 3.3, 3.4, 3.5
 */
public interface NotificationService {
    
    // ==================== 消息发送方法 ====================
    
    /**
     * 发送通知（自动判断在线/离线）
     * - 在线用户：通过WebSocket实时推送
     * - 离线用户：存入Redis离线队列
     * 
     * @param notification 通知内容
     * Requirements: 2.1, 2.2
     */
    void sendNotification(NotificationDTO notification);
    
    /**
     * 批量发送通知
     * 
     * @param notifications 通知列表
     * Requirements: 2.1, 2.2
     */
    void sendNotifications(List<NotificationDTO> notifications);
    
    /**
     * 发送给指定用户
     * 
     * @param userId 目标用户ID
     * @param notification 通知内容
     * Requirements: 2.1, 2.2
     */
    void sendToUser(Long userId, NotificationDTO notification);
    
    /**
     * 广播给所有在线用户
     * 
     * @param notification 通知内容
     * Requirements: 2.1
     */
    void broadcast(NotificationDTO notification);
    
    /**
     * 推送用户的离线消息
     * 当用户上线时调用，推送其所有离线消息
     * 
     * @param userId 用户ID
     * Requirements: 2.3, 2.4
     */
    void pushOfflineMessages(Long userId);
    
    // ==================== 消息查询方法 ====================
    
    /**
     * 获取用户消息列表（分页）
     * 
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页的通知列表
     * Requirements: 3.2
     */
    Page<NotificationDTO> getUserNotifications(Long userId, Pageable pageable);
    
    /**
     * 按类型获取用户消息（分页）
     * 
     * @param userId 用户ID
     * @param type 通知类型
     * @param pageable 分页参数
     * @return 分页的通知列表
     * Requirements: 3.3
     */
    Page<NotificationDTO> getUserNotificationsByType(Long userId, NotificationType type, Pageable pageable);
    
    /**
     * 按分类获取用户消息（分页）
     * 
     * @param userId 用户ID
     * @param category 通知分类（system, health, demand, forum, social）
     * @param pageable 分页参数
     * @return 分页的通知列表
     * Requirements: 3.3
     */
    Page<NotificationDTO> getUserNotificationsByCategory(Long userId, String category, Pageable pageable);
    
    /**
     * 获取用户未读消息列表
     * 
     * @param userId 用户ID
     * @return 未读通知列表
     * Requirements: 3.2
     */
    List<NotificationDTO> getUnreadNotifications(Long userId);
    
    // ==================== 未读统计方法 ====================
    
    /**
     * 获取用户未读消息统计
     * 
     * @param userId 用户ID
     * @return 未读统计（总数、按分类、按类型）
     * Requirements: 8.1
     */
    UnreadCountDTO getUnreadCount(Long userId);
    
    // ==================== 状态管理方法 ====================
    
    /**
     * 标记单条消息为已读
     * 
     * @param notificationId 通知ID
     * @param userId 用户ID（用于权限验证）
     * @return 是否成功
     * Requirements: 3.4
     */
    boolean markAsRead(Long notificationId, Long userId);
    
    /**
     * 标记用户所有消息为已读
     * 
     * @param userId 用户ID
     * @return 标记的消息数量
     * Requirements: 3.4, 8.4
     */
    int markAllAsRead(Long userId);
    
    /**
     * 按类型标记用户消息为已读
     * 
     * @param userId 用户ID
     * @param type 通知类型
     * @return 标记的消息数量
     * Requirements: 3.4
     */
    int markAsReadByType(Long userId, NotificationType type);
    
    /**
     * 删除通知
     * 
     * @param notificationId 通知ID
     * @param userId 用户ID（用于权限验证）
     * @return 是否成功
     * Requirements: 3.5
     */
    boolean deleteNotification(Long notificationId, Long userId);
    
    // ==================== 辅助方法 ====================
    
    /**
     * 创建并发送通知（便捷方法）
     * 
     * @param receiverId 接收者ID
     * @param senderId 发送者ID（系统消息为null）
     * @param type 通知类型
     * @param title 标题
     * @param content 内容
     * @param relatedId 关联业务ID
     * Requirements: 2.1, 2.2, 3.1
     */
    void createAndSendNotification(Long receiverId, Long senderId, NotificationType type, 
                                   String title, String content, Long relatedId);
    
    /**
     * 向多个用户发送相同通知
     * 
     * @param receiverIds 接收者ID列表
     * @param senderId 发送者ID
     * @param type 通知类型
     * @param title 标题
     * @param content 内容
     * @param relatedId 关联业务ID
     * Requirements: 2.1, 2.2, 3.1
     */
    void sendToMultipleUsers(List<Long> receiverIds, Long senderId, NotificationType type,
                             String title, String content, Long relatedId);
}
