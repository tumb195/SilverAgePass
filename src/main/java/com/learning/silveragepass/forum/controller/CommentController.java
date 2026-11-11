package com.learning.silveragepass.forum.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.forum.dto.*;
import com.learning.silveragepass.forum.service.CommentService;
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
 * 评论控制器
 */
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@Tag(name = "论坛-评论管理", description = "帖子评论相关接口")
public class CommentController {
    
    private final CommentService commentService;
    
    /**
     * 发表评论
     */
    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "发表评论", description = "对帖子发表评论，支持上传图片")
    public ResponseEntity<ApiResponse<CommentDTO>> createComment(
            @PathVariable Long postId,
            @Valid @ModelAttribute CommentCreateDTO commentCreateDTO,
            @RequestParam(value = "images", required = false) MultipartFile[] images) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        CommentDTO commentDTO = commentService.createComment(postId, commentCreateDTO, images, userId);
        
        return ResponseEntity.ok(ApiResponse.success("评论成功", commentDTO));
    }
    
    /**
     * 获取帖子评论列表
     */
    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "获取帖子评论列表", description = "获取指定帖子的评论列表")
    public ResponseEntity<ApiResponse<PageResponse<CommentDTO>>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = UserDetailsUtil.getCurrentUserIdOrNull();
        PageResponse<CommentDTO> comments = commentService.getComments(postId, userId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(comments));
    }
    
    /**
     * 回复评论
     */
    @PostMapping("/comments/{commentId}/replies")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "回复评论", description = "回复某条评论")
    public ResponseEntity<ApiResponse<ReplyDTO>> replyComment(
            @PathVariable Long commentId,
            @Valid @RequestBody ReplyCreateDTO replyCreateDTO) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        ReplyDTO replyDTO = commentService.replyComment(commentId, replyCreateDTO, userId);
        
        return ResponseEntity.ok(ApiResponse.success("回复成功", replyDTO));
    }
    
    /**
     * 获取评论的回复列表
     */
    @GetMapping("/comments/{commentId}/replies")
    @Operation(summary = "获取评论的回复列表", description = "分页获取指定评论的回复列表")
    public ResponseEntity<ApiResponse<PageResponse<ReplyDTO>>> getCommentReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = UserDetailsUtil.getCurrentUserIdOrNull();
        PageResponse<ReplyDTO> replies = commentService.getCommentReplies(commentId, userId, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(replies));
    }
    
    /**
     * 点赞/取消点赞评论
     */
    @PostMapping("/comments/{commentId}/like")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "点赞/取消点赞评论", description = "切换评论的点赞状态")
    public ResponseEntity<ApiResponse<LikeResultDTO>> toggleCommentLike(@PathVariable Long commentId) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        LikeResultDTO result = commentService.toggleCommentLike(commentId, userId);
        
        String message = result.getIsLiked() ? "点赞成功" : "取消点赞成功";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
    
    /**
     * 点赞/取消点赞回复
     */
    @PostMapping("/replies/{replyId}/like")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "点赞/取消点赞回复", description = "切换回复的点赞状态")
    public ResponseEntity<ApiResponse<LikeResultDTO>> toggleReplyLike(@PathVariable Long replyId) {
        Long userId = UserDetailsUtil.getCurrentUserId();
        LikeResultDTO result = commentService.toggleReplyLike(replyId, userId);
        
        String message = result.getIsLiked() ? "点赞成功" : "取消点赞成功";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}

