package com.learning.silveragepass.service;

import com.learning.silveragepass.entity.NotificationType;

/**
 * 通知事件发布器接口
 * 提供统一的业务事件通知发布方法
 * 
 * Requirements: 4.2, 4.3, 4.4, 5.3, 5.4, 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3, 7.4
 */
public interface NotificationEventPublisher {

    // ==================== 用药提醒通知 ====================
    
    /**
     * 发送用药提醒创建通知
     * 当亲属为老人创建用药提醒时，同时通知老人和创建者
     * 
     * @param reminderId 提醒ID
     * @param userId 老人用户ID
     * @param creatorId 创建者ID
     * @param medicationName 药品名称
     * Requirements: 4.2
     */
    void publishMedicationReminderCreated(Long reminderId, Long userId, Long creatorId, String medicationName);
    
    /**
     * 发送用药提醒更新通知
     * 
     * @param reminderId 提醒ID
     * @param userId 老人用户ID
     * @param updaterId 更新者ID
     * @param medicationName 药品名称
     * Requirements: 4.3
     */
    void publishMedicationReminderUpdated(Long reminderId, Long userId, Long updaterId, String medicationName);
    
    /**
     * 发送用药提醒删除通知
     * 
     * @param reminderId 提醒ID
     * @param userId 老人用户ID
     * @param deleterId 删除者ID
     * @param medicationName 药品名称
     * Requirements: 4.4
     */
    void publishMedicationReminderDeleted(Long reminderId, Long userId, Long deleterId, String medicationName);

    // ==================== 复诊提醒通知 ====================
    
    /**
     * 发送复诊提醒创建通知
     * 当亲属为老人创建复诊提醒时，同时通知老人和创建者
     * 
     * @param reminderId 提醒ID
     * @param userId 老人用户ID
     * @param creatorId 创建者ID
     * @param hospitalName 医院名称
     * @param appointmentDate 预约日期
     * Requirements: 5.3
     */
    void publishFollowupReminderCreated(Long reminderId, Long userId, Long creatorId, 
                                        String hospitalName, String appointmentDate);
    
    /**
     * 发送复诊提醒状态变更通知
     * 
     * @param reminderId 提醒ID
     * @param userId 老人用户ID
     * @param operatorId 操作者ID
     * @param hospitalName 医院名称
     * @param newStatus 新状态
     * Requirements: 5.4
     */
    void publishFollowupReminderStatusChanged(Long reminderId, Long userId, Long operatorId,
                                              String hospitalName, String newStatus);

    // ==================== 需求状态变更通知 ====================
    
    /**
     * 发送需求被接单通知
     * 
     * @param demandId 需求ID
     * @param publisherId 发布者ID
     * @param volunteerId 志愿者ID
     * @param demandTitle 需求标题
     * Requirements: 6.1
     */
    void publishDemandAccepted(Long demandId, Long publisherId, Long volunteerId, String demandTitle);
    
    /**
     * 发送服务完成通知
     * 
     * @param demandId 需求ID
     * @param publisherId 发布者ID
     * @param volunteerId 志愿者ID
     * @param demandTitle 需求标题
     * Requirements: 6.2
     */
    void publishDemandCompleted(Long demandId, Long publisherId, Long volunteerId, String demandTitle);
    
    /**
     * 发送需求取消通知
     * 
     * @param demandId 需求ID
     * @param publisherId 发布者ID
     * @param volunteerId 志愿者ID（可能为null）
     * @param demandTitle 需求标题
     * @param cancelledBy 取消者ID
     * Requirements: 6.3
     */
    void publishDemandCancelled(Long demandId, Long publisherId, Long volunteerId, 
                                String demandTitle, Long cancelledBy);
    
    /**
     * 发送评价完成通知
     * 
     * @param demandId 需求ID
     * @param volunteerId 志愿者ID
     * @param publisherId 发布者ID
     * @param demandTitle 需求标题
     * @param rating 评分
     * Requirements: 6.4
     */
    void publishDemandRated(Long demandId, Long volunteerId, Long publisherId, 
                            String demandTitle, Integer rating);

    // ==================== 论坛互动通知 ====================
    
    /**
     * 发送帖子评论通知
     * 
     * @param postId 帖子ID
     * @param postAuthorId 帖子作者ID
     * @param commenterId 评论者ID
     * @param postTitle 帖子标题
     * @param commentContent 评论内容摘要
     * Requirements: 7.1
     */
    void publishForumComment(Long postId, Long postAuthorId, Long commenterId, 
                             String postTitle, String commentContent);
    
    /**
     * 发送评论回复通知
     * 
     * @param commentId 评论ID
     * @param replyToUserId 被回复用户ID
     * @param replierId 回复者ID
     * @param postTitle 帖子标题
     * @param replyContent 回复内容摘要
     * Requirements: 7.2
     */
    void publishForumReply(Long commentId, Long replyToUserId, Long replierId, 
                           String postTitle, String replyContent);
    
    /**
     * 发送点赞通知
     * 
     * @param targetId 目标ID（帖子ID或评论ID）
     * @param targetOwnerId 目标所有者ID
     * @param likerId 点赞者ID
     * @param targetType 目标类型（post/comment）
     * @param targetTitle 目标标题或内容摘要
     * Requirements: 7.3
     */
    void publishForumLike(Long targetId, Long targetOwnerId, Long likerId, 
                          String targetType, String targetTitle);

    // ==================== 社交通知 ====================
    
    /**
     * 发送关注通知
     * 
     * @param followedId 被关注者ID
     * @param followerId 关注者ID
     * Requirements: 7.4
     */
    void publishUserFollow(Long followedId, Long followerId);
}
