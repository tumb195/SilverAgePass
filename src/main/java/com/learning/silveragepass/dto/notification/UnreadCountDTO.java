package com.learning.silveragepass.dto.notification;

import com.learning.silveragepass.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 未读消息统计对象
 * 用于返回用户的未读消息数量统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountDTO {
    
    /**
     * 未读消息总数
     */
    private int total;
    
    /**
     * 按分类统计的未读数
     * key: category (system, health, demand, forum, social)
     * value: 该分类的未读数
     */
    private Map<String, Integer> byCategory;
    
    /**
     * 按类型统计的未读数
     * key: NotificationType
     * value: 该类型的未读数
     */
    private Map<NotificationType, Integer> byType;
}
