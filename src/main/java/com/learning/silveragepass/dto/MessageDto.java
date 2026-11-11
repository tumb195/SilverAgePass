package com.learning.silveragepass.dto;

import com.learning.silveragepass.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private Long id;
    private String title;
    private String content;
    private MessageType type;
    private Long senderId;
    private String senderName; // 发送者名称，系统消息为"系统"
    private Long receiverId;
    private Boolean isRead;
    private Long relatedDemandId;
    private String relatedDemandTitle; // 关联需求的标题（如果有）
    private LocalDateTime createTime;
} 