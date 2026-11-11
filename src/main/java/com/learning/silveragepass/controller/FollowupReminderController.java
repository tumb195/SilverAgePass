package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.health.FollowupReminderCreateDTO;
import com.learning.silveragepass.dto.health.FollowupReminderDTO;
import com.learning.silveragepass.dto.health.FollowupReminderUpdateDTO;
import com.learning.silveragepass.entity.ReminderStatus;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.FollowupReminderService;
import com.learning.silveragepass.service.HealthAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 复诊提醒控制器
 * 处理复诊提醒的创建、查询、更新、状态管理和删除功能
 * Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 4.1, 5.1
 */
@RestController
@RequestMapping("/api/health/followup-reminder")
public class FollowupReminderController {

    private final FollowupReminderService followupReminderService;
    private final HealthAuthorizationService healthAuthorizationService;

    @Autowired
    public FollowupReminderController(FollowupReminderService followupReminderService,
                                      HealthAuthorizationService healthAuthorizationService) {
        this.followupReminderService = followupReminderService;
        this.healthAuthorizationService = healthAuthorizationService;
    }

    /**
     * 创建复诊提醒（为当前用户）
     * Requirements: 1.1
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FollowupReminderDTO>> createReminder(
            @RequestBody @Valid FollowupReminderCreateDTO dto) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        FollowupReminderDTO reminder = followupReminderService.createReminder(currentUserId, currentUserId, dto);
        return ResponseEntity.ok(ApiResponse.success("复诊提醒创建成功", reminder));
    }

    /**
     * 为亲属创建复诊提醒
     * Requirements: 1.2, 5.1
     */
    @PostMapping("/for/{targetUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FollowupReminderDTO>> createReminderForRelative(
            @PathVariable Long targetUserId,
            @RequestBody @Valid FollowupReminderCreateDTO dto) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        
        // 验证亲属关系
        if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, targetUserId)) {
            throw new BusinessException("无权为该用户创建复诊提醒");
        }
        
        FollowupReminderDTO reminder = followupReminderService.createReminder(targetUserId, currentUserId, dto);
        return ResponseEntity.ok(ApiResponse.success("复诊提醒创建成功", reminder));
    }


    /**
     * 获取当前用户的复诊提醒列表
     * 支持可选的状态过滤
     * Requirements: 2.1, 2.2
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FollowupReminderDTO>>> getReminders(
            @RequestParam(required = false) ReminderStatus status) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        List<FollowupReminderDTO> reminders = followupReminderService.getReminders(currentUserId, status);
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    /**
     * 获取指定用户的复诊提醒列表（需亲属权限）
     * Requirements: 2.3, 5.1
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FollowupReminderDTO>>> getRemindersForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) ReminderStatus status) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        
        // 验证访问权限
        if (!healthAuthorizationService.canAccessUserHealthData(currentUserId, userId)) {
            throw new BusinessException("无权访问该用户的复诊提醒");
        }
        
        List<FollowupReminderDTO> reminders = followupReminderService.getReminders(userId, status);
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    /**
     * 获取即将到来的复诊提醒（7天内，PENDING状态）
     * Requirements: 4.1
     */
    @GetMapping("/upcoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FollowupReminderDTO>>> getUpcomingReminders() {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        List<FollowupReminderDTO> reminders = followupReminderService.getUpcomingReminders(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(reminders));
    }

    /**
     * 更新复诊提醒
     * Requirements: 3.1
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FollowupReminderDTO>> updateReminder(
            @PathVariable Long id,
            @RequestBody @Valid FollowupReminderUpdateDTO dto) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        FollowupReminderDTO reminder = followupReminderService.updateReminder(id, currentUserId, dto);
        return ResponseEntity.ok(ApiResponse.success("复诊提醒更新成功", reminder));
    }

    /**
     * 标记复诊提醒为已完成
     * Requirements: 3.2
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FollowupReminderDTO>> markAsCompleted(@PathVariable Long id) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        FollowupReminderDTO reminder = followupReminderService.markAsCompleted(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("复诊提醒已标记为完成", reminder));
    }

    /**
     * 取消复诊提醒
     * Requirements: 3.3
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FollowupReminderDTO>> cancelReminder(@PathVariable Long id) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        FollowupReminderDTO reminder = followupReminderService.cancelReminder(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("复诊提醒已取消", reminder));
    }

    /**
     * 删除复诊提醒（硬删除）
     * Requirements: 3.4
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReminder(@PathVariable Long id) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        followupReminderService.deleteReminder(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.successVoid("复诊提醒删除成功"));
    }
}
