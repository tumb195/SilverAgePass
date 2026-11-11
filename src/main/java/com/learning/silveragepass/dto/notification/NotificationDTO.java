package com.learning.silveragepass.dto.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.learning.silveragepass.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知传输对象
 * 用于API响应和WebSocket消息推送
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    
    private Long id;
    
    private Long receiverId;
    
    private Long senderId;
    
    private String senderName;
    
    private String senderAvatar;
    
    private NotificationType type;
    
    private String typeDescription;
    
    private String category;
    
    private String title;
    
    private String content;
    
    private Map<String, Object> extraData;
    
    private Long relatedId;
    
    private Boolean isRead;
    
    private Integer priority;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;
}
