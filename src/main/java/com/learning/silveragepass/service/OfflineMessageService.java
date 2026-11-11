package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.notification.NotificationDTO;

import java.util.List;

/**
 * 离线消息服务接口
 * 基于Redis存储离线用户的待推送消息
 */
public interface OfflineMessageService {
    
    /**
     * 存储离线消息到Redis队列
     * @param userId 用户ID
     * @param notification 通知消息
     */
    void storeOfflineMessage(Long userId, NotificationDTO notification);
    
    /**
     * 获取并清除用户的所有离线消息
     * @param userId 用户ID
     * @return 离线消息列表
     */
    List<NotificationDTO> getAndClearOfflineMessages(Long userId);
    
    /**
     * 获取用户离线消息数量
     * @param userId 用户ID
     * @return 离线消息数量
     */
    int getOfflineMessageCount(Long userId);
    
    /**
     * 检查用户是否有离线消息
     * @param userId 用户ID
     * @return 如果有离线消息返回true
     */
    boolean hasOfflineMessages(Long userId);
}
