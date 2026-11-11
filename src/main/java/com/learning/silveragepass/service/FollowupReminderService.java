package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.health.FollowupReminderCreateDTO;
import com.learning.silveragepass.dto.health.FollowupReminderDTO;
import com.learning.silveragepass.dto.health.FollowupReminderUpdateDTO;
import com.learning.silveragepass.entity.ReminderStatus;

import java.util.List;

/**
 * 复诊提醒服务接口
 * Requirements: 1.1, 2.1, 3.1, 3.2, 3.3, 3.4, 4.1
 */
public interface FollowupReminderService {

    /**
     * 创建复诊提醒
     * 
     * @param userId 提醒所属用户ID
     * @param creatorId 创建者ID（可能是亲属）
     * @param dto 复诊提醒创建DTO
     * @return 创建的复诊提醒DTO
     * Requirements: 1.1, 1.3
     */
    FollowupReminderDTO createReminder(Long userId, Long creatorId, FollowupReminderCreateDTO dto);

    /**
     * 获取用户的复诊提醒列表
     * 
     * @param userId 用户ID
     * @param status 可选的状态过滤
     * @return 复诊提醒列表，按复诊日期和时间升序排列
     * Requirements: 2.1, 2.2
     */
    List<FollowupReminderDTO> getReminders(Long userId, ReminderStatus status);

    /**
     * 获取即将到来的复诊提醒（7天内，PENDING状态）
     * 
     * @param userId 用户ID
     * @return 即将到来的复诊提醒列表
     * Requirements: 4.1
     */
    List<FollowupReminderDTO> getUpcomingReminders(Long userId);

    /**
     * 更新复诊提醒
     * 
     * @param reminderId 提醒ID
     * @param currentUserId 当前用户ID（用于权限验证）
     * @param dto 复诊提醒更新DTO
     * @return 更新后的复诊提醒DTO
     * Requirements: 3.1
     */
    FollowupReminderDTO updateReminder(Long reminderId, Long currentUserId, FollowupReminderUpdateDTO dto);

    /**
     * 标记复诊提醒为已完成
     * 
     * @param reminderId 提醒ID
     * @param currentUserId 当前用户ID（用于权限验证）
     * @return 更新后的复诊提醒DTO
     * Requirements: 3.2
     */
    FollowupReminderDTO markAsCompleted(Long reminderId, Long currentUserId);

    /**
     * 取消复诊提醒
     * 
     * @param reminderId 提醒ID
     * @param currentUserId 当前用户ID（用于权限验证）
     * @return 更新后的复诊提醒DTO
     * Requirements: 3.3
     */
    FollowupReminderDTO cancelReminder(Long reminderId, Long currentUserId);

    /**
     * 删除复诊提醒（硬删除）
     * 
     * @param reminderId 提醒ID
     * @param currentUserId 当前用户ID（用于权限验证）
     * Requirements: 3.4
     */
    void deleteReminder(Long reminderId, Long currentUserId);
}
