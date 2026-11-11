package com.learning.silveragepass.controller.admin;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.UserFilterOptionsDTO;
import com.learning.silveragepass.dto.admin.UserSearchResultDTO;
import com.learning.silveragepass.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台管理 - 用户搜索控制器
 * 提供用户搜索和详细信息查询功能
 */
@Tag(name = "后台管理 - 用户搜索", description = "用户搜索相关接口")
@RestController
@RequestMapping("/api/admin/user-search")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserSearchController {
    
    private final AdminUserService adminUserService;
    
    /**
     * 搜索用户
     * 支持根据用户名、手机号、角色和状态进行搜索和筛选
     * 当搜索条件为空时，返回所有用户信息（分页）
     */
    @GetMapping
    @Operation(
        summary = "搜索用户", 
        description = "根据用户名、手机号、角色和状态搜索用户。支持模糊搜索和精确筛选。当所有条件为空时，返回所有用户信息（分页）"
    )
    public ResponseEntity<ApiResponse<PageResponse<UserSearchResultDTO>>> searchUsers(
            @Parameter(description = "用户名（可选，支持模糊搜索）") 
            @RequestParam(required = false) String username,
            
            @Parameter(description = "手机号（可选，支持模糊搜索）") 
            @RequestParam(required = false) String phone,
            
            @Parameter(description = "角色（可选，ELDERLY=需求者, VOLUNTEER=志愿者）") 
            @RequestParam(required = false) String role,
            
            @Parameter(description = "状态（可选，NORMAL=正常, BANNED=已禁用）") 
            @RequestParam(required = false) String status,
            
            @Parameter(description = "页码，从1开始") 
            @RequestParam(defaultValue = "1") int page,
            
            @Parameter(description = "每页数量") 
            @RequestParam(defaultValue = "10") int pageSize) {
        
        PageResponse<UserSearchResultDTO> response = adminUserService.searchUsers(username, phone, role, status, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 获取筛选选项
     * 返回所有可用的角色和状态选项，用于前端动态生成筛选下拉框
     */
    @GetMapping("/filter-options")
    @Operation(
        summary = "获取筛选选项", 
        description = "获取所有可用的角色和状态选项，用于前端动态生成筛选下拉框"
    )
    public ResponseEntity<ApiResponse<UserFilterOptionsDTO>> getFilterOptions() {
        UserFilterOptionsDTO options = adminUserService.getUserFilterOptions();
        return ResponseEntity.ok(ApiResponse.success(options));
    }
    
    /**
     * 根据用户ID获取详细信息
     */
    @GetMapping("/{userId}")
    @Operation(summary = "获取用户详细信息", description = "根据用户ID获取用户的详细信息")
    public ResponseEntity<ApiResponse<UserSearchResultDTO>> getUserDetail(
            @Parameter(description = "用户ID") 
            @PathVariable Long userId) {
        
        UserSearchResultDTO userDetail = adminUserService.getUserDetailById(userId);
        return ResponseEntity.ok(ApiResponse.success(userDetail));
    }
}
