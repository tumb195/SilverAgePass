package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.MessageDto;
import com.learning.silveragepass.entity.Message;
import com.learning.silveragepass.entity.MessageType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MessageService {
    
    // 发送消息
    Message sendMessage(Message message);
    
    // 发送系统消息
    Message sendSystemMessage(String title, String content, Long receiverId);
    
    // 发送管理员消息
    Message sendAdminMessage(String title, String content, Long senderId, Long receiverId);
    
    // 发送需求相关消息
    Message sendDemandMessage(String title, String content, Long senderId, Long receiverId, Long demandId);
    
    // 发送亲人消息（需求同步）
    Message sendRelativeDemandMessage(Long demandId, Long userId);
    
    // 获取用户消息列表（分页）
    Page<MessageDto> getUserMessages(Long userId, int page, int size);
    
    // 获取用户特定类型消息列表（分页）
    Page<MessageDto> getUserMessagesByType(Long userId, MessageType type, int page, int size);
    
    // 获取用户未读消息数量
    Long getUnreadMessageCount(Long userId);
    
    // 标记消息为已读
    void markMessageAsRead(Long messageId);
    
    // 批量标记消息为已读
    void markMessagesAsRead(List<Long> messageIds);
    
    // 删除消息
    void deleteMessage(Long messageId);
    
    // 批量删除消息
    void deleteMessages(List<Long> messageIds);
    
    // 获取消息详情
    MessageDto getMessageDetail(Long messageId);
} 