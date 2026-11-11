package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.health.MedicationReminderCreateDTO;
import com.learning.silveragepass.dto.health.MedicationReminderDTO;
import com.learning.silveragepass.dto.health.MedicationReminderUpdateDTO;

import java.util.List;

/**
 * 用药提醒服务接口
 * Requirements: 3.1, 3.4, 3.5, 3.6
 */
public interface MedicationReminderService {

    /**
     * 创建用药提醒
     * 
     * @param userId 提醒所属用户ID
     * @param creatorId 创建者ID（可能是亲属）
     * @param dto 用药提醒创建DTO
     * @return 创建的用药提醒DTO
     */
    MedicationReminderDTO createReminder(Long userId, Long creatorId, MedicationReminderCreateDTO dto);

    /**
     * 获取用户的所有活跃用药提醒
     * 
     * @param userId 用户ID
     * @return 活跃的用药提醒列表
     */
    List<MedicationReminderDTO> getReminders(Long userId);

    /**
     * 更新用药提醒
     * 
     * @param reminderId 提醒ID
     * @param currentUserId 当前用户ID（用于权限验证）
     * @param dto 用药提醒更新DTO
     * @return 更新后的用药提醒DTO
     */
    MedicationReminderDTO updateReminder(Long reminderId, Long currentUserId, MedicationReminderUpdateDTO dto);

    /**
     * 删除用药提醒（软删除，设置active=false）
     * 
     * @param reminderId 提醒ID
     * @param currentUserId 当前用户ID（用于权限验证）
     */
    void deleteReminder(Long reminderId, Long currentUserId);
}
