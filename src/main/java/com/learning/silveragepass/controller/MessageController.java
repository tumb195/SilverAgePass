package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.MessageDto;
import com.learning.silveragepass.entity.Message;
import com.learning.silveragepass.entity.MessageType;
import com.learning.silveragepass.security.CurrentUser;
import com.learning.silveragepass.security.UserPrincipal;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 * 处理系统消息的查询、已读标记、删除等功能
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 获取当前用户的消息列表
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 分页的消息列表
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MessageDto>> getUserMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        return ResponseEntity.ok(messageService.getUserMessages(currentUserId, page, size));
    }

    /**
     * 获取当前用户特定类型的消息列表
     * @param type 消息类型
     * @param page 页码
     * @param size 每页数量
     * @return 分页的消息列表
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<MessageDto>> getUserMessagesByType(
            @PathVariable MessageType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        return ResponseEntity.ok(messageService.getUserMessagesByType(currentUserId, type, page, size));
    }

    /**
     * 获取当前用户的未读消息数量
     * @return 未读消息数量
     */
    @GetMapping("/unread/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Long>> getUnreadMessageCount() {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        Long count = messageService.getUnreadMessageCount(currentUserId);
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    /**
     * 标记消息为已读
     * @param messageId 消息ID
     * @return 操作结果
     */
    @PutMapping("/{messageId}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> markMessageAsRead(@PathVariable Long messageId) {
        messageService.markMessageAsRead(messageId);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量标记消息为已读
     * @param messageIds 消息ID列表
     * @return 操作结果
     */
    @PutMapping("/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> markMessagesAsRead(@RequestBody List<Long> messageIds) {
        messageService.markMessagesAsRead(messageIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除消息
     * @param messageId 消息ID
     * @return 操作结果
     */
    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.ok().build();
    }

    /**
     * 批量删除消息
     * @param messageIds 消息ID列表
     * @return 操作结果
     */
    @DeleteMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteMessages(@RequestBody List<Long> messageIds) {
        messageService.deleteMessages(messageIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取消息详情
     * @param messageId 消息ID
     * @return 消息详情
     */
    @GetMapping("/{messageId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageDto> getMessageDetail(@PathVariable Long messageId) {
        return ResponseEntity.ok(messageService.getMessageDetail(messageId));
    }

    /**
     * 管理员发送消息
     * @param currentUser 当前管理员用户
     * @param title 消息标题
     * @param content 消息内容
     * @param receiverId 接收者ID
     * @return 发送的消息
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Message> sendAdminMessage(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam Long receiverId) {
        return ResponseEntity.ok(messageService.sendAdminMessage(title, content, currentUser.getId(), receiverId));
    }
} 