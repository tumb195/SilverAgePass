package com.learning.silveragepass.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learning.silveragepass.dto.notification.NotificationDTO;
import com.learning.silveragepass.service.OfflineMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * 离线消息服务实现类
 * 使用Redis List存储离线消息，支持消息过期
 */
@Slf4j
@Service
public class OfflineMessageServiceImpl implements OfflineMessageService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // 内存缓存，用于测试环境（无Redis时）
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<NotificationDTO>> memoryCache = new ConcurrentHashMap<>();
    
    // Redis key前缀
    private static final String OFFLINE_MESSAGE_PREFIX = "offline:msg:";
    
    // 离线消息最大数量
    @Value("${notification.offline.max-messages:100}")
    private int maxMessages;
    
    // 离线消息过期天数
    @Value("${notification.offline.expire-days:7}")
    private int expireDays;
    
    // JSON序列化器
    private final ObjectMapper objectMapper;
    
    public OfflineMessageServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * 获取用户离线消息的Redis key
     */
    private String getOfflineMessageKey(Long userId) {
        return OFFLINE_MESSAGE_PREFIX + userId;
    }

    @Override
    public void storeOfflineMessage(Long userId, NotificationDTO notification) {
        if (userId == null || notification == null) {
            log.warn("无法存储离线消息：userId或notification为空");
            return;
        }
        
        String key = getOfflineMessageKey(userId);
        
        if (redisTemplate != null) {
            try {
                // 序列化通知为JSON字符串
                String jsonMessage = objectMapper.writeValueAsString(notification);
                
                // 使用RPUSH添加到列表末尾
                redisTemplate.opsForList().rightPush(key, jsonMessage);
                
                // 限制列表长度，保留最新的maxMessages条消息
                Long size = redisTemplate.opsForList().size(key);
                if (size != null && size > maxMessages) {
                    // 移除最旧的消息
                    redisTemplate.opsForList().trim(key, size - maxMessages, -1);
                }
                
                // 设置过期时间
                redisTemplate.expire(key, expireDays, TimeUnit.DAYS);
                
                log.debug("离线消息已存储 - userId: {}, notificationId: {}", userId, notification.getId());
            } catch (JsonProcessingException e) {
                log.error("序列化离线消息失败 - userId: {}", userId, e);
            }
        } else {
            // 内存缓存模式
            memoryCache.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(notification);
            
            // 限制列表长度
            CopyOnWriteArrayList<NotificationDTO> messages = memoryCache.get(userId);
            while (messages.size() > maxMessages) {
                messages.remove(0);
            }
            
            log.debug("离线消息已存储到内存缓存 - userId: {}", userId);
        }
    }

    @Override
    public List<NotificationDTO> getAndClearOfflineMessages(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        String key = getOfflineMessageKey(userId);
        List<NotificationDTO> messages = new ArrayList<>();
        
        if (redisTemplate != null) {
            try {
                // 获取所有离线消息
                List<Object> rawMessages = redisTemplate.opsForList().range(key, 0, -1);
                
                if (rawMessages != null && !rawMessages.isEmpty()) {
                    for (Object rawMessage : rawMessages) {
                        try {
                            String jsonMessage = rawMessage.toString();
                            NotificationDTO notification = objectMapper.readValue(jsonMessage, NotificationDTO.class);
                            messages.add(notification);
                        } catch (JsonProcessingException e) {
                            log.error("反序列化离线消息失败", e);
                        }
                    }
                    
                    // 清除已获取的消息
                    redisTemplate.delete(key);
                    
                    log.info("已获取并清除离线消息 - userId: {}, count: {}", userId, messages.size());
                }
            } catch (Exception e) {
                log.error("获取离线消息失败 - userId: {}", userId, e);
            }
        } else {
            // 内存缓存模式
            CopyOnWriteArrayList<NotificationDTO> cachedMessages = memoryCache.remove(userId);
            if (cachedMessages != null) {
                messages.addAll(cachedMessages);
                log.debug("已从内存缓存获取并清除离线消息 - userId: {}, count: {}", userId, messages.size());
            }
        }
        
        return messages;
    }

    @Override
    public int getOfflineMessageCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        
        String key = getOfflineMessageKey(userId);
        
        if (redisTemplate != null) {
            Long size = redisTemplate.opsForList().size(key);
            return size != null ? size.intValue() : 0;
        } else {
            // 内存缓存模式
            CopyOnWriteArrayList<NotificationDTO> messages = memoryCache.get(userId);
            return messages != null ? messages.size() : 0;
        }
    }

    @Override
    public boolean hasOfflineMessages(Long userId) {
        return getOfflineMessageCount(userId) > 0;
    }
}
