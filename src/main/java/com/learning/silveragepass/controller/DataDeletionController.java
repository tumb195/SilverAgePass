package com.learning.silveragepass.controller;

import com.learning.silveragepass.service.DataDeletionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 数据删除请求控制器
 * Data deletion request controller
 * 
 * 提供用户数据删除请求的REST API端点
 * Provides REST API endpoints for user data deletion requests
 * 
 * 端点：POST /api/health-assistant/data-deletion
 * Endpoint: POST /api/health-assistant/data-deletion
 */
@Slf4j
@RestController
@RequestMapping("/api/health-assistant/data-deletion")
public class DataDeletionController {

    private final DataDeletionService dataDeletionService;

    public DataDeletionController(DataDeletionService dataDeletionService) {
        this.dataDeletionService = dataDeletionService;
    }

    /**
     * 创建数据删除请求
     * Create data deletion request
     * 
     * 用户请求删除其所有健康助手数据
     * 数据将在30天内被永久删除
     * 
     * User requests deletion of all their health assistant data
     * Data will be permanently deleted within 30 days
     * 
     * @param userDetails 认证用户信息 Authenticated user details
     * @return 删除请求响应 Deletion request response
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDeletionRequest(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            // Extract user ID from authenticated user
            Long userId = extractUserId(userDetails);
            
            log.info("收到数据删除请求 - userId: {}", userId);

            // Create deletion request
            UUID requestId = dataDeletionService.createDeletionRequest(userId);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "数据删除请求已创建，您的数据将在30天内被永久删除");
            response.put("requestId", requestId.toString());
            response.put("scheduledDays", 30);

            log.info("数据删除请求创建成功 - userId: {}, requestId: {}", userId, requestId);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalStateException e) {
            log.warn("创建数据删除请求失败 - 用户已有待处理的请求: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "您已有待处理的数据删除请求");
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception e) {
            log.error("创建数据删除请求时发生错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "创建删除请求失败，请稍后重试");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取删除请求状态
     * Get deletion request status
     * 
     * @param userDetails 认证用户信息 Authenticated user details
     * @return 删除请求状态 Deletion request status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDeletionRequestStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long userId = extractUserId(userDetails);
            
            String status = dataDeletionService.getDeletionRequestStatus(userId);

            Map<String, Object> response = new HashMap<>();
            if (status != null) {
                response.put("success", true);
                response.put("status", status);
                response.put("message", getStatusMessage(status));
            } else {
                response.put("success", true);
                response.put("status", "NONE");
                response.put("message", "没有数据删除请求");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取删除请求状态时发生错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取状态失败");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 统计用户数据量
     * Count user data
     * 
     * @param userDetails 认证用户信息 Authenticated user details
     * @return 数据统计 Data count
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> countUserData(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long userId = extractUserId(userDetails);
            
            long count = dataDeletionService.countUserData(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRecords", count);
            response.put("message", String.format("您有 %d 条健康助手数据记录", count));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("统计用户数据时发生错误", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "统计数据失败");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 从UserDetails中提取用户ID
     * Extract user ID from UserDetails
     * 
     * @param userDetails 用户详情 User details
     * @return 用户ID User ID
     */
    private Long extractUserId(UserDetails userDetails) {
        // Assuming username is the user ID
        // Adjust this based on your actual UserDetails implementation
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            // If username is not a number, you might need to query the database
            // or use a custom UserDetails implementation that includes userId
            throw new IllegalArgumentException("无法从认证信息中提取用户ID");
        }
    }

    /**
     * 获取状态描述消息
     * Get status description message
     * 
     * @param status 状态 Status
     * @return 描述消息 Description message
     */
    private String getStatusMessage(String status) {
        return switch (status) {
            case "PENDING" -> "您的数据删除请求正在等待处理，将在30天内完成";
            case "PROCESSING" -> "您的数据正在删除中";
            case "COMPLETED" -> "您的数据已成功删除";
            case "FAILED" -> "数据删除失败，请联系客服";
            default -> "未知状态";
        };
    }
}
