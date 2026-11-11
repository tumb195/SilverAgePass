package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.Message;
import com.learning.silveragepass.entity.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    // 查询用户的所有消息（分页）
    Page<Message> findByReceiverIdOrderByCreateTimeDesc(Long receiverId, Pageable pageable);
    
    // 查询用户的未读消息数量
    Long countByReceiverIdAndIsReadFalse(Long receiverId);
    
    // 查询用户的特定类型消息（分页）
    Page<Message> findByReceiverIdAndTypeOrderByCreateTimeDesc(Long receiverId, MessageType type, Pageable pageable);
    
    // 查询与特定需求相关的消息
    List<Message> findByRelatedDemandIdOrderByCreateTimeDesc(Long demandId);
    
    // 批量更新消息为已读状态
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.id IN :ids")
    void markMessagesAsRead(List<Long> ids);
    
    // 批量删除消息
    @Modifying
    void deleteByIdIn(List<Long> ids);
} 