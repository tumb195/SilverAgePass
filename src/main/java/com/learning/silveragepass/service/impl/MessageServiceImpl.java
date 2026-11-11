package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.MessageDto;
import com.learning.silveragepass.entity.Demand;
import com.learning.silveragepass.entity.Message;
import com.learning.silveragepass.entity.MessageType;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.DemandRepository;
import com.learning.silveragepass.repository.MessageRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.MessageService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DemandRepository demandRepository;

    @Override
    public Message sendMessage(Message message) {
        return messageRepository.save(message);
    }

    @Override
    public Message sendSystemMessage(String title, String content, Long receiverId) {
        Message message = new Message();
        message.setTitle(title);
        message.setContent(content);
        message.setType(MessageType.SYSTEM);
        message.setReceiverId(receiverId);
        return messageRepository.save(message);
    }

    @Override
    public Message sendAdminMessage(String title, String content, Long senderId, Long receiverId) {
        Message message = new Message();
        message.setTitle(title);
        message.setContent(content);
        message.setType(MessageType.ADMIN);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        return messageRepository.save(message);
    }

    @Override
    public Message sendDemandMessage(String title, String content, Long senderId, Long receiverId, Long demandId) {
        Message message = new Message();
        message.setTitle(title);
        message.setContent(content);
        message.setType(MessageType.DEMAND);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setRelatedDemandId(demandId);
        return messageRepository.save(message);
    }

    @Override
    public Message sendRelativeDemandMessage(Long demandId, Long userId) {
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResourceNotFoundException("需求不存在"));
        
        User publisher = userRepository.findById(demand.getPublisherId())
                .orElseThrow(() -> new ResourceNotFoundException("发布者不存在"));
        
        String title = "亲人需求通知";
        String content = String.format("您的亲人 %s 发布了一个新的需求：%s", 
                publisher.getRealName() != null ? publisher.getRealName() : publisher.getUsername(), 
                demand.getTitle());
        
        Message message = new Message();
        message.setTitle(title);
        message.setContent(content);
        message.setType(MessageType.RELATIVE);
        message.setSenderId(demand.getPublisherId());
        message.setReceiverId(userId);
        message.setRelatedDemandId(demandId);
        return messageRepository.save(message);
    }

    @Override
    public Page<MessageDto> getUserMessages(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findByReceiverIdOrderByCreateTimeDesc(userId, pageable);
        
        List<MessageDto> messageDtos = messagePage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return new PageImpl<>(messageDtos, pageable, messagePage.getTotalElements());
    }

    @Override
    public Page<MessageDto> getUserMessagesByType(Long userId, MessageType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findByReceiverIdAndTypeOrderByCreateTimeDesc(userId, type, pageable);
        
        List<MessageDto> messageDtos = messagePage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return new PageImpl<>(messageDtos, pageable, messagePage.getTotalElements());
    }

    @Override
    public Long getUnreadMessageCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markMessageAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("消息不存在"));
        message.setIsRead(true);
        messageRepository.save(message);
    }

    @Override
    @Transactional
    public void markMessagesAsRead(List<Long> messageIds) {
        if (messageIds != null && !messageIds.isEmpty()) {
            messageRepository.markMessagesAsRead(messageIds);
        }
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
    }

    @Override
    @Transactional
    public void deleteMessages(List<Long> messageIds) {
        if (messageIds != null && !messageIds.isEmpty()) {
            messageRepository.deleteByIdIn(messageIds);
        }
    }

    @Override
    public MessageDto getMessageDetail(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("消息不存在"));
        return convertToDto(message);
    }
    
    // 将Message实体转换为MessageDto
    private MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setTitle(message.getTitle());
        dto.setContent(message.getContent());
        dto.setType(message.getType());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setIsRead(message.getIsRead());
        dto.setRelatedDemandId(message.getRelatedDemandId());
        dto.setCreateTime(message.getCreateTime());
        
        // 设置发送者名称
        if (message.getSenderId() != null) {
            userRepository.findById(message.getSenderId()).ifPresent(sender -> 
                dto.setSenderName(sender.getRealName() != null ? sender.getRealName() : sender.getUsername()));
        } else if (message.getType() == MessageType.SYSTEM) {
            dto.setSenderName("系统");
        } else if (message.getType() == MessageType.ADMIN) {
            dto.setSenderName("管理员");
        }
        
        // 设置关联需求标题
        if (message.getRelatedDemandId() != null) {
            demandRepository.findById(message.getRelatedDemandId()).ifPresent(demand -> 
                dto.setRelatedDemandTitle(demand.getTitle()));
        }
        
        return dto;
    }
} 