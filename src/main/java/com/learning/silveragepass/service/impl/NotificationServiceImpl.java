package com.learning.silveragepass.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.dto.notification.NotificationDTO;
import com.learning.silveragepass.dto.notification.UnreadCountDTO;
import com.learning.silveragepass.dto.notification.WebSocketMessage;
import com.learning.silveragepass.entity.Notification;
import com.learning.silveragepass.entity.NotificationType;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.repository.NotificationRepository;
import com.learning.silveragepass.service.NotificationService;
import com.learning.silveragepass.service.OfflineMessageService;
import com.learning.silveragepass.service.UserService;
import com.learning.silveragepass.websocket.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知服务实现类
 * 实现消息持久化、在线/离线推送、分页查询、已读状态管理等功能
 * 
 * Requirements: 2.1, 2.2, 3.1, 3.2, 3.3, 3.4, 3.5, 8.1, 8.2, 8.3
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Autowired
    private OfflineMessageService offlineMessageService;
    
    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 通知类型按分类分组
    private static final Map<String, List<NotificationType>> TYPE_BY_CATEGORY = new HashMap<>();
    
    static {
        TYPE_BY_CATEGORY.put("system", Arrays.asList(NotificationType.SYSTEM, NotificationType.ADMIN));
        TYPE_BY_CATEGORY.put("health", Arrays.asList(NotificationType.MEDICATION_REMINDER, NotificationType.FOLLOWUP_REMINDER));
        TYPE_BY_CATEGORY.put("demand", Arrays.asList(NotificationType.DEMAND_ACCEPTED, NotificationType.DEMAND_COMPLETED, 
                NotificationType.DEMAND_CANCELLED, NotificationType.DEMAND_RATED));
        TYPE_BY_CATEGORY.put("forum", Arrays.asList(NotificationType.FORUM_COMMENT, NotificationType.FORUM_REPLY, 
                NotificationType.FORUM_LIKE));
        TYPE_BY_CATEGORY.put("social", Arrays.asList(NotificationType.USER_FOLLOW, NotificationType.RELATIVE_BIND));
    }

    // ==================== 消息发送方法 ====================

    @Override
    @Transactional
    public void sendNotification(NotificationDTO notification) {
        if (notification == null || notification.getReceiverId() == null) {
            log.warn("无法发送通知：notification或receiverId为空");
            return;
        }
        
        // 1. 持久化到数据库
        Notification entity = saveNotification(notification);
        notification.setId(entity.getId());
        notification.setCreateTime(entity.getCreateTime());
        
        // 2. 填充发送者信息
        enrichSenderInfo(notification);
        
        // 3. 判断用户在线状态并推送
        Long receiverId = notification.getReceiverId();
        if (sessionManager.isUserOnline(receiverId)) {
            // 在线：通过WebSocket实时推送
            pushToUser(receiverId, notification);
            log.debug("实时推送通知给在线用户 - userId: {}, notificationId: {}", receiverId, notification.getId());
        } else {
            // 离线：存入Redis队列
            offlineMessageService.storeOfflineMessage(receiverId, notification);
            log.debug("存储离线消息 - userId: {}, notificationId: {}", receiverId, notification.getId());
        }
    }

    @Override
    @Transactional
    public void sendNotifications(List<NotificationDTO> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        
        for (NotificationDTO notification : notifications) {
            sendNotification(notification);
        }
    }

    @Override
    @Transactional
    public void sendToUser(Long userId, NotificationDTO notification) {
        if (userId == null || notification == null) {
            log.warn("无法发送通知：userId或notification为空");
            return;
        }
        
        notification.setReceiverId(userId);
        sendNotification(notification);
    }

    @Override
    @Transactional
    public void broadcast(NotificationDTO notification) {
        if (notification == null) {
            log.warn("无法广播通知：notification为空");
            return;
        }
        
        Set<Long> onlineUsers = sessionManager.getOnlineUsers();
        log.info("广播通知给 {} 个在线用户", onlineUsers.size());
        
        for (Long userId : onlineUsers) {
            NotificationDTO copy = copyNotification(notification);
            copy.setReceiverId(userId);
            sendNotification(copy);
        }
    }

    @Override
    public void pushOfflineMessages(Long userId) {
        if (userId == null) {
            return;
        }
        
        List<NotificationDTO> offlineMessages = offlineMessageService.getAndClearOfflineMessages(userId);
        if (offlineMessages.isEmpty()) {
            log.debug("用户 {} 没有离线消息", userId);
            return;
        }
        
        log.info("推送 {} 条离线消息给用户 {}", offlineMessages.size(), userId);
        
        // 批量推送离线消息
        if (messagingTemplate != null) {
            WebSocketMessage<List<NotificationDTO>> message = WebSocketMessage.<List<NotificationDTO>>builder()
                    .type("offline_messages")
                    .payload(offlineMessages)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/notifications",
                    message
            );
        }
        
        // 推送最新未读数
        pushUnreadCount(userId);
    }

    // ==================== 消息查询方法 ====================

    @Override
    public Page<NotificationDTO> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByReceiverIdOrderByCreateTimeDesc(userId, pageable);
        return notifications.map(this::convertToDTO);
    }

    @Override
    public Page<NotificationDTO> getUserNotificationsByType(Long userId, NotificationType type, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByReceiverIdAndTypeOrderByCreateTimeDesc(userId, type, pageable);
        return notifications.map(this::convertToDTO);
    }

    @Override
    public Page<NotificationDTO> getUserNotificationsByCategory(Long userId, String category, Pageable pageable) {
        List<NotificationType> types = TYPE_BY_CATEGORY.get(category);
        if (types == null || types.isEmpty()) {
            log.warn("未知的通知分类: {}", category);
            return Page.empty(pageable);
        }
        
        Page<Notification> notifications = notificationRepository.findByReceiverIdAndTypeInOrderByCreateTimeDesc(userId, types, pageable);
        return notifications.map(this::convertToDTO);
    }

    @Override
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreateTimeDesc(userId);
        return notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ==================== 未读统计方法 ====================

    @Override
    public UnreadCountDTO getUnreadCount(Long userId) {
        // 获取总未读数
        Long total = notificationRepository.countByReceiverIdAndIsReadFalse(userId);
        
        // 按类型统计未读数
        List<Object[]> typeCountList = notificationRepository.countUnreadByReceiverIdGroupByType(userId);
        Map<NotificationType, Integer> byType = new EnumMap<>(NotificationType.class);
        Map<String, Integer> byCategory = new HashMap<>();
        
        for (Object[] row : typeCountList) {
            NotificationType type = (NotificationType) row[0];
            Integer count = ((Long) row[1]).intValue();
            byType.put(type, count);
            
            // 按分类累加
            String category = type.getCategory();
            byCategory.merge(category, count, Integer::sum);
        }
        
        return UnreadCountDTO.builder()
                .total(total.intValue())
                .byType(byType)
                .byCategory(byCategory)
                .build();
    }

    // ==================== 状态管理方法 ====================

    @Override
    @Transactional
    public boolean markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated > 0) {
            // 推送最新未读数
            pushUnreadCount(userId);
            log.debug("标记通知已读 - notificationId: {}, userId: {}", notificationId, userId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsRead(userId);
        if (updated > 0) {
            // 推送最新未读数
            pushUnreadCount(userId);
            log.info("标记所有通知已读 - userId: {}, count: {}", userId, updated);
        }
        return updated;
    }

    @Override
    @Transactional
    public int markAsReadByType(Long userId, NotificationType type) {
        int updated = notificationRepository.markAsReadByType(userId, type);
        if (updated > 0) {
            // 推送最新未读数
            pushUnreadCount(userId);
            log.debug("按类型标记通知已读 - userId: {}, type: {}, count: {}", userId, type, updated);
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean deleteNotification(Long notificationId, Long userId) {
        int deleted = notificationRepository.deleteByIdAndReceiverId(notificationId, userId);
        if (deleted > 0) {
            log.debug("删除通知 - notificationId: {}, userId: {}", notificationId, userId);
            return true;
        }
        return false;
    }

    // ==================== 辅助方法 ====================

    @Override
    @Transactional
    public void createAndSendNotification(Long receiverId, Long senderId, NotificationType type,
                                          String title, String content, Long relatedId) {
        NotificationDTO notification = NotificationDTO.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .type(type)
                .title(title)
                .content(content)
                .relatedId(relatedId)
                .isRead(false)
                .priority(0)
                .build();
        
        sendNotification(notification);
    }

    @Override
    @Transactional
    public void sendToMultipleUsers(List<Long> receiverIds, Long senderId, NotificationType type,
                                    String title, String content, Long relatedId) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }
        
        for (Long receiverId : receiverIds) {
            createAndSendNotification(receiverId, senderId, type, title, content, relatedId);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 保存通知到数据库
     */
    private Notification saveNotification(NotificationDTO dto) {
        Notification entity = new Notification();
        entity.setReceiverId(dto.getReceiverId());
        entity.setSenderId(dto.getSenderId());
        entity.setType(dto.getType());
        entity.setTitle(dto.getTitle());
        entity.setContent(dto.getContent());
        entity.setRelatedId(dto.getRelatedId());
        entity.setIsRead(false);
        entity.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        
        // 序列化extraData
        if (dto.getExtraData() != null && !dto.getExtraData().isEmpty()) {
            try {
                entity.setExtraData(objectMapper.writeValueAsString(dto.getExtraData()));
            } catch (JsonProcessingException e) {
                log.error("序列化extraData失败", e);
            }
        }
        
        return notificationRepository.save(entity);
    }

    /**
     * 填充发送者信息
     */
    private void enrichSenderInfo(NotificationDTO notification) {
        if (notification.getSenderId() != null) {
            try {
                User sender = userService.getUserById(notification.getSenderId());
                if (sender != null) {
                    notification.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername());
                    notification.setSenderAvatar(sender.getAvatar());
                }
            } catch (Exception e) {
                log.warn("获取发送者信息失败 - senderId: {}", notification.getSenderId());
            }
        }
        
        // 填充类型描述和分类
        if (notification.getType() != null) {
            notification.setTypeDescription(notification.getType().getDescription());
            notification.setCategory(notification.getType().getCategory());
        }
    }

    /**
     * 通过WebSocket推送通知给用户
     */
    private void pushToUser(Long userId, NotificationDTO notification) {
        if (messagingTemplate == null) {
            log.warn("SimpMessagingTemplate未配置，无法推送WebSocket消息");
            return;
        }
        
        WebSocketMessage<NotificationDTO> message = WebSocketMessage.notification(notification);
        
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notifications",
                message
        );
    }

    /**
     * 推送未读数更新给用户
     */
    private void pushUnreadCount(Long userId) {
        if (messagingTemplate == null || !sessionManager.isUserOnline(userId)) {
            return;
        }
        
        UnreadCountDTO unreadCount = getUnreadCount(userId);
        WebSocketMessage<UnreadCountDTO> message = WebSocketMessage.unreadCount(unreadCount);
        
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notifications",
                message
        );
    }

    /**
     * 转换实体为DTO
     */
    private NotificationDTO convertToDTO(Notification entity) {
        NotificationDTO dto = NotificationDTO.builder()
                .id(entity.getId())
                .receiverId(entity.getReceiverId())
                .senderId(entity.getSenderId())
                .type(entity.getType())
                .title(entity.getTitle())
                .content(entity.getContent())
                .relatedId(entity.getRelatedId())
                .isRead(entity.getIsRead())
                .priority(entity.getPriority())
                .createTime(entity.getCreateTime())
                .readTime(entity.getReadTime())
                .build();
        
        // 填充类型描述和分类
        if (entity.getType() != null) {
            dto.setTypeDescription(entity.getType().getDescription());
            dto.setCategory(entity.getType().getCategory());
        }
        
        // 反序列化extraData
        if (entity.getExtraData() != null && !entity.getExtraData().isEmpty()) {
            try {
                Map<String, Object> extraData = objectMapper.readValue(
                        entity.getExtraData(), 
                        new TypeReference<Map<String, Object>>() {}
                );
                dto.setExtraData(extraData);
            } catch (JsonProcessingException e) {
                log.error("反序列化extraData失败", e);
            }
        }
        
        // 填充发送者信息
        enrichSenderInfo(dto);
        
        return dto;
    }

    /**
     * 复制通知对象
     */
    private NotificationDTO copyNotification(NotificationDTO source) {
        return NotificationDTO.builder()
                .senderId(source.getSenderId())
                .senderName(source.getSenderName())
                .senderAvatar(source.getSenderAvatar())
                .type(source.getType())
                .typeDescription(source.getTypeDescription())
                .category(source.getCategory())
                .title(source.getTitle())
                .content(source.getContent())
                .extraData(source.getExtraData() != null ? new HashMap<>(source.getExtraData()) : null)
                .relatedId(source.getRelatedId())
                .isRead(false)
                .priority(source.getPriority())
                .build();
    }
}
