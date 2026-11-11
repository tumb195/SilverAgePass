package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.UserListDTO;
import com.learning.silveragepass.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台管理 - 用户统计控制器
 * 提供用户列表、创建统计、角色统计、状态统计等功能
 */
@Tag(name = "后台管理 - 用户统计", description = "用户统计相关接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserStatsController {
    
    private final AdminUserService adminUserService;
    
    /**
     * 获取用户列表（后台管理）
     */
    @GetMapping
    @Operation(summary = "获取用户列表", description = "后台管理 - 分页获取用户列表")
    public ResponseEntity<ApiResponse<PageResponse<UserListDTO>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        PageResponse<UserListDTO> response = adminUserService.getUserList(page, pageSize, search, role, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取用户创建统计（后台管理）
     */
    @GetMapping("/creation-stats")
    @Operation(summary = "获取用户创建统计", description = "后台管理 - 获取最近N天的用户创建统计")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCreationStats(
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> stats = adminUserService.getCreationStats(days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 获取角色统计（后台管理）
     */
    @GetMapping("/role-stats")
    @Operation(summary = "获取角色统计", description = "后台管理 - 获取各角色用户数量统计")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoleStats() {
        List<Map<String, Object>> stats = adminUserService.getRoleStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 获取状态统计（后台管理）
     */
    @GetMapping("/status-stats")
    @Operation(summary = "获取状态统计", description = "后台管理 - 获取各状态用户数量统计")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStatusStats() {
        List<Map<String, Object>> stats = adminUserService.getStatusStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
