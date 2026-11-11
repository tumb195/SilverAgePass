package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.entity.NotificationType;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.service.NotificationEventPublisher;
import com.learning.silveragepass.service.NotificationService;
import com.learning.silveragepass.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知事件发布器实现类
 * 负责将业务事件转换为通知并发送
 * 
 * Requirements: 4.2, 4.3, 4.4, 5.3, 5.4, 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3, 7.4
 */
@Slf4j
@Service
public class NotificationEventPublisherImpl implements NotificationEventPublisher {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserService userService;

    // ==================== 用药提醒通知 ====================

    @Override
    @Async
    public void publishMedicationReminderCreated(Long reminderId, Long userId, Long creatorId, String medicationName) {
        log.info("发布用药提醒创建通知 - reminderId: {}, userId: {}, creatorId: {}", reminderId, userId, creatorId);
        
        String creatorName = getUserName(creatorId);
        
        // 如果是亲属创建的，通知老人
        if (!userId.equals(creatorId)) {
            notificationService.createAndSendNotification(
                    userId,
                    creatorId,
                    NotificationType.MEDICATION_REMINDER,
                    "新增用药提醒",
                    String.format("%s为您创建了用药提醒：%s", creatorName, medicationName),
                    reminderId
            );
        }
        
        // 通知创建者（确认创建成功）
        if (!userId.equals(creatorId)) {
            String userName = getUserName(userId);
            notificationService.createAndSendNotification(
                    creatorId,
                    null,
                    NotificationType.MEDICATION_REMINDER,
                    "用药提醒创建成功",
                    String.format("您已成功为%s创建用药提醒：%s", userName, medicationName),
                    reminderId
            );
        }
    }

    @Override
    @Async
    public void publishMedicationReminderUpdated(Long reminderId, Long userId, Long updaterId, String medicationName) {
        log.info("发布用药提醒更新通知 - reminderId: {}, userId: {}, updaterId: {}", reminderId, userId, updaterId);
        
        String updaterName = getUserName(updaterId);
        
        // 如果是亲属更新的，通知老人
        if (!userId.equals(updaterId)) {
            notificationService.createAndSendNotification(
                    userId,
                    updaterId,
                    NotificationType.MEDICATION_REMINDER,
                    "用药提醒已更新",
                    String.format("%s更新了您的用药提醒：%s", updaterName, medicationName),
                    reminderId
            );
        }
    }

    @Override
    @Async
    public void publishMedicationReminderDeleted(Long reminderId, Long userId, Long deleterId, String medicationName) {
        log.info("发布用药提醒删除通知 - reminderId: {}, userId: {}, deleterId: {}", reminderId, userId, deleterId);
        
        String deleterName = getUserName(deleterId);
        
        // 如果是亲属删除的，通知老人
        if (!userId.equals(deleterId)) {
            notificationService.createAndSendNotification(
                    userId,
                    deleterId,
                    NotificationType.MEDICATION_REMINDER,
                    "用药提醒已取消",
                    String.format("%s取消了您的用药提醒：%s", deleterName, medicationName),
                    reminderId
            );
        }
    }

    // ==================== 复诊提醒通知 ====================

    @Override
    @Async
    public void publishFollowupReminderCreated(Long reminderId, Long userId, Long creatorId,
                                               String hospitalName, String appointmentDate) {
        log.info("发布复诊提醒创建通知 - reminderId: {}, userId: {}, creatorId: {}", reminderId, userId, creatorId);
        
        String creatorName = getUserName(creatorId);
        
        // 如果是亲属创建的，通知老人
        if (!userId.equals(creatorId)) {
            notificationService.createAndSendNotification(
                    userId,
                    creatorId,
                    NotificationType.FOLLOWUP_REMINDER,
                    "新增复诊提醒",
                    String.format("%s为您创建了复诊提醒：%s %s", creatorName, hospitalName, appointmentDate),
                    reminderId
            );
        }
        
        // 通知创建者（确认创建成功）
        if (!userId.equals(creatorId)) {
            String userName = getUserName(userId);
            notificationService.createAndSendNotification(
                    creatorId,
                    null,
                    NotificationType.FOLLOWUP_REMINDER,
                    "复诊提醒创建成功",
                    String.format("您已成功为%s创建复诊提醒：%s %s", userName, hospitalName, appointmentDate),
                    reminderId
            );
        }
    }

    @Override
    @Async
    public void publishFollowupReminderStatusChanged(Long reminderId, Long userId, Long operatorId,
                                                     String hospitalName, String newStatus) {
        log.info("发布复诊提醒状态变更通知 - reminderId: {}, userId: {}, newStatus: {}", reminderId, userId, newStatus);
        
        String operatorName = getUserName(operatorId);
        String statusText = getStatusText(newStatus);
        
        // 如果是亲属操作的，通知老人
        if (!userId.equals(operatorId)) {
            notificationService.createAndSendNotification(
                    userId,
                    operatorId,
                    NotificationType.FOLLOWUP_REMINDER,
                    "复诊提醒状态更新",
                    String.format("%s将您的复诊提醒（%s）标记为%s", operatorName, hospitalName, statusText),
                    reminderId
            );
        }
    }

    // ==================== 需求状态变更通知 ====================

    @Override
    @Async
    public void publishDemandAccepted(Long demandId, Long publisherId, Long volunteerId, String demandTitle) {
        log.info("发布需求被接单通知 - demandId: {}, publisherId: {}, volunteerId: {}", demandId, publisherId, volunteerId);
        
        String volunteerName = getUserName(volunteerId);
        
        // 通知发布者
        notificationService.createAndSendNotification(
                publisherId,
                volunteerId,
                NotificationType.DEMAND_ACCEPTED,
                "需求已被接单",
                String.format("志愿者%s已接单您的需求：%s", volunteerName, demandTitle),
                demandId
        );
    }

    @Override
    @Async
    public void publishDemandCompleted(Long demandId, Long publisherId, Long volunteerId, String demandTitle) {
        log.info("发布服务完成通知 - demandId: {}, publisherId: {}, volunteerId: {}", demandId, publisherId, volunteerId);
        
        String volunteerName = getUserName(volunteerId);
        
        // 通知发布者
        notificationService.createAndSendNotification(
                publisherId,
                volunteerId,
                NotificationType.DEMAND_COMPLETED,
                "服务已完成",
                String.format("志愿者%s已完成您的需求：%s，请及时评价", volunteerName, demandTitle),
                demandId
        );
    }

    @Override
    @Async
    public void publishDemandCancelled(Long demandId, Long publisherId, Long volunteerId,
                                       String demandTitle, Long cancelledBy) {
        log.info("发布需求取消通知 - demandId: {}, publisherId: {}, volunteerId: {}, cancelledBy: {}", 
                demandId, publisherId, volunteerId, cancelledBy);
        
        String cancellerName = getUserName(cancelledBy);
        List<Long> notifyUserIds = new ArrayList<>();
        
        // 确定需要通知的用户
        if (!publisherId.equals(cancelledBy)) {
            notifyUserIds.add(publisherId);
        }
        if (volunteerId != null && !volunteerId.equals(cancelledBy)) {
            notifyUserIds.add(volunteerId);
        }
        
        // 发送通知
        for (Long userId : notifyUserIds) {
            notificationService.createAndSendNotification(
                    userId,
                    cancelledBy,
                    NotificationType.DEMAND_CANCELLED,
                    "需求已取消",
                    String.format("%s取消了需求：%s", cancellerName, demandTitle),
                    demandId
            );
        }
    }

    @Override
    @Async
    public void publishDemandRated(Long demandId, Long volunteerId, Long publisherId,
                                   String demandTitle, Integer rating) {
        log.info("发布评价完成通知 - demandId: {}, volunteerId: {}, rating: {}", demandId, volunteerId, rating);
        
        String publisherName = getUserName(publisherId);
        String ratingStars = "★".repeat(rating) + "☆".repeat(5 - rating);
        
        // 通知志愿者
        notificationService.createAndSendNotification(
                volunteerId,
                publisherId,
                NotificationType.DEMAND_RATED,
                "收到新评价",
                String.format("%s对您完成的需求「%s」给出了评价：%s", publisherName, demandTitle, ratingStars),
                demandId
        );
    }

    // ==================== 论坛互动通知 ====================

    @Override
    @Async
    public void publishForumComment(Long postId, Long postAuthorId, Long commenterId,
                                    String postTitle, String commentContent) {
        log.info("发布帖子评论通知 - postId: {}, postAuthorId: {}, commenterId: {}", postId, postAuthorId, commenterId);
        
        // 不通知自己
        if (postAuthorId.equals(commenterId)) {
            return;
        }
        
        String commenterName = getUserName(commenterId);
        String contentPreview = truncateContent(commentContent, 50);
        
        notificationService.createAndSendNotification(
                postAuthorId,
                commenterId,
                NotificationType.FORUM_COMMENT,
                "您的帖子收到新评论",
                String.format("%s评论了您的帖子「%s」：%s", commenterName, postTitle, contentPreview),
                postId
        );
    }

    @Override
    @Async
    public void publishForumReply(Long commentId, Long replyToUserId, Long replierId,
                                  String postTitle, String replyContent) {
        log.info("发布评论回复通知 - commentId: {}, replyToUserId: {}, replierId: {}", commentId, replyToUserId, replierId);
        
        // 不通知自己
        if (replyToUserId.equals(replierId)) {
            return;
        }
        
        String replierName = getUserName(replierId);
        String contentPreview = truncateContent(replyContent, 50);
        
        notificationService.createAndSendNotification(
                replyToUserId,
                replierId,
                NotificationType.FORUM_REPLY,
                "您的评论收到新回复",
                String.format("%s在帖子「%s」下回复了您：%s", replierName, postTitle, contentPreview),
                commentId
        );
    }

    @Override
    @Async
    public void publishForumLike(Long targetId, Long targetOwnerId, Long likerId,
                                 String targetType, String targetTitle) {
        log.info("发布点赞通知 - targetId: {}, targetOwnerId: {}, likerId: {}, targetType: {}", 
                targetId, targetOwnerId, likerId, targetType);
        
        // 不通知自己
        if (targetOwnerId.equals(likerId)) {
            return;
        }
        
        String likerName = getUserName(likerId);
        String targetTypeText = "post".equals(targetType) ? "帖子" : "评论";
        String titlePreview = truncateContent(targetTitle, 30);
        
        notificationService.createAndSendNotification(
                targetOwnerId,
                likerId,
                NotificationType.FORUM_LIKE,
                "收到新点赞",
                String.format("%s点赞了您的%s「%s」", likerName, targetTypeText, titlePreview),
                targetId
        );
    }

    // ==================== 社交通知 ====================

    @Override
    @Async
    public void publishUserFollow(Long followedId, Long followerId) {
        log.info("发布关注通知 - followedId: {}, followerId: {}", followedId, followerId);
        
        String followerName = getUserName(followerId);
        
        notificationService.createAndSendNotification(
                followedId,
                followerId,
                NotificationType.USER_FOLLOW,
                "新增粉丝",
                String.format("%s关注了您", followerName),
                followerId
        );
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取用户名称
     */
    private String getUserName(Long userId) {
        if (userId == null) {
            return "系统";
        }
        try {
            User user = userService.getUserById(userId);
            if (user != null) {
                return user.getRealName() != null ? user.getRealName() : user.getUsername();
            }
        } catch (Exception e) {
            log.warn("获取用户名称失败 - userId: {}", userId);
        }
        return "用户" + userId;
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(String status) {
        if (status == null) {
            return "未知状态";
        }
        switch (status.toUpperCase()) {
            case "COMPLETED":
                return "已完成";
            case "CANCELLED":
                return "已取消";
            case "PENDING":
                return "待处理";
            default:
                return status;
        }
    }

    /**
     * 截断内容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
