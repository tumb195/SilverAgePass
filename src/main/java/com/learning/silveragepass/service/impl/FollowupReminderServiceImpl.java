package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.health.FollowupReminderCreateDTO;
import com.learning.silveragepass.dto.health.FollowupReminderDTO;
import com.learning.silveragepass.dto.health.FollowupReminderUpdateDTO;
import com.learning.silveragepass.entity.FollowupReminder;
import com.learning.silveragepass.entity.ReminderStatus;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.FollowupReminderRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.FollowupReminderService;
import com.learning.silveragepass.service.HealthAuthorizationService;
import com.learning.silveragepass.service.NotificationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 复诊提醒服务实现
 * Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 5.1, 5.2, 5.3, 5.4
 */
@Service
public class FollowupReminderServiceImpl implements FollowupReminderService {

    @Autowired
    private FollowupReminderRepository followupReminderRepository;

    @Autowired
    private HealthAuthorizationService healthAuthorizationService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;

    @Override
    @Transactional
    public FollowupReminderDTO createReminder(Long userId, Long creatorId, FollowupReminderCreateDTO dto) {
        // 验证亲属权限（如果创建者不是用户本人）
        // Requirements: 1.2, 5.1, 5.2
        if (!userId.equals(creatorId)) {
            if (!healthAuthorizationService.canAccessUserHealthData(creatorId, userId)) {
                throw new BusinessException("UNAUTHORIZED", "无权访问该用户的复诊提醒");
            }
        }

        // 创建复诊提醒
        // Requirements: 1.1, 1.3
        FollowupReminder reminder = new FollowupReminder();
        reminder.setUserId(userId);
        reminder.setCreatorId(creatorId);
        reminder.setAppointmentDate(dto.getAppointmentDate());
        reminder.setAppointmentTime(dto.getAppointmentTime());
        reminder.setHospitalName(dto.getHospitalName());
        reminder.setDoctorName(dto.getDoctorName());
        reminder.setDepartment(dto.getDepartment());
        reminder.setNote(dto.getNote());
        reminder.setStatus(ReminderStatus.PENDING);

        FollowupReminder savedReminder = followupReminderRepository.save(reminder);
        
        // 发送通知 - Requirements: 5.3
        if (notificationEventPublisher != null) {
            String appointmentDateStr = dto.getAppointmentDate() != null ? dto.getAppointmentDate().toString() : "";
            notificationEventPublisher.publishFollowupReminderCreated(
                    savedReminder.getId(),
                    userId,
                    creatorId,
                    dto.getHospitalName(),
                    appointmentDateStr
            );
        }
        
        return convertToDTO(savedReminder);
    }


    @Override
    public List<FollowupReminderDTO> getReminders(Long userId, ReminderStatus status) {
        // Requirements: 2.1, 2.2, 2.4
        List<FollowupReminder> reminders;
        
        if (status != null) {
            // 按状态过滤
            reminders = followupReminderRepository.findByUserIdAndStatus(userId, status);
            // 手动排序，因为findByUserIdAndStatus没有排序
            reminders.sort(Comparator.comparing(FollowupReminder::getAppointmentDate)
                    .thenComparing(FollowupReminder::getAppointmentTime));
        } else {
            // 获取所有提醒，已按日期时间升序排列
            reminders = followupReminderRepository.findByUserIdOrderByAppointmentDateAscAppointmentTimeAsc(userId);
        }
        
        return reminders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FollowupReminderDTO> getUpcomingReminders(Long userId) {
        // Requirements: 4.1
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(7);
        
        List<FollowupReminder> reminders = followupReminderRepository
                .findByUserIdAndStatusAndAppointmentDateBetween(userId, ReminderStatus.PENDING, today, endDate);
        
        // 按日期时间升序排列
        reminders.sort(Comparator.comparing(FollowupReminder::getAppointmentDate)
                .thenComparing(FollowupReminder::getAppointmentTime));
        
        return reminders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FollowupReminderDTO updateReminder(Long reminderId, Long currentUserId, FollowupReminderUpdateDTO dto) {
        // Requirements: 3.1, 5.1, 5.2
        FollowupReminder reminder = followupReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("复诊提醒不存在"));

        // 验证权限：只有记录所属用户或已接受的亲属可以更新
        if (!reminder.getUserId().equals(currentUserId)) {
            if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, reminder.getUserId())) {
                throw new BusinessException("UNAUTHORIZED", "无权修改该复诊提醒");
            }
        }

        // 更新字段（只更新非空字段）
        if (dto.getAppointmentDate() != null) {
            reminder.setAppointmentDate(dto.getAppointmentDate());
        }
        if (dto.getAppointmentTime() != null) {
            reminder.setAppointmentTime(dto.getAppointmentTime());
        }
        if (dto.getHospitalName() != null) {
            reminder.setHospitalName(dto.getHospitalName());
        }
        if (dto.getDoctorName() != null) {
            reminder.setDoctorName(dto.getDoctorName());
        }
        if (dto.getDepartment() != null) {
            reminder.setDepartment(dto.getDepartment());
        }
        if (dto.getNote() != null) {
            reminder.setNote(dto.getNote());
        }

        FollowupReminder updatedReminder = followupReminderRepository.save(reminder);
        return convertToDTO(updatedReminder);
    }


    @Override
    @Transactional
    public FollowupReminderDTO markAsCompleted(Long reminderId, Long currentUserId) {
        // Requirements: 3.2, 5.1, 5.2
        FollowupReminder reminder = followupReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("复诊提醒不存在"));

        // 验证权限
        if (!reminder.getUserId().equals(currentUserId)) {
            if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, reminder.getUserId())) {
                throw new BusinessException("UNAUTHORIZED", "无权修改该复诊提醒");
            }
        }

        // 检查当前状态
        if (reminder.getStatus() != ReminderStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "该提醒已完成或已取消");
        }

        reminder.setStatus(ReminderStatus.COMPLETED);
        FollowupReminder updatedReminder = followupReminderRepository.save(reminder);
        
        // 发送通知 - Requirements: 5.4
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishFollowupReminderStatusChanged(
                    updatedReminder.getId(),
                    updatedReminder.getUserId(),
                    currentUserId,
                    updatedReminder.getHospitalName(),
                    "COMPLETED"
            );
        }
        
        return convertToDTO(updatedReminder);
    }

    @Override
    @Transactional
    public FollowupReminderDTO cancelReminder(Long reminderId, Long currentUserId) {
        // Requirements: 3.3, 5.1, 5.2
        FollowupReminder reminder = followupReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("复诊提醒不存在"));

        // 验证权限
        if (!reminder.getUserId().equals(currentUserId)) {
            if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, reminder.getUserId())) {
                throw new BusinessException("UNAUTHORIZED", "无权修改该复诊提醒");
            }
        }

        // 检查当前状态
        if (reminder.getStatus() != ReminderStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "该提醒已完成或已取消");
        }

        reminder.setStatus(ReminderStatus.CANCELLED);
        FollowupReminder updatedReminder = followupReminderRepository.save(reminder);
        
        // 发送通知 - Requirements: 5.4
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishFollowupReminderStatusChanged(
                    updatedReminder.getId(),
                    updatedReminder.getUserId(),
                    currentUserId,
                    updatedReminder.getHospitalName(),
                    "CANCELLED"
            );
        }
        
        return convertToDTO(updatedReminder);
    }

    @Override
    @Transactional
    public void deleteReminder(Long reminderId, Long currentUserId) {
        // Requirements: 3.4, 5.1, 5.2
        FollowupReminder reminder = followupReminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("复诊提醒不存在"));

        // 验证权限
        if (!reminder.getUserId().equals(currentUserId)) {
            if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, reminder.getUserId())) {
                throw new BusinessException("UNAUTHORIZED", "无权删除该复诊提醒");
            }
        }

        // 硬删除
        followupReminderRepository.delete(reminder);
    }

    /**
     * 将实体转换为DTO
     * Requirements: 4.2 - 计算daysRemaining
     */
    private FollowupReminderDTO convertToDTO(FollowupReminder reminder) {
        String creatorName = null;
        if (reminder.getCreatorId() != null) {
            User creator = userRepository.findById(reminder.getCreatorId()).orElse(null);
            if (creator != null) {
                creatorName = creator.getRealName() != null ? creator.getRealName() : creator.getUsername();
            }
        }

        // 计算距离复诊还有几天
        Integer daysRemaining = null;
        if (reminder.getAppointmentDate() != null) {
            daysRemaining = (int) ChronoUnit.DAYS.between(LocalDate.now(), reminder.getAppointmentDate());
        }

        return FollowupReminderDTO.builder()
                .id(reminder.getId())
                .userId(reminder.getUserId())
                .creatorId(reminder.getCreatorId())
                .creatorName(creatorName)
                .appointmentDate(reminder.getAppointmentDate())
                .appointmentTime(reminder.getAppointmentTime())
                .hospitalName(reminder.getHospitalName())
                .doctorName(reminder.getDoctorName())
                .department(reminder.getDepartment())
                .note(reminder.getNote())
                .status(reminder.getStatus())
                .statusDescription(reminder.getStatus() != null ? reminder.getStatus().getDescription() : null)
                .daysRemaining(daysRemaining)
                .createTime(reminder.getCreateTime())
                .updateTime(reminder.getUpdateTime())
                .build();
    }
}
