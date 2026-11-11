package com.learning.silveragepass.forum.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.forum.dto.MyCommentDTO;
import com.learning.silveragepass.forum.dto.PostDTO;
import com.learning.silveragepass.forum.service.CommentService;
import com.learning.silveragepass.forum.service.PostService;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 论坛个人中心控制器
 */
@RestController
@RequestMapping("/api/forum/my")
@RequiredArgsConstructor
@Tag(name = "论坛-个人中心", description = "用户论坛相关数据接口")
public class ForumPersonalController {
    
    private final PostService postService;
    private final CommentService commentService;
    
    /**
     * 获取我的帖子列表
     */
    @GetMapping("/posts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的帖子列表", description = "获取当前用户发布的所有帖子")
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        PageResponse<PostDTO> posts = postService.getMyPosts(userId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(posts));
    }
    
    /**
     * 获取我的收藏列表
     */
    @GetMapping("/favorites")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的收藏列表", description = "获取当前用户收藏的所有帖子")
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        PageResponse<PostDTO> posts = postService.getMyFavorites(userId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(posts));
    }
    
    /**
     * 获取我的评论列表
     */
    @GetMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取我的评论列表", description = "获取当前用户发表的所有评论")
    public ResponseEntity<ApiResponse<PageResponse<MyCommentDTO>>> getMyComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        PageResponse<MyCommentDTO> comments = commentService.getMyComments(userId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
}

