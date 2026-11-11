package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.UserPublicDTO;
import com.learning.silveragepass.dto.UserStatisticsDTO;
import com.learning.silveragepass.dto.UserUpdateDTO;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.FileStorageService;
import com.learning.silveragepass.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户控制器
 * 处理用户信息查询、更新、地址管理等功能
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户信息查询、更新、统计等接口")
public class UserController {
    
    private final UserService userService;
    private final FileStorageService fileStorageService;
    
    @Autowired
    public UserController(UserService userService, FileStorageService fileStorageService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }
    
    /**
     * 获取当前用户信息（完整信息）
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        Long userId = UserDetailsUtil.getCurrentUserId();
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    /**
     * 根据用户ID获取用户公开信息（不包含敏感信息）
     * 此接口需要登录，用于查看其他用户的基本信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取用户公开信息", description = "获取指定用户的公开信息，不包含手机号、邮箱等敏感信息")
    public ResponseEntity<ApiResponse<UserPublicDTO>> getUserPublicInfo(@PathVariable Long id) {
        UserPublicDTO userPublicInfo = userService.getUserPublicInfo(id);
        return ResponseEntity.ok(ApiResponse.success(userPublicInfo));
    }
    
    /**
     * 更新用户信息
     * 只允许更新非敏感信息：真实姓名、头像、年龄、地址、邮箱
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UserUpdateDTO userUpdateDTO) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        
        // 检查是否是当前用户
        if (!id.equals(userId)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("无权修改其他用户信息"));
        }
        
        User updatedUser = userService.updateUserInfo(id, userUpdateDTO);
        return ResponseEntity.ok(ApiResponse.success("用户信息更新成功", updatedUser));
    }
    
    /**
     * 上传并更新用户头像（一步完成）
     * 自动删除旧头像
     */
    @PostMapping("/{id}/avatar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "上传并更新用户头像")
    public ResponseEntity<ApiResponse<User>> uploadAndUpdateAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        
        // 检查是否是当前用户
        if (!id.equals(userId)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("无权修改其他用户信息"));
        }
        
        // 1. 上传新头像文件
        String avatarUrl = fileStorageService.storeFile(file, "avatars");
        
        // 2. 更新用户信息（会自动删除旧头像）
        UserUpdateDTO userUpdateDTO = new UserUpdateDTO();
        userUpdateDTO.setAvatar(avatarUrl);
        User updatedUser = userService.updateUserInfo(id, userUpdateDTO);
        
        return ResponseEntity.ok(ApiResponse.success("头像更新成功", updatedUser));
    }
    
    /**
     * 获取用户统计信息
     */
    @GetMapping("/{id}/statistics")
    @Operation(summary = "获取用户统计信息", description = "获取用户的发帖数、粉丝数、关注数、收藏帖子数")
    public ResponseEntity<ApiResponse<UserStatisticsDTO>> getUserStatistics(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        UserStatisticsDTO statistics = userService.getUserStatistics(id);
        return ResponseEntity.ok(ApiResponse.success("获取统计信息成功", statistics));
    }
    
    /**
     * 获取当前用户统计信息
     */
    @GetMapping("/me/statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前用户统计信息", description = "获取当前登录用户的发帖数、粉丝数、关注数、收藏帖子数")
    public ResponseEntity<ApiResponse<UserStatisticsDTO>> getCurrentUserStatistics() {
        Long userId = UserDetailsUtil.getCurrentUserId();
        UserStatisticsDTO statistics = userService.getUserStatistics(userId);
        return ResponseEntity.ok(ApiResponse.success("获取统计信息成功", statistics));
    }
} 