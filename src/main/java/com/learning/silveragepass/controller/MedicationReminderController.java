package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.health.MedicationReminderCreateDTO;
import com.learning.silveragepass.dto.health.MedicationReminderDTO;
import com.learning.silveragepass.dto.health.MedicationReminderUpdateDTO;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.HealthAuthorizationService;
import com.learning.silveragepass.service.MedicationReminderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用药提醒控制器
 * 处理用药提醒的创建、查询、更新和删除功能
 * Requirements: 3.1, 3.2, 3.4, 3.5, 3.6
 */
@RestController
@RequestMapping("/api/health/medication-reminder")
public class MedicationReminderController {

    private final MedicationReminderService medicationReminderService;
    private final HealthAuthorizationService healthAuthorizationService;

    @Autowired
    public MedicationReminderController(MedicationReminderService medicationReminderService,
                                        HealthAuthorizationService healthAuthorizationService) {
        this.medicationReminderService = medicationReminderService;
        this.healthAuthorizationService = healthAuthorizationService;
    }

    /**
     * 创建用药提醒（为当前用户）
     * Requirements: 3.1
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MedicationReminderDTO>> createReminder(
            @RequestBody @Valid MedicationReminderCreateDTO dto) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        MedicationReminderDTO reminder = medicationReminderService.createReminder(currentUserId, currentUserId, dto);
        return ResponseEntity.ok(ApiResponse.success("用药提醒创建成功", reminder));
    }

    /**
     * 为亲属创建用药提醒
     * Requirements: 3.2
     */
    @PostMapping("/for/{targetUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MedicationReminderDTO>> createReminderForRelative(
            @PathVariable Long targetUserId,
            @RequestBody @Valid MedicationReminderCreateDTO dto) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        
        // 验证亲属关系
        if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, targetUserId)) {
            throw new BusinessException("无权为该用户创建用药提醒");
        }
        
        MedicationReminderDTO reminder = medicationReminderService.createReminder(targetUserId, currentUserId, dto);
        return ResponseEntity.ok(ApiResponse.success("用药提醒创建成功", reminder));
    }

    /**
     * 获取当前用户的用药提醒列表
     * Requirements: 3.4
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MedicationReminderDTO>>> getReminders() {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        List<MedicationReminderDTO> reminders = medicationReminderService.getReminders(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    /**
     * 获取指定用户的用药提醒列表（需亲属权限）
     * Requirements: 3.4
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MedicationReminderDTO>>> getRemindersForUser(
            @PathVariable Long userId) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        
        // 验证访问权限
        if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, userId)) {
            throw new BusinessException("无权访问该用户的用药提醒");
        }
        
        List<MedicationReminderDTO> reminders = medicationReminderService.getReminders(userId);
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    /**
     * 更新用药提醒
     * Requirements: 3.5
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MedicationReminderDTO>> updateReminder(
            @PathVariable Long id,
            @RequestBody @Valid MedicationReminderUpdateDTO dto) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        MedicationReminderDTO reminder = medicationReminderService.updateReminder(id, currentUserId, dto);
        return ResponseEntity.ok(ApiResponse.success("用药提醒更新成功", reminder));
    }

    /**
     * 删除用药提醒（软删除）
     * Requirements: 3.6
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReminder(@PathVariable Long id) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        medicationReminderService.deleteReminder(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.successVoid("用药提醒删除成功"));
    }
}
