package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.video.*;
import com.learning.silveragepass.security.services.UserDetailsUtil;
import com.learning.silveragepass.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视频控制器（用户端）
 * 处理视频列表、详情、播放URL、分类列表、观看历史等功能
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.4, 2.5, 6.2, 6.3
 */
@RestController
@RequestMapping("/api/videos")
@Tag(name = "视频模块", description = "视频浏览、播放、观看历史等接口")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    /**
     * 获取视频列表（分页）
     * 支持分类筛选和关键词搜索
     * 
     * @param categoryId 分类ID（可选）
     * @param keyword 搜索关键词（可选）
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 视频分页列表
     * 
     * Requirements: 1.1, 1.2, 1.3, 1.4
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取视频列表", description = "获取已发布的视频列表，支持分类筛选和关键词搜索")
    public ResponseEntity<ApiResponse<PageResponse<VideoDTO>>> getVideos(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<VideoDTO> videos = videoService.getVideos(categoryId, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(videos));
    }

    /**
     * 获取视频详情
     * 
     * @param id 视频ID
     * @return 视频详情
     * 
     * Requirements: 1.4, 2.4
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取视频详情", description = "根据ID获取视频详细信息")
    public ResponseEntity<ApiResponse<VideoDTO>> getVideo(
            @Parameter(description = "视频ID") @PathVariable Long id) {
        
        VideoDTO video = videoService.getVideo(id);
        return ResponseEntity.ok(ApiResponse.success(video));
    }

    /**
     * 获取视频播放URL
     * 生成带签名的临时访问URL，并记录观看历史
     * 
     * @param id 视频ID
     * @return 视频播放信息（包含签名URL和上次观看位置）
     * 
     * Requirements: 2.1, 2.4, 2.5
     */
    @GetMapping("/{id}/play")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取视频播放URL", description = "获取带签名的临时播放URL，同时返回上次观看位置")
    public ResponseEntity<ApiResponse<VideoPlayDTO>> getPlayUrl(
            @Parameter(description = "视频ID") @PathVariable Long id) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        VideoPlayDTO playInfo = videoService.getPlayUrl(id, userId);
        return ResponseEntity.ok(ApiResponse.success(playInfo));
    }

    /**
     * 获取所有启用的视频分类
     * 
     * @return 分类列表（按sortOrder排序）
     * 
     * Requirements: 1.1
     */
    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取视频分类列表", description = "获取所有启用的视频分类")
    public ResponseEntity<ApiResponse<List<VideoCategoryDTO>>> getCategories() {
        
        List<VideoCategoryDTO> categories = videoService.getCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * 获取用户观看历史（分页）
     * 
     * @param page 页码（从0开始）
     * @param size 每页数量
     * @return 观看历史分页列表
     * 
     * Requirements: 6.2
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取观看历史", description = "获取当前用户的视频观看历史")
    public ResponseEntity<ApiResponse<PageResponse<WatchHistoryDTO>>> getWatchHistory(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<WatchHistoryDTO> history = videoService.getWatchHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * 更新观看进度
     * 
     * @param id 视频ID
     * @param progress 观看进度信息
     * @return 操作结果
     * 
     * Requirements: 6.3
     */
    @PostMapping("/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "更新观看进度", description = "保存当前视频的观看位置")
    public ResponseEntity<ApiResponse<Void>> updateProgress(
            @Parameter(description = "视频ID") @PathVariable Long id,
            @Valid @RequestBody WatchProgressDTO progress) {
        
        Long userId = UserDetailsUtil.getCurrentUserId();
        videoService.updateWatchProgress(id, userId, progress.getPosition());
        return ResponseEntity.ok(ApiResponse.successVoid("观看进度已保存"));
    }
}
