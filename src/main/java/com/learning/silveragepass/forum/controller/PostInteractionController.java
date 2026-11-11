package com.learning.silveragepass.forum.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.forum.dto.LikeResultDTO;
import com.learning.silveragepass.forum.service.PostInteractionService;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子互动控制器（点赞、收藏）
 */
@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
@Tag(name = "论坛-帖子互动", description = "帖子点赞、收藏相关接口")
public class PostInteractionController {
    
    private final PostInteractionService postInteractionService;
    
    /**
     * 点赞/取消点赞帖子
     */
    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "点赞/取消点赞帖子", description = "切换帖子的点赞状态")
    public ResponseEntity<ApiResponse<LikeResultDTO>> toggleLike(@PathVariable Long id) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        LikeResultDTO result = postInteractionService.toggleLike(id, userId);
        
        String message = result.getIsLiked() ? "点赞成功" : "取消点赞成功";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
    
    /**
     * 收藏/取消收藏帖子
     */
    @PostMapping("/{id}/favorite")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "收藏/取消收藏帖子", description = "切换帖子的收藏状态")
    public ResponseEntity<ApiResponse<LikeResultDTO>> toggleFavorite(@PathVariable Long id) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        LikeResultDTO result = postInteractionService.toggleFavorite(id, userId);
        
        String message = result.getIsLiked() ? "收藏成功" : "取消收藏成功";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}

