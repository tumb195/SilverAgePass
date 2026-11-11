package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.*;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.UserFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户关注控制器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户关注", description = "用户关注相关接口")
public class UserFollowController {
    
    private final UserFollowService userFollowService;
    
    /**
     * 关注/取消关注用户
     */
    @PostMapping("/{userId}/follow")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "关注/取消关注用户", description = "切换对指定用户的关注状态")
    public ResponseEntity<ApiResponse<FollowResultDTO>> toggleFollow(
            @Parameter(description = "被关注用户ID") @PathVariable Long userId) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        FollowResultDTO result = userFollowService.toggleFollow(currentUserId, userId);
        
        String message = result.getIsFollowing() ? "关注成功" : "取消关注成功";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
    
    /**
     * 获取用户的关注列表（我关注的人）
     */
    @GetMapping("/{userId}/following")
    @Operation(summary = "获取关注列表", description = "获取用户关注的人的列表")
    public ResponseEntity<ApiResponse<PageResponse<FollowDTO>>> getFollowingList(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = UserDetailsUtil.getCurrentUserIdOrNull();
        PageResponse<FollowDTO> result = userFollowService.getFollowingList(userId, currentUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success("获取关注列表成功", result));
    }
    
    /**
     * 获取用户的粉丝列表（关注我的人）
     */
    @GetMapping("/{userId}/followers")
    @Operation(summary = "获取粉丝列表", description = "获取关注该用户的人的列表")
    public ResponseEntity<ApiResponse<PageResponse<FollowDTO>>> getFollowersList(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = UserDetailsUtil.getCurrentUserIdOrNull();
        PageResponse<FollowDTO> result = userFollowService.getFollowersList(userId, currentUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success("获取粉丝列表成功", result));
    }
    
    /**
     * 检查是否已关注某用户
     */
    @GetMapping("/{userId}/is-following")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "检查是否已关注", description = "检查当前用户是否已关注指定用户")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @Parameter(description = "被关注用户ID") @PathVariable Long userId) {
        Long currentUserId = UserDetailsUtil.getCurrentUserId();
        boolean isFollowing = userFollowService.isFollowing(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.success("查询成功", isFollowing));
    }
    
    /**
     * 获取用户的关注统计信息
     */
    @GetMapping("/{userId}/follow-statistics")
    @Operation(summary = "获取关注统计", description = "获取用户的关注、粉丝、互相关注数量统计")
    public ResponseEntity<ApiResponse<FollowStatisticsDTO>> getFollowStatistics(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        FollowStatisticsDTO result = userFollowService.getFollowStatistics(userId);
        return ResponseEntity.ok(ApiResponse.success("获取统计信息成功", result));
    }
}

