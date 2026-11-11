package com.learning.silveragepass.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.dto.health.MedicationReminderCreateDTO;
import com.learning.silveragepass.dto.health.MedicationReminderDTO;
import com.learning.silveragepass.dto.health.MedicationReminderUpdateDTO;
import com.learning.silveragepass.entity.MedicationReminder;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.MedicationReminderRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.HealthAuthorizationService;
import com.learning.silveragepass.service.MedicationReminderService;
import com.learning.silveragepass.service.NotificationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用药提醒服务实现
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 4.2, 4.3, 4.4
 */
@Service
public class MedicationReminderServiceImpl implements MedicationReminderService {

    @Autowired
    private MedicationReminderRepository medicationReminderRepository;

    @Autowired
    private HealthAuthorizationService healthAuthorizationService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public MedicationReminderDTO createReminder(Long userId, Long creatorId, MedicationReminderCreateDTO dto) {
        // 验证亲属权限（如果创建者不是用户本人）
        if (!userId.equals(creatorId)) {
            if (!healthAuthorizationService.canAccessUserHealthData(creatorId, userId)) {
                throw new BusinessException("UNAUTHORIZED", "无权访问该用户的健康数据");
            }
        }

        // 创建用药提醒
        MedicationReminder reminder = new MedicationReminder();
        reminder.setUserId(userId);
        reminder.setCreatorId(creatorId);
        reminder.setMedicationName(dto.getMedicationName());
        reminder.setDosage(dto.getDosage());
        reminder.setReminderTimes(convertListToJson(dto.getReminderTimes()));
        reminder.setFrequency(dto.getFrequency());
        reminder.setStartDate(dto.getStartDate());
        reminder.setEndDate(dto.getEndDate());
        reminder.setNote(dto.getNote());
        reminder.setActive(true);

        MedicationReminder savedReminder = medicationReminderRepository.save(reminder);
        
        // 发送通知 - Requirements: 4.2
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishMedicationReminderCreated(
                    savedReminder.getId(),
                    userId,
                    creatorId,
                    dto.getMedicationName()
            );
        }
        
        return convertToDTO(savedReminder);
    }


    @Override
    public List<MedicationReminderDTO> getReminders(Long userId) {
        // 获取用户的所有活跃用药提醒（包括自己创建的和亲属创建的）
        List<MedicationReminder> reminders = medicationReminderRepository.findByUserIdAndActiveTrue(userId);
        return reminders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MedicationReminderDTO updateReminder(Long reminderId, Long currentUserId, MedicationReminderUpdateDTO dto) {
        MedicationReminder reminder = medicationReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("记录不存在"));

        // 验证权限：只有记录所属用户或创建者可以更新
        if (!reminder.getUserId().equals(currentUserId) && !reminder.getCreatorId().equals(currentUserId)) {
            // 检查是否是已接受的亲属
            if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, reminder.getUserId())) {
                throw new BusinessException("UNAUTHORIZED", "无权修改该记录");
            }
        }

        // 更新字段（只更新非空字段）
        if (dto.getMedicationName() != null) {
            reminder.setMedicationName(dto.getMedicationName());
        }
        if (dto.getDosage() != null) {
            reminder.setDosage(dto.getDosage());
        }
        if (dto.getReminderTimes() != null && !dto.getReminderTimes().isEmpty()) {
            reminder.setReminderTimes(convertListToJson(dto.getReminderTimes()));
        }
        if (dto.getFrequency() != null) {
            reminder.setFrequency(dto.getFrequency());
        }
        if (dto.getStartDate() != null) {
            reminder.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            reminder.setEndDate(dto.getEndDate());
        }
        if (dto.getNote() != null) {
            reminder.setNote(dto.getNote());
        }
        if (dto.getActive() != null) {
            reminder.setActive(dto.getActive());
        }

        MedicationReminder updatedReminder = medicationReminderRepository.save(reminder);
        
        // 发送通知 - Requirements: 4.3
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishMedicationReminderUpdated(
                    updatedReminder.getId(),
                    updatedReminder.getUserId(),
                    currentUserId,
                    updatedReminder.getMedicationName()
            );
        }
        
        return convertToDTO(updatedReminder);
    }

    @Override
    @Transactional
    public void deleteReminder(Long reminderId, Long currentUserId) {
        MedicationReminder reminder = medicationReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("记录不存在"));

        // 验证权限：只有记录所属用户或创建者可以删除
        if (!reminder.getUserId().equals(currentUserId) && !reminder.getCreatorId().equals(currentUserId)) {
            // 检查是否是已接受的亲属
            if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, reminder.getUserId())) {
                throw new BusinessException("UNAUTHORIZED", "无权删除该记录");
            }
        }

        // 软删除：设置active=false
        reminder.setActive(false);
        medicationReminderRepository.save(reminder);
        
        // 发送通知 - Requirements: 4.4
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishMedicationReminderDeleted(
                    reminderId,
                    reminder.getUserId(),
                    currentUserId,
                    reminder.getMedicationName()
            );
        }
    }

    /**
     * 将实体转换为DTO
     */
    private MedicationReminderDTO convertToDTO(MedicationReminder reminder) {
        String creatorName = null;
        if (reminder.getCreatorId() != null) {
            User creator = userRepository.findById(reminder.getCreatorId()).orElse(null);
            if (creator != null) {
                creatorName = creator.getRealName() != null ? creator.getRealName() : creator.getUsername();
            }
        }

        return MedicationReminderDTO.builder()
                .id(reminder.getId())
                .userId(reminder.getUserId())
                .creatorId(reminder.getCreatorId())
                .creatorName(creatorName)
                .medicationName(reminder.getMedicationName())
                .dosage(reminder.getDosage())
                .reminderTimes(convertJsonToList(reminder.getReminderTimes()))
                .frequency(reminder.getFrequency())
                .startDate(reminder.getStartDate())
                .endDate(reminder.getEndDate())
                .note(reminder.getNote())
                .active(reminder.getActive())
                .createTime(reminder.getCreateTime())
                .updateTime(reminder.getUpdateTime())
                .build();
    }

    /**
     * 将List转换为JSON字符串
     */
    private String convertListToJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new BusinessException("JSON_ERROR", "时间列表格式转换失败");
        }
    }

    /**
     * 将JSON字符串转换为List
     */
    private List<String> convertJsonToList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException("JSON_ERROR", "时间列表格式解析失败");
        }
    }
}
