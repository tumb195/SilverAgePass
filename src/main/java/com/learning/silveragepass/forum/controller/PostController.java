package com.learning.silveragepass.forum.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.forum.dto.PostCreateDTO;
import com.learning.silveragepass.forum.dto.PostDTO;
import com.learning.silveragepass.forum.dto.PostQueryDTO;
import com.learning.silveragepass.forum.service.PostService;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 帖子控制器
 */
@RestController
@RequestMapping("/api/forum/posts")
@RequiredArgsConstructor
@Tag(name = "论坛-帖子管理", description = "论坛帖子相关接口")
public class PostController {
    
    private final PostService postService;
    
    /**
     * 发布帖子
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发布帖子", description = "用户发布新帖子，支持上传图片或视频")
    public ResponseEntity<ApiResponse<PostDTO>> createPost(
            @Valid @ModelAttribute PostCreateDTO postCreateDTO,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "video", required = false) MultipartFile video) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        PostDTO postDTO = postService.createPost(postCreateDTO, images, video, userId);
        
        return ResponseEntity.ok(ApiResponse.success("发布成功", postDTO));
    }
    
    /**
     * 获取帖子列表
     */
    @GetMapping
    @Operation(summary = "获取帖子列表", description = "获取帖子列表，支持按类型、关键词筛选")
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getPosts(
            @ModelAttribute PostQueryDTO queryDTO) {
        
        Long userId = UserDetailsUtil.getCurrentUserIdOrNull();
        PageResponse<PostDTO> posts = postService.getPosts(queryDTO, userId);
        
        return ResponseEntity.ok(ApiResponse.success(posts));
    }
    
    /**
     * 获取帖子详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取帖子详情", description = "获取指定帖子的详细信息")
    public ResponseEntity<ApiResponse<PostDTO>> getPostById(@PathVariable Long id) {
        Long userId = UserDetailsUtil.getCurrentUserIdOrNull();
        PostDTO postDTO = postService.getPostById(id, userId);
        
        return ResponseEntity.ok(ApiResponse.success(postDTO));
    }
    
    /**
     * 删除帖子
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "删除帖子", description = "删除自己发布的帖子")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        postService.deletePost(id, userId);
        
        return ResponseEntity.ok(ApiResponse.successVoid("删除成功"));
    }
    
    /**
     * 根据用户ID获取该用户发布的所有帖子
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户发布的帖子", description = "查看指定用户发布的所有帖子，支持登录和未登录访问")
    public ResponseEntity<ApiResponse<PageResponse<PostDTO>>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        // 获取当前登录用户ID（可能为null，表示未登录）
        Long currentUserId = UserDetailsUtil.getCurrentUserIdOrNull();
        
        // 查询指定用户的帖子，使用当前用户ID来判断点赞和收藏状态
        PageResponse<PostDTO> posts = postService.getUserPosts(userId, currentUserId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(posts));
    }
}

