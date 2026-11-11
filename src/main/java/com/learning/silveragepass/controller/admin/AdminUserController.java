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

@Tag(name = "后台管理 - 用户管理", description = "用户管理相关接口")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    
    private final AdminUserService adminUserService;
    
    @Operation(summary = "获取用户列表")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserListDTO>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        PageResponse<UserListDTO> response = adminUserService.getUserList(page, pageSize, search, role, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "获取用户总数")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getUserCount(
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        long count = adminUserService.getUserCount(includeDeleted);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
    
    @Operation(summary = "获取用户创建统计")
    @GetMapping("/creation-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCreationStats(
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> stats = adminUserService.getCreationStats(days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    @Operation(summary = "获取角色统计")
    @GetMapping("/role-stats")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoleStats() {
        List<Map<String, Object>> stats = adminUserService.getRoleStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    @Operation(summary = "获取状态统计")
    @GetMapping("/status-stats")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStatusStats() {
        List<Map<String, Object>> stats = adminUserService.getStatusStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    @Operation(summary = "禁用/解禁用户")
    @PostMapping("/ban")
    public ResponseEntity<ApiResponse<String>> banUser(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        boolean banned = (boolean) request.get("banned");
        String reason = (String) request.get("reason");
        Integer days = request.containsKey("days") ? (Integer) request.get("days") : null;
        
        String result = adminUserService.banUser(userId, banned, reason, days);
        return ResponseEntity.ok(ApiResponse.<String>success(result));
    }
    
    @Operation(summary = "删除用户")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<String>> deleteUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean hardDelete,
            @RequestParam(required = false) String reason) {
        String result = adminUserService.deleteUser(userId, hardDelete, reason);
        return ResponseEntity.ok(ApiResponse.<String>success(result));
    }
}
