package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.notification.NotificationDTO;
import com.learning.silveragepass.dto.notification.UnreadCountDTO;
import com.learning.silveragepass.entity.NotificationType;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 通知消息REST API控制器
 * 提供通知的查询、已读标记、删除等功能
 * 
 * Requirements: 3.2, 3.3, 3.4, 3.5, 8.1
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知管理", description = "通知消息的查询、已读标记、删除等接口")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取当前用户的通知列表（分页）
     * 
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @param type 通知类型（可选）
     * @param category 通知分类（可选）：system, health, demand, forum, social
     * @return 分页的通知列表
     * 
     * Requirements: 3.2, 3.3
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "获取通知列表", description = "获取当前用户的通知列表，支持分页和按类型/分类筛选")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getUserNotifications(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "通知类型") @RequestParam(required = false) NotificationType type,
            @Parameter(description = "通知分类：system, health, demand, forum, social") @RequestParam(required = false) String category) {
        
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        
        Page<NotificationDTO> notifications;
        if (type != null) {
            // 按类型筛选
            notifications = notificationService.getUserNotificationsByType(currentUserId, type, pageable);
        } else if (category != null && !category.isEmpty()) {
            // 按分类筛选
            notifications = notificationService.getUserNotificationsByCategory(currentUserId, category, pageable);
        } else {
            // 获取所有通知
            notifications = notificationService.getUserNotifications(currentUserId, pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * 获取当前用户的未读消息统计
     * 
     * @return 未读消息统计（总数、按分类、按类型）
     * 
     * Requirements: 8.1
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "获取未读统计", description = "获取当前用户的未读消息数量统计，包括总数、按分类和按类型的统计")
    public ResponseEntity<ApiResponse<UnreadCountDTO>> getUnreadCount() {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        UnreadCountDTO unreadCount = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(unreadCount));
    }

    /**
     * 标记单条通知为已读
     * 
     * @param id 通知ID
     * @return 操作结果
     * 
     * Requirements: 3.4
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "标记单条已读", description = "将指定的通知标记为已读状态")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @Parameter(description = "通知ID") @PathVariable Long id) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        boolean success = notificationService.markAsRead(id, currentUserId);
        
        if (success) {
            return ResponseEntity.ok(ApiResponse.successVoid("标记已读成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "通知不存在或无权操作"));
        }
    }

    /**
     * 标记当前用户所有通知为已读
     * 
     * @return 标记的消息数量
     * 
     * Requirements: 3.4, 8.4
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "标记全部已读", description = "将当前用户的所有未读通知标记为已读")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead() {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        int count = notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("已标记 " + count + " 条消息为已读", count));
    }

    /**
     * 按类型标记通知为已读
     * 
     * @param type 通知类型
     * @return 标记的消息数量
     * 
     * Requirements: 3.4
     */
    @PutMapping("/read-by-type")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "按类型标记已读", description = "将指定类型的所有未读通知标记为已读")
    public ResponseEntity<ApiResponse<Integer>> markAsReadByType(
            @Parameter(description = "通知类型") @RequestParam NotificationType type) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        int count = notificationService.markAsReadByType(currentUserId, type);
        return ResponseEntity.ok(ApiResponse.success("已标记 " + count + " 条消息为已读", count));
    }

    /**
     * 删除单条通知
     * 
     * @param id 通知ID
     * @return 操作结果
     * 
     * Requirements: 3.5
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "删除通知", description = "删除指定的通知")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @Parameter(description = "通知ID") @PathVariable Long id) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        boolean success = notificationService.deleteNotification(id, currentUserId);
        
        if (success) {
            return ResponseEntity.ok(ApiResponse.successVoid("删除成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "通知不存在或无权操作"));
        }
    }
}
