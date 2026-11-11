package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.entity.HealthConversationMessage;
import com.learning.silveragepass.repository.HealthConversationMessageRepository;
import com.learning.silveragepass.service.DataRetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据保留策略服务实现
 * Data retention policy service implementation
 * 
 * 实现定期删除超过90天的消息内容，同时保留情感分析记录用于统计
 * Implements scheduled deletion of message content older than 90 days
 * while preserving emotion analysis records for statistics
 */
@Slf4j
@Service
public class DataRetentionServiceImpl implements DataRetentionService {

    private final HealthConversationMessageRepository messageRepository;

    @Value("${app.data-retention.message-retention-days:90}")
    private int retentionPeriodDays;

    public DataRetentionServiceImpl(HealthConversationMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * 定期执行数据保留策略
     * Scheduled execution of data retention policy
     * 
     * 每天凌晨2点执行，删除超过保留期限的消息内容
     * Runs daily at 2 AM to delete message content older than retention period
     */
    @Scheduled(cron = "${app.data-retention.schedule:0 0 2 * * ?}")
    @Transactional
    public int executeRetentionPolicy() {
        log.info("开始执行数据保留策略 - Starting data retention policy execution");
        
        try {
            // 计算截止日期
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionPeriodDays);
            log.info("保留期限截止日期: {} ({}天前)", cutoffDate, retentionPeriodDays);
            
            // 查找超过保留期限的消息
            List<HealthConversationMessage> oldMessages = messageRepository.findByCreatedAtBefore(cutoffDate);
            int messageCount = oldMessages.size();
            
            if (messageCount == 0) {
                log.info("没有需要删除的消息");
                return 0;
            }
            
            log.info("找到 {} 条超过保留期限的消息", messageCount);
            
            // 删除消息内容，但保留记录结构和情感分析关联
            int deletedCount = 0;
            for (HealthConversationMessage message : oldMessages) {
                try {
                    // 将消息内容设置为null，保留其他元数据
                    // 这样可以保留情感分析记录的关联，用于统计
                    message.setContent(null);
                    messageRepository.save(message);
                    deletedCount++;
                } catch (Exception e) {
                    log.error("删除消息内容失败 - messageId: {}, error: {}", 
                             message.getMessageId(), e.getMessage());
                }
            }
            
            log.info("数据保留策略执行完成 - 成功删除 {} 条消息内容", deletedCount);
            log.info("保留了情感分析记录用于统计目的");
            
            return deletedCount;
            
        } catch (Exception e) {
            log.error("执行数据保留策略时发生错误", e);
            throw new RuntimeException("数据保留策略执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int getRetentionPeriodDays() {
        return retentionPeriodDays;
    }

    @Override
    public long countMessagesOlderThanRetentionPeriod() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionPeriodDays);
        List<HealthConversationMessage> oldMessages = messageRepository.findByCreatedAtBefore(cutoffDate);
        return oldMessages.stream()
                .filter(msg -> msg.getContent() != null)
                .count();
    }
}
